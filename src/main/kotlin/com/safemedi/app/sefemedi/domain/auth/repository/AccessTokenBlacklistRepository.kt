package com.safemedi.app.sefemedi.domain.auth.repository

import com.safemedi.app.sefemedi.domain.auth.entity.AccessTokenBlacklist
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface AccessTokenBlacklistRepository : JpaRepository<AccessTokenBlacklist, Long> {

    fun findByTokenHash(
        tokenHash: String,
    ): AccessTokenBlacklist?

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
            delete from AccessTokenBlacklist accessTokenBlacklist
            where accessTokenBlacklist.expiresAt <= :expiresAt
        """,
    )
    fun deleteExpiredEntriesAtOrBefore(
        @Param("expiresAt") expiresAt: LocalDateTime,
    ): Int

    @Modifying
    @Query(
        value = """
            insert into access_token_blacklist (
                token_hash,
                expires_at,
                created_at,
                updated_at
            )
            values (
                :tokenHash,
                :expiresAt,
                current_timestamp,
                current_timestamp
            )
            on duplicate key update
                expires_at = values(expires_at),
                updated_at = current_timestamp
        """,
        nativeQuery = true,
    )
    fun upsertTokenHash(
        @Param("tokenHash") tokenHash: String,
        @Param("expiresAt") expiresAt: LocalDateTime,
    ): Int
}
