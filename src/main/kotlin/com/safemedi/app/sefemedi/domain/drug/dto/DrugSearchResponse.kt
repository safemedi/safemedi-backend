package com.safemedi.app.sefemedi.domain.drug.dto

data class DrugSearchResponse(
    val drugCode: String,
    val atcCode: String,
    val drugName: String,
)

data class DrugSearchPageResponse(
    val content: List<DrugSearchResponse>,
    val page: Int,
    val size: Int,
    val isLast: Boolean,
)
