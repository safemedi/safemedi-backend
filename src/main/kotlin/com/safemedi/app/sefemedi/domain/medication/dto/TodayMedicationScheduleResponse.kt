package com.safemedi.app.sefemedi.domain.medication.dto

import java.time.LocalDate

data class TodayMedicationScheduleResponse(
    val date: LocalDate,
    val summary: TodayMedicationSummaryResponse,
    val schedules: List<TodayMedicationScheduleItemResponse>,
)

data class TodayMedicationSummaryResponse(
    val completedCount: Int,
    val totalCount: Int,
    val completionRate: Int,
)

data class TodayMedicationScheduleItemResponse(
    val takeTime: String,
    val recordStatus: String,
    val displayStatus: String,
    val prescriptionId: Long,
    val prescriptionTitle: String,
    val drugCount: Int,
    val drugNames: List<String>,
    val recordIds: List<Long>,
)
