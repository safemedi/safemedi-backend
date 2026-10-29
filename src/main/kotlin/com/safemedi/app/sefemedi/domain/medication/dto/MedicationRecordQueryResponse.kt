package com.safemedi.app.sefemedi.domain.medication.dto

import java.time.LocalDate

data class MedicationRecordQueryResponse(
    val type: MedicationRecordQueryType,
    val date: LocalDate? = null,
    val periodStartDate: LocalDate? = null,
    val periodEndDate: LocalDate? = null,
    val familyId: Long? = null,
    val relation: String? = null,
    val summary: MedicationRecordSummaryResponse,
    val records: List<DailyMedicationRecordItemResponse>? = null,
    val dailyRecords: List<PeriodMedicationRecordGroupResponse>? = null,
)

data class MedicationRecordSummaryResponse(
    val totalCount: Int,
    val takenCount: Int,
    val fraction: String,
)

data class DailyMedicationRecordItemResponse(
    val recordIds: List<Long>,
    val prescriptionTitle: String,
    val medicationNames: List<String>,
    val scheduledTime: String,
    val takenTime: String?,
    val status: String,
)

data class PeriodMedicationRecordGroupResponse(
    val date: LocalDate,
    val totalCount: Int,
    val takenCount: Int,
    val fraction: String,
    val items: List<PeriodMedicationRecordItemResponse>,
)

data class PeriodMedicationRecordItemResponse(
    val recordIds: List<Long>,
    val prescriptionTitle: String,
    val scheduledTime: String,
    val status: String,
)

enum class MedicationRecordQueryType {
    DAILY, WEEK, MONTH
}
