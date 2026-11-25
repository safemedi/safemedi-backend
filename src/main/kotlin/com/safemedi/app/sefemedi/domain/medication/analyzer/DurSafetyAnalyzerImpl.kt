package com.safemedi.app.sefemedi.domain.medication.analyzer

import com.safemedi.app.sefemedi.domain.drug.entity.DurType
import com.safemedi.app.sefemedi.domain.drug.repository.*
import com.safemedi.app.sefemedi.domain.medication.dto.MedicationSafetyStatus
import com.safemedi.app.sefemedi.domain.medication.dto.MedicationWarningType
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId

@Component
@Order(3)
class DurSafetyAnalyzerImpl(
    private val durElderlyRepository: DurElderlyRepository,
    private val durAgeRepository: DurAgeRepository,
    private val durPregnancyRepository: DurPregnancyRepository,
    private val durInteractionRepository: DurInteractionRepository,
    private val durCoverageRepository: DurCoverageRepository,
    private val clock: Clock,
) : PrescriptionAnalyzer {
    override fun analyze(context: PrescriptionContext) {
        val codes = context.medications.map { it.drugCode }.distinct()
        if (codes.isEmpty()) return
        val today = LocalDate.now(clock.withZone(ZoneId.of("Asia/Seoul")))
        val birth = context.healthProfile?.birthDate?.takeIf { it <= today }
        val coverage = durCoverageRepository.findByDrugCodeInAndVerifiedTrue(codes)
            .filter { it.verified }.map { it.drugCode to it.durType }.toSet()
        val required = buildSet {
            add(DurType.INTERACTION)
            if (birth != null) {
                add(DurType.AGE)
                if (today >= birth.plusYears(65)) add(DurType.ELDERLY)
            }
            if (context.healthProfile?.isPregnant == true) add(DurType.PREGNANCY)
        }
        context.medications.forEach { medication ->
            if (required.any { (medication.drugCode to it) !in coverage }) {
                context.addInformationalPrecaution(medication, "일부 DUR 데이터의 매핑 또는 적용 조건이 검증되지 않아 안전성 판정을 완료하지 못했습니다.")
            }
            if (birth == null) context.addInformationalPrecaution(medication, "생년월일 정보가 없어 연령금기 및 노인주의 판정을 완료하지 못했습니다.")
            if (context.healthProfile == null) context.addInformationalPrecaution(medication, "건강 정보가 없어 임부금기 적용 여부를 확인하지 못했습니다.")
        }
        if (birth != null) {
            durAgeRepository.findByDrugCodeInAndRuntimeVerifiedTrue(codes)
                .filter { it.runtimeVerified && (it.drugCode to DurType.AGE) in coverage && it.matches(birth, today) }
                .forEach { rule -> context.warn(rule.drugCode, MedicationWarningType.DUR_AGE,
                    rule.warningMessage?.takeIf(String::isNotBlank) ?: "연령금기 대상 의약품입니다.", MedicationSafetyStatus.DANGER) }
            if (today >= birth.plusYears(65)) {
                durElderlyRepository.findByDrugCodeInAndRuntimeVerifiedTrue(codes)
                    .filter { it.runtimeVerified && (it.drugCode to DurType.ELDERLY) in coverage }
                    .forEach { rule -> context.warn(rule.drugCode, MedicationWarningType.DUR_ELDERLY,
                        rule.warningMessage?.takeIf(String::isNotBlank) ?: "노인주의 대상 의약품입니다.", MedicationSafetyStatus.WARNING) }
            }
        }
        if (context.healthProfile?.isPregnant == true) {
            durPregnancyRepository.findByDrugCodeInAndRuntimeVerifiedTrue(codes)
                .filter { it.runtimeVerified && (it.drugCode to DurType.PREGNANCY) in coverage }
                .forEach { rule ->
                    val fallback = if (rule.grade == "M")
                        "임부금기 조건부 대상 의약품입니다. 치료 목적에 따라 금기 등급이 달라질 수 있습니다."
                    else "임부금기 대상 의약품입니다."
                    context.warn(rule.drugCode, MedicationWarningType.DUR_PREGNANCY,
                        rule.warningMessage?.takeIf(String::isNotBlank) ?: fallback, MedicationSafetyStatus.DANGER)
                }
        }
        durInteractionRepository.findInteractions(codes)
            .filter { it.runtimeVerified && (it.drugCodeA to DurType.INTERACTION) in coverage && (it.drugCodeB to DurType.INTERACTION) in coverage }
            .forEach { rule ->
                val a = context.medications.filter { it.drugCode == rule.drugCodeA }
                val b = context.medications.filter { it.drugCode == rule.drugCodeB }
                if (a.none { left -> b.any { right -> left !== right } }) return@forEach
                val message = rule.warningMessage?.takeIf(String::isNotBlank) ?: "병용금기 대상 의약품 조합입니다."
                context.warn(rule.drugCodeA, MedicationWarningType.DUR_INTERACTION, message, MedicationSafetyStatus.DANGER)
                context.warn(rule.drugCodeB, MedicationWarningType.DUR_INTERACTION, message, MedicationSafetyStatus.DANGER)
            }
    }

    private fun PrescriptionContext.warn(code: String?, type: MedicationWarningType, message: String, status: MedicationSafetyStatus) {
        if (code == null) return
        analyzedMedications.filter { it.medication.drugCode == code }.forEach { target ->
            if (target.warnings.none { it.type == type && it.message == message }) addWarning(target.medication, type, message, status)
        }
    }
}
