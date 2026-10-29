package com.safemedi.app.sefemedi.domain.medication.dto

data class PrescriptionCreateResponse(
    val prescriptionId: Long,
    val message: String = "처방전이 성공적으로 등록되었습니다.",
)
