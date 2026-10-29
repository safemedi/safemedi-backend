package com.safemedi.app.sefemedi.domain.map.client

interface MedicalFacilitySearchClient {
    fun search(
        category: FacilityCategory,
        query: String,
        latitude: Double,
        longitude: Double,
    ): List<RawFacility>
}
