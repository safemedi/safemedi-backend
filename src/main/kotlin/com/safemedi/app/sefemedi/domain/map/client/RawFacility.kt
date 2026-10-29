package com.safemedi.app.sefemedi.domain.map.client

data class RawFacility(
    val name: String,
    val category: FacilityCategory,
    val address: String,
    val roadAddress: String,
    val latitude: Double,
    val longitude: Double,
    val distanceMeters: Int,
    val phoneNumber: String,
    val categoryName: String,
    val placeUrl: String?,
)
