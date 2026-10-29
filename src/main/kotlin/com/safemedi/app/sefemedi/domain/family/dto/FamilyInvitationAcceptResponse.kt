package com.safemedi.app.sefemedi.domain.family.dto

import java.time.Instant

data class FamilyInvitationAcceptResponse(
    val familyId: Long,
    val name: String,
    val relation: String,
    val connectedAt: Instant,
)
