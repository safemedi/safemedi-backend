package com.safemedi.app.sefemedi.domain.map.service

import com.safemedi.app.sefemedi.domain.map.client.FacilityCategory
import com.safemedi.app.sefemedi.domain.map.client.MedicalFacilitySearchClient
import com.safemedi.app.sefemedi.domain.map.client.RawFacility
import com.safemedi.app.sefemedi.domain.map.dto.FacilityResponse
import com.safemedi.app.sefemedi.domain.map.dto.MapFacilitiesResponse
import com.safemedi.app.sefemedi.global.error.BusinessException
import com.safemedi.app.sefemedi.global.error.ErrorCode
import jakarta.annotation.PreDestroy
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Service
class MapFacilityQueryService(
    private val facilitySearchClient: MedicalFacilitySearchClient,
) {
    private val facilitySearchExecutor: ExecutorService = Executors.newFixedThreadPool(EXTERNAL_CALL_POOL_SIZE)

    @PreDestroy
    fun shutdownExecutor() {
        facilitySearchExecutor.shutdown()
    }

    fun getFacilities(
        latitude: Double?,
        longitude: Double?,
        category: String,
        keyword: String,
    ): MapFacilitiesResponse {
        val (validLatitude, validLongitude) = requireValidCoordinates(latitude, longitude)
        val facilityCategory = resolveCategory(category)
        val normalizedKeyword = keyword.trim()

        val rawResults = fetchFacilities(facilityCategory, normalizedKeyword, validLatitude, validLongitude)
        val facilities = dedupAndSort(rawResults)

        return if (facilities.isEmpty()) {
            mockResponse(facilityCategory)
        } else {
            MapFacilitiesResponse(source = "kakao", facilities = facilities)
        }
    }

    private fun fetchFacilities(
        category: FacilityCategory?,
        keyword: String,
        latitude: Double,
        longitude: Double,
    ): List<RawFacility> {
        val categories = category?.let { listOf(it) } ?: FacilityCategory.entries
        val futures = categories.map { searchCategory ->
            CompletableFuture.supplyAsync(
                { searchCategoryFacilities(searchCategory, keyword, latitude, longitude) },
                facilitySearchExecutor,
            )
        }
        return futures.flatMap { it.join() }
    }

    private fun searchCategoryFacilities(
        category: FacilityCategory,
        keyword: String,
        latitude: Double,
        longitude: Double,
    ): List<RawFacility> {
        return try {
            facilitySearchClient.search(
                category = category,
                query = buildQuery(category, keyword),
                latitude = latitude,
                longitude = longitude,
            )
        } catch (e: Exception) {
            log.warn("카카오 로컬 API 호출 실패(category={}), 해당 카테고리는 결과 없음으로 처리합니다.", category, e)
            emptyList()
        }
    }

    private fun buildQuery(
        category: FacilityCategory,
        keyword: String,
    ): String {
        return if (keyword.isBlank()) category.defaultKeyword else "$keyword ${category.defaultKeyword}"
    }

    private fun dedupAndSort(rawFacilities: List<RawFacility>): List<FacilityResponse> {
        val deduped = rawFacilities
            .groupBy { Triple(it.category, it.name, it.roadAddress.ifBlank { it.address }) }
            .map { (_, group) -> checkNotNull(group.minByOrNull { it.distanceMeters }) }
            .sortedBy { it.distanceMeters }

        return deduped.mapIndexed { index, facility -> facility.toResponse(index) }
    }

    private fun RawFacility.toResponse(index: Int): FacilityResponse {
        val is24Hours = name.contains("24") || categoryName.contains("24")
        return FacilityResponse(
            id = "${category.code}-$name-$index-$latitude-$longitude",
            name = name,
            category = category.code,
            address = address,
            roadAddress = roadAddress,
            latitude = latitude,
            longitude = longitude,
            distanceMeters = distanceMeters,
            phoneNumber = phoneNumber,
            is24Hours = is24Hours,
            status = if (is24Hours) "open" else "unknown",
            placeUrl = placeUrl,
        )
    }

    private fun mockResponse(category: FacilityCategory?): MapFacilitiesResponse {
        val mockFacilities = MOCK_FACILITIES.filter { category == null || it.category == category }
        return MapFacilitiesResponse(
            source = "mock",
            facilities = mockFacilities.mapIndexed { index, facility -> facility.toResponse(index) },
        )
    }

    private fun requireValidCoordinates(
        latitude: Double?,
        longitude: Double?,
    ): Pair<Double, Double> {
        if (latitude == null || longitude == null ||
            latitude !in MIN_LATITUDE..MAX_LATITUDE ||
            longitude !in MIN_LONGITUDE..MAX_LONGITUDE
        ) {
            throw BusinessException(ErrorCode.INVALID_COORDINATES)
        }
        return latitude to longitude
    }

    private fun resolveCategory(category: String): FacilityCategory? {
        if (category == ALL_CATEGORY) return null
        return FacilityCategory.fromCode(category) ?: throw BusinessException(ErrorCode.INVALID_FACILITY_CATEGORY)
    }

    private companion object {
        val log = LoggerFactory.getLogger(MapFacilityQueryService::class.java)
        const val ALL_CATEGORY = "all"
        const val MIN_LATITUDE = -90.0
        const val MAX_LATITUDE = 90.0
        const val MIN_LONGITUDE = -180.0
        const val MAX_LONGITUDE = 180.0
        const val EXTERNAL_CALL_POOL_SIZE = 4

        val MOCK_FACILITIES = listOf(
            RawFacility(
                name = "24시 서울약국",
                category = FacilityCategory.PHARMACY,
                address = "서울시 강남구 테헤란로 123",
                roadAddress = "서울시 강남구 테헤란로 123",
                latitude = 37.497941,
                longitude = 127.027618,
                distanceMeters = 250,
                phoneNumber = "02-1234-5678",
                categoryName = "의료,건강 > 약국",
                placeUrl = null,
            ),
            RawFacility(
                name = "24시 세이프메디 응급실",
                category = FacilityCategory.EMERGENCY,
                address = "서울시 강남구 테헤란로 456",
                roadAddress = "서울시 강남구 테헤란로 456",
                latitude = 37.4985,
                longitude = 127.0282,
                distanceMeters = 620,
                phoneNumber = "02-9876-5432",
                categoryName = "의료,건강 > 병원 > 응급실",
                placeUrl = null,
            ),
        )
    }
}
