package com.safemedi.app.sefemedi.domain.auth.dto

data class TokenReissueRequest(
    val refreshToken: String? = null
)
