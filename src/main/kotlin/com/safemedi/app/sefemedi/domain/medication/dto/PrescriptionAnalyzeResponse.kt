package com.safemedi.app.sefemedi.domain.medication.dto

data class PrescriptionAnalyzeResponse(
    val safetySummary: SafetySummaryResponse,
    val analyzedMedications: List<AnalyzedMedicationResponse>,
)

data class SafetySummaryResponse(
    val safeCount: Int,
    val warningCount: Int,
    val dangerCount: Int,
)

data class AnalyzedMedicationResponse(
    val atcCode: String,
    val drugName: String,
    val status: MedicationSafetyStatus,
    val efficacy: String,
    val precautions: List<String>,
    val warnings: List<MedicationWarningResponse>,
)

data class MedicationWarningResponse(
    val type: MedicationWarningType,
    val message: String,
)

enum class MedicationSafetyStatus {
    SAFE,
    WARNING,
    DANGER,
}

enum class MedicationWarningType {
    ALLERGY,
    DUR_ELDERLY,
    DUR_AGE,
    DUR_PREGNANCY,
    DUR_INTERACTION,
}
