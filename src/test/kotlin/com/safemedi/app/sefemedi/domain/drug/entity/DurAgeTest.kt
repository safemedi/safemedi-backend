package com.safemedi.app.sefemedi.domain.drug.entity

import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DurAgeTest {
    private val birth = LocalDate.of(2024, 1, 31)
    private fun rule(age: Int, unit: DurAgeUnit, condition: String) = DurAge(
        drugName = "테스트", targetAge = age, ageUnit = unit, ageCondition = condition,
    )

    @Test
    fun `6개월 미만은 6개월 도달일에 해제된다`() {
        val r = rule(6, DurAgeUnit.MONTH, "UNDER")
        assertTrue(r.matches(birth, LocalDate.of(2024, 7, 30)))
        assertFalse(r.matches(birth, LocalDate.of(2024, 7, 31)))
    }

    @Test
    fun `월말과 윤년은 달력의 실제 개월 경계를 사용한다`() {
        val r = rule(1, DurAgeUnit.MONTH, "UNDER")
        assertTrue(r.matches(birth, LocalDate.of(2024, 2, 28)))
        assertFalse(r.matches(birth, LocalDate.of(2024, 2, 29)))
    }

    @Test
    fun `6주 미만을 1년 미만으로 넓히지 않는다`() {
        val r = rule(6, DurAgeUnit.WEEK, "UNDER")
        assertTrue(r.matches(birth, birth.plusDays(41)))
        assertFalse(r.matches(birth, birth.plusDays(42)))
    }

    @Test
    fun `만 2세 이하는 세번째 생일 직전까지 적용된다`() {
        val r = rule(2, DurAgeUnit.YEAR, "LTE")
        assertTrue(r.matches(birth, birth.plusYears(3).minusDays(1)))
        assertFalse(r.matches(birth, birth.plusYears(3)))
    }

    @Test
    fun `이상은 경계일을 포함하고 초과는 다음 만 나이부터 적용한다`() {
        assertTrue(rule(2, DurAgeUnit.YEAR, "GTE").matches(birth, birth.plusYears(2)))
        assertFalse(rule(2, DurAgeUnit.YEAR, "GT").matches(birth, birth.plusYears(2)))
        assertTrue(rule(2, DurAgeUnit.YEAR, "GT").matches(birth, birth.plusYears(3)))
    }

    @Test
    fun `미래 생일과 음수 기준 및 미지원 비교조건은 적용하지 않는다`() {
        assertFalse(rule(1, DurAgeUnit.YEAR, "UNDER").matches(birth, birth.minusDays(1)))
        assertFalse(rule(-1, DurAgeUnit.MONTH, "UNDER").matches(birth, birth))
        assertFalse(rule(1, DurAgeUnit.YEAR, "UNKNOWN").matches(birth, birth))
    }
}
