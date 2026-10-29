package com.safemedi.app.sefemedi.domain.medication.dto

data class PrescriptionAnalyzeRequest(
    val medications: List<MedicationAnalyzeRequest> = emptyList(),
)

data class MedicationAnalyzeRequest(
    val drugCode: String,
)
