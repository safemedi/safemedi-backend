package com.safemedi.app.sefemedi.domain.auth.dto

data class LogoutRequest(
    val deviceToken: String? = null,
)
