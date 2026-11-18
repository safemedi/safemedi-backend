package com.safemedi.app.sefemedi.domain.medication.dto

data class PrescriptionExtractResponse(
    val title: String,
    val startDate: String?,
    val endDate: String?,
    val medications: List<ExtractedMedication>,
)

data class ExtractedMedication(
    val drugName: String,
    val drugCode: String? = null,
    val atcCode: String? = null,
    val dailyDoseCount: Int?,
    val instructions: String?,
)
