package com.safemedi.app.sefemedi.domain.family.dto

import java.time.Instant

data class FamilyInvitationInfoResponse(
    val inviterName: String,
    val expiresAt: Instant,
)
