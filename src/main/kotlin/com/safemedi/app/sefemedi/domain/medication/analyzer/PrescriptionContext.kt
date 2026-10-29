package com.safemedi.app.sefemedi.domain.medication.analyzer

import com.safemedi.app.sefemedi.domain.drug.entity.DrugIngredientMap
import com.safemedi.app.sefemedi.domain.medication.dto.MedicationSafetyStatus
import com.safemedi.app.sefemedi.domain.medication.dto.MedicationWarningResponse
import com.safemedi.app.sefemedi.domain.medication.dto.MedicationWarningType
import com.safemedi.app.sefemedi.domain.user.entity.User
import com.safemedi.app.sefemedi.domain.user.entity.UserAllergy
import com.safemedi.app.sefemedi.domain.user.entity.UserHealthProfile

class PrescriptionContext(
    val user: User,
    val healthProfile: UserHealthProfile?,
    val allergies: List<UserAllergy>,
    val medications: List<MedicationAnalysisTarget>,
    val ingredientMaps: List<DrugIngredientMap>,
) {
    private val mutableAnalyzedMedications =
        medications.map { AnalyzedMedication(it) }

    val analyzedMedications: List<AnalyzedMedication>
        get() = mutableAnalyzedMedications

    fun addPrecaution(
        medication: MedicationAnalysisTarget,
        message: String,
    ) {
        mutableAnalyzedMedications
            .filter { it.medication === medication }
            .forEach { it.addPrecaution(message) }
    }

    fun addWarning(
        medication: MedicationAnalysisTarget,
        type: MedicationWarningType,
        message: String,
        status: MedicationSafetyStatus,
    ) {
        mutableAnalyzedMedications
            .filter { it.medication === medication }
            .forEach { it.addWarning(type, message, status) }
    }

    fun ingredientsOf(medication: MedicationAnalysisTarget): List<String> =
        ingredientMaps
            .filter {
                it.drug.drugCode == medication.drugCode
            }
            .mapNotNull { it.ingredient.ingredientName }
            .distinct()
}

data class MedicationAnalysisTarget(
    val drugCode: String,
    val atcCode: String,
    val drugName: String,
)

class AnalyzedMedication(
    val medication: MedicationAnalysisTarget,
) {
    var status: MedicationSafetyStatus = MedicationSafetyStatus.SAFE
        private set

    val efficacy: String = "정보 없음"
    private val mutablePrecautions = linkedSetOf<String>()
    private val mutableWarnings = mutableListOf<MedicationWarningResponse>()

    val precautions: List<String>
        get() = mutablePrecautions.toList()

    val warnings: List<MedicationWarningResponse>
        get() = mutableWarnings.toList()

    fun addPrecaution(message: String) {
        mutablePrecautions.add(message)
        if (status == MedicationSafetyStatus.SAFE) {
            status = MedicationSafetyStatus.WARNING
        }
    }

    fun addWarning(
        type: MedicationWarningType,
        message: String,
        warningStatus: MedicationSafetyStatus,
    ) {
        mutableWarnings.add(MedicationWarningResponse(type, message))
        if (warningStatus == MedicationSafetyStatus.DANGER || status == MedicationSafetyStatus.SAFE) {
            status = warningStatus
        }
    }
}
