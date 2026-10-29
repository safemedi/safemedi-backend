package com.safemedi.app.sefemedi.domain.medication.dto

data class PrescriptionUpdateResponse(
    val prescriptionId: Long,
    val title: String,
    val message: String = "처방전 정보와 향후 복약 스케줄이 성공적으로 업데이트되었습니다.",
)
