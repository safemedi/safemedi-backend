package com.safemedi.app.sefemedi.domain.user.dto

data class TutorialResponse(
    val message: String = "튜토리얼 정보가 성공적으로 등록되었습니다.",
    val isTutorialCompleted: Boolean,
)
