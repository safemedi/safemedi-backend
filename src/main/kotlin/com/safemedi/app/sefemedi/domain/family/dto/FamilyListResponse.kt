package com.safemedi.app.sefemedi.domain.family.dto

data class FamilyListResponse(
    val families: List<FamilyListItemResponse>,
)

data class FamilyListItemResponse(
    val familyId: Long?,
    val name: String?,
    val relation: String,
)
