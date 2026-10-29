package com.safemedi.app.sefemedi.domain.medication.dto

import java.time.LocalDate

data class PrescriptionDetailResponse(
    val prescriptionId: Long,
    val title: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val isDoctorApproved: Boolean,
    val hasAllergyConflict: Boolean,
    val medications: List<PrescriptionMedicationResponse>,
)

data class PrescriptionMedicationResponse(
    val prescriptionDrugId: Long,
    val drugCode: String?,
    val drugName: String,
    val atcCode: String?,
    val takeTimes: List<String>,
)
