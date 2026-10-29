package com.safemedi.app.sefemedi.domain.map.dto

data class MapFacilitiesResponse(
    val source: String,
    val facilities: List<FacilityResponse>,
)
