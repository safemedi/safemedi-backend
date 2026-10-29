package com.safemedi.app.sefemedi.domain.auth.client

interface SocialLoginVerifier {
    fun resolveSocialId(
        accessToken: String
    ): String
}
