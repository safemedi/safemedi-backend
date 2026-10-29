package com.safemedi.app.sefemedi.domain.medication.dto

import java.time.LocalDate

data class MedicationStatisticsResponse(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val familyId: Long?,
    val relation: String?,
    val totalCount: Int,
    val takenCount: Int,
    val fraction: String,
    val dailyCompliance: List<DailyMedicationComplianceResponse>,
)

data class DailyMedicationComplianceResponse(
    val date: LocalDate,
    val takenCount: Int,
    val totalCount: Int,
    val fraction: String,
)
