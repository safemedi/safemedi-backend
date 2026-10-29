package com.safemedi.app.sefemedi.domain.medication.analyzer

import com.safemedi.app.sefemedi.domain.drug.entity.DurAge
import com.safemedi.app.sefemedi.domain.drug.repository.DurAgeRepository
import com.safemedi.app.sefemedi.domain.drug.repository.DurElderlyRepository
import com.safemedi.app.sefemedi.domain.drug.repository.DurInteractionRepository
import com.safemedi.app.sefemedi.domain.drug.repository.DurPregnancyRepository
import com.safemedi.app.sefemedi.domain.medication.dto.MedicationSafetyStatus
import com.safemedi.app.sefemedi.domain.medication.dto.MedicationWarningType
import com.safemedi.app.sefemedi.domain.user.entity.Gender
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.Period

@Component
@Order(3)
class DurSafetyAnalyzerImpl(
    private val durElderlyRepository: DurElderlyRepository,
    private val durAgeRepository: DurAgeRepository,
    private val durPregnancyRepository: DurPregnancyRepository,
    private val durInteractionRepository: DurInteractionRepository,
) : PrescriptionAnalyzer {
    override fun analyze(context: PrescriptionContext) {
        val drugNames = context.medications.map { it.drugName }
        val age = context.healthProfile?.birthDate?.let { Period.between(it, LocalDate.now()).years }

        analyzeElderly(context, drugNames, age)
        analyzeAge(context, drugNames, age)
        analyzePregnancy(context, drugNames)
        analyzeInteraction(context, drugNames)
    }

    private fun analyzeElderly(
        context: PrescriptionContext,
        drugNames: List<String>,
        age: Int?,
    ) {
        if (age == null || age < ELDERLY_AGE) {
            return
        }

        durElderlyRepository.findByDrugNameIn(drugNames).forEach { dur ->
            context.findMedications(dur.drugName).forEach {
                context.addWarning(
                    medication = it,
                    type = MedicationWarningType.DUR_ELDERLY,
                    message = dur.warningMessage,
                    status = MedicationSafetyStatus.WARNING,
                )
            }
        }
    }

    private fun analyzeAge(
        context: PrescriptionContext,
        drugNames: List<String>,
        age: Int?,
    ) {
        if (age == null) {
            return
        }

        durAgeRepository.findByDrugNameIn(drugNames)
            .filter { it.matches(age) }
            .forEach { dur ->
                context.findMedications(dur.drugName).forEach {
                    context.addWarning(
                        medication = it,
                        type = MedicationWarningType.DUR_AGE,
                        message = dur.warningMessage,
                        status = MedicationSafetyStatus.DANGER,
                    )
                }
            }
    }

    private fun analyzePregnancy(
        context: PrescriptionContext,
        drugNames: List<String>,
    ) {
        if (context.healthProfile?.gender != Gender.FEMALE) {
            return
        }

        durPregnancyRepository.findByDrugNameIn(drugNames).forEach { dur ->
            context.findMedications(dur.drugName).forEach {
                context.addWarning(
                    medication = it,
                    type = MedicationWarningType.DUR_PREGNANCY,
                    message = dur.warningMessage,
                    status = MedicationSafetyStatus.DANGER,
                )
            }
        }
    }

    private fun analyzeInteraction(
        context: PrescriptionContext,
        drugNames: List<String>,
    ) {
        durInteractionRepository.findInteractions(drugNames).forEach { dur ->
            val message = dur.warningMessage

            context.findMedications(dur.drugNameA).forEach {
                context.addWarning(
                    medication = it,
                    type = MedicationWarningType.DUR_INTERACTION,
                    message = message,
                    status = MedicationSafetyStatus.DANGER,
                )
            }
            context.findMedications(dur.drugNameB).forEach {
                context.addWarning(
                    medication = it,
                    type = MedicationWarningType.DUR_INTERACTION,
                    message = message,
                    status = MedicationSafetyStatus.DANGER,
                )
            }
        }
    }

    private fun PrescriptionContext.findMedications(drugName: String): List<MedicationAnalysisTarget> =
        medications.filter { it.drugName.equals(drugName, ignoreCase = true) }

    private fun DurAge.matches(age: Int): Boolean =
        when (ageCondition.trim().uppercase()) {
            "UNDER", "LESS_THAN", "LT" -> age < targetAge
            "UNDER_OR_EQUAL", "LESS_THAN_OR_EQUAL", "LTE" -> age <= targetAge
            "OVER", "GREATER_THAN", "GT" -> age > targetAge
            "OVER_OR_EQUAL", "GREATER_THAN_OR_EQUAL", "GTE" -> age >= targetAge
            else -> false
        }

    private companion object {
        const val ELDERLY_AGE = 65
    }
}
