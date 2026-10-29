package com.safemedi.app.sefemedi.domain.medication.dto

data class PrescriptionUpdateRequest(
    val title: String? = null,
    val medications: List<PrescriptionMedicationUpdateRequest>? = null,
)

data class PrescriptionMedicationUpdateRequest(
    val prescriptionDrugId: Long,
    val takeTimes: List<String>,
)
