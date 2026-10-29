package com.safemedi.app.sefemedi.domain.medication.dto

import com.safemedi.app.sefemedi.domain.medication.entity.MedicationStatus

data class MedicationRecordUpdateRequest(
    val recordIds: List<Long>,
    val status: MedicationStatus,
)
