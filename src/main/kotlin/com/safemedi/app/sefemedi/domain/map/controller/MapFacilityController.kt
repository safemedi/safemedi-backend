package com.safemedi.app.sefemedi.domain.map.controller

import com.safemedi.app.sefemedi.domain.map.dto.MapFacilitiesResponse
import com.safemedi.app.sefemedi.domain.map.service.MapFacilityQueryService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/map")
class MapFacilityController(
    private val mapFacilityQueryService: MapFacilityQueryService,
) {
    @GetMapping("/facilities")
    fun getFacilities(
        @RequestParam(required = false) latitude: Double?,
        @RequestParam(required = false) longitude: Double?,
        @RequestParam(defaultValue = "all") category: String,
        @RequestParam(defaultValue = "") keyword: String,
    ): MapFacilitiesResponse {
        return mapFacilityQueryService.getFacilities(
            latitude = latitude,
            longitude = longitude,
            category = category,
            keyword = keyword,
        )
    }
}
