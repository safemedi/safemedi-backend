package com.safemedi.app.sefemedi.domain.medication.dto

import java.time.LocalDate

data class PrescriptionCreateRequest(
    val title: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val isDoctorApproved: Boolean = false,
    val medications: List<PrescriptionMedicationRequest> = emptyList(),
)

data class PrescriptionMedicationRequest(
    val drugCode: String,
    val atcCode: String? = null,
    val drugName: String? = null,
    val takeTimes: List<String> = emptyList(),
)
