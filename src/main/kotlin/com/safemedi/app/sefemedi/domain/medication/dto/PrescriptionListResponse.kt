package com.safemedi.app.sefemedi.domain.medication.dto

import java.time.LocalDate

data class PrescriptionListResponse(
    val content: List<PrescriptionSummaryResponse>,
    val isLast: Boolean,
)

data class PrescriptionSummaryResponse(
    val prescriptionId: Long,
    val title: String,
    val createdAt: LocalDate,
    val drugCount: Int,
    val hasAllergyConflict: Boolean,
)
