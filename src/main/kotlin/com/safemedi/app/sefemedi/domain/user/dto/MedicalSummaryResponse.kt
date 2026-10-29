package com.safemedi.app.sefemedi.domain.user.dto

import com.safemedi.app.sefemedi.domain.user.entity.BloodType
import com.safemedi.app.sefemedi.domain.user.entity.Gender
import com.safemedi.app.sefemedi.domain.user.entity.RhType
import java.time.LocalDate

data class MedicalSummaryResponse(
    val name: String?,
    val birthDate: LocalDate?,
    val gender: Gender?,
    val height: Int?,
    val weight: Int?,
    val bloodType: BloodType?,
    val rhType: RhType?,
    val diseases: List<DiseaseResponse>,
    val allergies: List<AllergyResponse>,
    val activeMedications: List<MedicalSummaryActiveMedicationResponse>,
)

data class MedicalSummaryActiveMedicationResponse(
    val prescriptionId: Long,
    val prescriptionTitle: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val medications: List<MedicalSummaryMedicationResponse>,
)

data class MedicalSummaryMedicationResponse(
    val drugName: String,
)
