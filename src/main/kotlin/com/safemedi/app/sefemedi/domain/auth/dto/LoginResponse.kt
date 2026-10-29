package com.safemedi.app.sefemedi.domain.auth.dto

data class LoginResponse(
    val accessToken: String,
    val refreshToken: String,
    val isTutorialCompleted: Boolean
)
