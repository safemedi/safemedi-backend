package com.safemedi.app.sefemedi.domain.auth.entity

import com.safemedi.app.sefemedi.global.entity.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "access_token_blacklist")
class AccessTokenBlacklist(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "token_hash", nullable = false, length = 64, unique = true)
    val tokenHash: String,

    @Column(name = "expires_at", nullable = false)
    var expiresAt: LocalDateTime,
) : BaseTimeEntity()
