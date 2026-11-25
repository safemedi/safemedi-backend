package com.safemedi.app.sefemedi.domain.medication.analyzer

import com.safemedi.app.sefemedi.domain.drug.entity.*
import com.safemedi.app.sefemedi.domain.drug.repository.*
import com.safemedi.app.sefemedi.domain.medication.dto.MedicationSafetyStatus
import com.safemedi.app.sefemedi.domain.user.entity.User
import com.safemedi.app.sefemedi.domain.user.entity.UserHealthProfile
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import java.time.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DurSafetyAnalyzerTest {
    private val elderly = mock(DurElderlyRepository::class.java)
    private val age = mock(DurAgeRepository::class.java)
    private val pregnancy = mock(DurPregnancyRepository::class.java)
    private val interaction = mock(DurInteractionRepository::class.java)
    private val coverage = mock(DurCoverageRepository::class.java)
    private val clock = Clock.fixed(Instant.parse("2026-07-31T15:00:00Z"), ZoneOffset.UTC)
    private val analyzer = DurSafetyAnalyzerImpl(elderly, age, pregnancy, interaction, coverage, clock)
    private val user = User(id = 1, socialId = "test")
    private fun context(vararg codes: String, birth: LocalDate? = LocalDate.of(1990, 1, 1)) = PrescriptionContext(
        user, UserHealthProfile(userId = 1, user = user, birthDate = birth), emptyList(),
        codes.map { MedicationAnalysisTarget(it, "A01", "같은 제품명") }, emptyList(),
    )
    private fun covered(vararg codes: String) {
        `when`(coverage.findByDrugCodeInAndVerifiedTrue(codes.toList())).thenReturn(
            codes.flatMap { c -> DurType.entries.map { DurCoverage("$c:$it", c, it, "test", true) } },
        )
    }

    @Test
    fun `데이터 미검증은 안내만 추가하고 약물 위험 상태를 올리지 않는다`() {
        val c = context("A")
        analyzer.analyze(c)
        assertEquals(MedicationSafetyStatus.SAFE, c.analyzedMedications.single().status)
        assertTrue(c.analyzedMedications.single().precautions.any { "완료하지 못" in it })
        assertTrue(c.analyzedMedications.single().warnings.isEmpty())
    }

    @Test
    fun `생년월일과건강정보누락도안내만기존응답으로전달한다`() {
        val c = PrescriptionContext(user, null, emptyList(), listOf(MedicationAnalysisTarget("A", "A01", "약")), emptyList())
        analyzer.analyze(c)
        val result = c.analyzedMedications.single()
        assertEquals(MedicationSafetyStatus.SAFE, result.status)
        assertEquals(3, result.precautions.size)
        assertTrue(result.warnings.isEmpty())
    }

    @Test
    fun `미검증안내가있어도이미확인된위험경고를유지한다`() {
        val c = context("A")
        c.addWarning(c.medications.single(), com.safemedi.app.sefemedi.domain.medication.dto.MedicationWarningType.ALLERGY,
            "알러지 경고", MedicationSafetyStatus.DANGER)
        analyzer.analyze(c)
        val result = c.analyzedMedications.single()
        assertEquals(MedicationSafetyStatus.DANGER, result.status)
        assertEquals("알러지 경고", result.warnings.single().message)
        assertTrue(result.precautions.isNotEmpty())
    }

    @Test
    fun `일반약물주의는기존대로WARNING을유지한다`() {
        val c = context("A")
        c.addPrecaution(c.medications.single(), "실제 약물 주의")
        analyzer.analyze(c)
        assertEquals(MedicationSafetyStatus.WARNING, c.analyzedMedications.single().status)
    }

    @Test
    fun `미검증 규칙은 저장되어 있어도 위험 경고로 사용하지 않는다`() {
        covered("A", "B")
        `when`(interaction.findInteractions(listOf("A", "B"))).thenReturn(listOf(
            DurInteraction(drugNameA = "A", drugNameB = "B", drugCodeA = "A", drugCodeB = "B", runtimeVerified = false),
        ))
        val c = context("A", "B")
        analyzer.analyze(c)
        assertTrue(c.analyzedMedications.all { it.warnings.isEmpty() })
    }

    @Test
    fun `역순 조합도 양쪽 약에 경고하고 중복 규칙을 제거한다`() {
        covered("A", "B")
        val rule = DurInteraction(drugNameA = "원본B", drugNameB = "원본A", drugCodeA = "B", drugCodeB = "A", runtimeVerified = true)
        `when`(interaction.findInteractions(listOf("A", "B"))).thenReturn(listOf(rule, rule))
        val c = context("A", "B")
        analyzer.analyze(c)
        assertTrue(c.analyzedMedications.all { it.status == MedicationSafetyStatus.DANGER && it.warnings.size == 1 })
    }

    @Test
    fun `같은 코드 단일 약을 자기 자신과 병용한 것으로 판정하지 않는다`() {
        covered("A")
        `when`(interaction.findInteractions(listOf("A"))).thenReturn(listOf(
            DurInteraction(drugNameA = "A", drugNameB = "A", drugCodeA = "A", drugCodeB = "A", runtimeVerified = true),
        ))
        val c = context("A")
        analyzer.analyze(c)
        assertEquals(MedicationSafetyStatus.SAFE, c.analyzedMedications.single().status)
    }

    @Test
    fun `서울 날짜 경계와 주 단위 생일을 사용한다`() {
        covered("A")
        val r = DurAge(drugName = "원본", drugCode = "A", targetAge = 6,
            ageUnit = DurAgeUnit.WEEK, ageCondition = "UNDER", runtimeVerified = true)
        `when`(age.findByDrugCodeInAndRuntimeVerifiedTrue(listOf("A"))).thenReturn(listOf(r))
        val c = context("A", birth = LocalDate.of(2026, 6, 20))
        analyzer.analyze(c) // 서울 8월 1일: 정확히 6주이므로 미만 아님
        assertEquals(MedicationSafetyStatus.SAFE, c.analyzedMedications.single().status)
    }
}
