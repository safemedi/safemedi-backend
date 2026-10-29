package com.safemedi.app.sefemedi.domain.drug.dto

data class DiseaseSearchResponse(
    val diseaseCode: String,
    val diseaseName: String,
)

data class DiseaseSearchPageResponse(
    val content: List<DiseaseSearchResponse>,
    val page: Int,
    val size: Int,
    val isLast: Boolean,
)
