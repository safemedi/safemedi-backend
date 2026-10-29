package com.safemedi.app.sefemedi.domain.user.dto

data class DeviceTokenResponse(
    val deviceId: Long,
    val message: String = "기기 푸시 토큰이 성공적으로 등록(갱신)되었습니다.",
)
