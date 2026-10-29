package com.safemedi.app.sefemedi.domain.drug.dto

data class DrugAllergySearchResponse(
    val allergyType: String,
    val allergyValue: String,
    val allergyName: String,
)

data class DrugAllergySearchPageResponse(
    val content: List<DrugAllergySearchResponse>,
    val page: Int,
    val size: Int,
    val isLast: Boolean,
)
