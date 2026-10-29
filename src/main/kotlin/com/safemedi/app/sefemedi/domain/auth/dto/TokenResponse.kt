package com.safemedi.app.sefemedi.domain.auth.dto

data class TokenResponse(
    val accessToken: String,
    val refreshToken: String
)
