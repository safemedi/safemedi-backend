package com.safemedi.app.sefemedi.domain.family.dto

import com.safemedi.app.sefemedi.domain.family.entity.FamilyInvitationStatus
import java.time.Instant

data class FamilyInvitationCreateResponse(
    val invitationId: Long,
    val inviteUrl: String,
    val status: FamilyInvitationStatus,
    val createdAt: Instant,
    val expiresAt: Instant,
)
