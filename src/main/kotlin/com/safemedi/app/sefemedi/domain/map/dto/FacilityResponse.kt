package com.safemedi.app.sefemedi.domain.map.dto

data class FacilityResponse(
    val id: String,
    val name: String,
    val category: String,
    val address: String,
    val roadAddress: String,
    val latitude: Double,
    val longitude: Double,
    val distanceMeters: Int,
    val phoneNumber: String,
    val is24Hours: Boolean,
    val status: String,
    val placeUrl: String?,
)
