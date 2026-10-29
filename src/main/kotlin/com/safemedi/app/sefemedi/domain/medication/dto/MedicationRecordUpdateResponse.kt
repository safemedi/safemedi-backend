package com.safemedi.app.sefemedi.domain.medication.dto

import java.time.LocalDateTime

data class MedicationRecordUpdateResponse(
    val recordIds: List<Long>,
    val prescriptionId: Long,
    val scheduledAt: LocalDateTime,
    val drugNames: List<String>,
    val takenAt: LocalDateTime?,
    val status: String,
)
