package com.safemedi.app.sefemedi.domain.auth.repository

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@SpringBootTest(properties = ["spring.jpa.hibernate.ddl-auto=create-drop"])
@ActiveProfiles("test")
@Transactional
class AccessTokenBlacklistRepositoryTest {

    @Autowired
    private lateinit var accessTokenBlacklistRepository: AccessTokenBlacklistRepository

    @Test
    fun `upsert token hash keeps a single row when called twice`() {
        val tokenHash = "a".repeat(64)
        val firstExpiresAt = LocalDateTime.of(2026, 7, 30, 10, 0)
        val secondExpiresAt = firstExpiresAt.plusMinutes(5)

        accessTokenBlacklistRepository.upsertTokenHash(
            tokenHash = tokenHash,
            expiresAt = firstExpiresAt,
        )
        accessTokenBlacklistRepository.upsertTokenHash(
            tokenHash = tokenHash,
            expiresAt = secondExpiresAt,
        )

        val stored = accessTokenBlacklistRepository.findByTokenHash(tokenHash)

        assertNotNull(stored)
        assertEquals(tokenHash, stored!!.tokenHash)
        assertEquals(secondExpiresAt, stored.expiresAt)
        assertEquals(1L, accessTokenBlacklistRepository.count())
    }

    @Test
    fun `delete expired entries at or before removes only expired rows`() {
        val expiredTokenHash = "a".repeat(64)
        val activeTokenHash = "b".repeat(64)
        val purgeTime = LocalDateTime.of(2026, 7, 30, 10, 0)

        accessTokenBlacklistRepository.saveAllAndFlush(
            listOf(
                com.safemedi.app.sefemedi.domain.auth.entity.AccessTokenBlacklist(
                    tokenHash = expiredTokenHash,
                    expiresAt = purgeTime.minusSeconds(1),
                ),
                com.safemedi.app.sefemedi.domain.auth.entity.AccessTokenBlacklist(
                    tokenHash = activeTokenHash,
                    expiresAt = purgeTime.plusSeconds(1),
                ),
            ),
        )

        val deletedCount = accessTokenBlacklistRepository.deleteExpiredEntriesAtOrBefore(purgeTime)

        assertEquals(1, deletedCount)
        assertNull(accessTokenBlacklistRepository.findByTokenHash(expiredTokenHash))
        assertNotNull(accessTokenBlacklistRepository.findByTokenHash(activeTokenHash))
        assertEquals(1L, accessTokenBlacklistRepository.count())
    }
}
