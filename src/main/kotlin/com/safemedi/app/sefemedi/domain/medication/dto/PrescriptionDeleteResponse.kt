package com.safemedi.app.sefemedi.domain.medication.dto

data class PrescriptionDeleteResponse(
    val message: String = "처방전이 삭제 처리되었으며, 예정된 복약 스케줄이 삭제되었습니다.",
)
