package com.safemedi.app.sefemedi.domain.family.dto

import java.time.Instant

data class FamilyRelationUpdateResponse(
    val familyId: Long,
    val name: String,
    val relation: String,
    val updatedAt: Instant,
)
