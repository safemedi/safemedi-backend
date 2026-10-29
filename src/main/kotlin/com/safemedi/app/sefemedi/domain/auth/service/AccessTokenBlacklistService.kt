package com.safemedi.app.sefemedi.domain.auth.service

import java.time.Instant

interface AccessTokenBlacklistService {

    fun contains(
        token: String,
    ): Boolean

    fun blacklist(
        token: String,
        expiresAt: Instant,
    )

    fun purgeExpiredEntries()
}
