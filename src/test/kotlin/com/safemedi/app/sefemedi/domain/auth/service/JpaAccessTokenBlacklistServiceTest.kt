package com.safemedi.app.sefemedi.domain.auth.service

import com.safemedi.app.sefemedi.domain.auth.entity.AccessTokenBlacklist
import com.safemedi.app.sefemedi.domain.auth.repository.AccessTokenBlacklistRepository
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import java.security.MessageDigest
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

class JpaAccessTokenBlacklistServiceTest {

    private val accessToken = "access-token"
    private val clock =
        Clock.fixed(
            Instant.parse("2026-07-29T00:00:00Z"),
            ZoneOffset.UTC,
        )
    private val accessTokenBlacklistRepository =
        mock(AccessTokenBlacklistRepository::class.java)
    private val service =
        JpaAccessTokenBlacklistService(
            accessTokenBlacklistRepository = accessTokenBlacklistRepository,
            clock = clock,
        )

    @Test
    fun `blacklist stores hashed token with expiration time`() {
        val expiresAt = Instant.parse("2026-07-29T00:10:00Z")
        val tokenHash = hashToken()

        service.blacklist(
            token = accessToken,
            expiresAt = expiresAt,
        )

        verify(accessTokenBlacklistRepository).upsertTokenHash(
            tokenHash = tokenHash,
            expiresAt = LocalDateTime.ofInstant(expiresAt, clock.zone),
        )
    }

    @Test
    fun `contains returns true for active blacklisted token`() {
        val tokenHash = hashToken()
        val blacklistedToken =
            AccessTokenBlacklist(
                id = 1L,
                tokenHash = tokenHash,
                expiresAt = LocalDateTime.ofInstant(
                    Instant.parse("2026-07-29T00:10:00Z"),
                    clock.zone,
                ),
            )

        given(accessTokenBlacklistRepository.findByTokenHash(tokenHash)).willReturn(blacklistedToken)

        assertTrue(
            service.contains(accessToken),
        )
    }

    @Test
    fun `contains returns false for expired blacklisted token`() {
        val tokenHash = hashToken()
        val blacklistedToken =
            AccessTokenBlacklist(
                id = 1L,
                tokenHash = tokenHash,
                expiresAt = LocalDateTime.ofInstant(
                    Instant.parse("2026-07-28T23:59:59Z"),
                    clock.zone,
                ),
            )

        given(accessTokenBlacklistRepository.findByTokenHash(tokenHash)).willReturn(blacklistedToken)

        assertFalse(
            service.contains(accessToken),
        )
    }

    @Test
    fun `purgeExpiredEntries deletes entries expired at the current time`() {
        service.purgeExpiredEntries()

        verify(accessTokenBlacklistRepository).deleteExpiredEntriesAtOrBefore(
            LocalDateTime.now(clock),
        )
    }

    private fun hashToken(): String {
        val digest =
            MessageDigest.getInstance("SHA-256")
                .digest(accessToken.toByteArray(Charsets.UTF_8))

        return digest.joinToString(separator = "") {
            "%02x".format(it.toInt() and 0xff)
        }
    }
}
