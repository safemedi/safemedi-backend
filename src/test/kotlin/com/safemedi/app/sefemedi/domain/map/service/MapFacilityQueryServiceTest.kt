package com.safemedi.app.sefemedi.domain.map.service

import com.safemedi.app.sefemedi.domain.map.client.FacilityCategory
import com.safemedi.app.sefemedi.domain.map.client.MedicalFacilitySearchClient
import com.safemedi.app.sefemedi.domain.map.client.RawFacility
import com.safemedi.app.sefemedi.global.error.BusinessException
import com.safemedi.app.sefemedi.global.error.ErrorCode
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class MapFacilityQueryServiceTest {
    private lateinit var facilitySearchClient: MedicalFacilitySearchClient
    private lateinit var mapFacilityQueryService: MapFacilityQueryService

    private val latitude = 37.497941
    private val longitude = 127.027618

    @BeforeEach
    fun setUp() {
        facilitySearchClient = mock(MedicalFacilitySearchClient::class.java)
        mapFacilityQueryService = MapFacilityQueryService(facilitySearchClient)
    }

    @Test
    fun `latitude가 없으면 INVALID_COORDINATES 예외가 발생한다`() {
        val exception = assertFailsWith<BusinessException> {
            mapFacilityQueryService.getFacilities(null, longitude, "all", "")
        }

        assertEquals(ErrorCode.INVALID_COORDINATES, exception.errorCode)
    }

    @Test
    fun `latitude가 유효 범위를 벗어나면 INVALID_COORDINATES 예외가 발생한다`() {
        val exception = assertFailsWith<BusinessException> {
            mapFacilityQueryService.getFacilities(100.0, longitude, "all", "")
        }

        assertEquals(ErrorCode.INVALID_COORDINATES, exception.errorCode)
    }

    @Test
    fun `category가 all-pharmacy-emergency가 아니면 INVALID_FACILITY_CATEGORY 예외가 발생한다`() {
        val exception = assertFailsWith<BusinessException> {
            mapFacilityQueryService.getFacilities(latitude, longitude, "hospital", "")
        }

        assertEquals(ErrorCode.INVALID_FACILITY_CATEGORY, exception.errorCode)
    }

    @Test
    fun `상호명과 도로명주소가 같으면 거리가 더 가까운 항목만 남긴다`() {
        given(
            facilitySearchClient.search(FacilityCategory.PHARMACY, "약국", latitude, longitude)
        ).willReturn(
            listOf(
                pharmacy(name = "온누리약국", roadAddress = "서울 강남구 테헤란로 123", distanceMeters = 300),
                pharmacy(name = "온누리약국", roadAddress = "서울 강남구 테헤란로 123", distanceMeters = 180),
            )
        )

        val response = mapFacilityQueryService.getFacilities(latitude, longitude, "pharmacy", "")

        assertEquals(1, response.facilities.size)
        assertEquals(180, response.facilities[0].distanceMeters)
    }

    @Test
    fun `category가 all이면 약국과 응급실을 합쳐 거리순으로 정렬한다`() {
        given(
            facilitySearchClient.search(FacilityCategory.PHARMACY, "약국", latitude, longitude)
        ).willReturn(listOf(pharmacy(name = "온누리약국", roadAddress = "테헤란로 123", distanceMeters = 950)))
        given(
            facilitySearchClient.search(FacilityCategory.EMERGENCY, "응급실", latitude, longitude)
        ).willReturn(listOf(emergency(name = "강남세브란스병원 응급실", roadAddress = "언주로 211", distanceMeters = 180)))

        val response = mapFacilityQueryService.getFacilities(latitude, longitude, "all", "")

        assertEquals("kakao", response.source)
        assertEquals(listOf("강남세브란스병원 응급실", "온누리약국"), response.facilities.map { it.name })
    }

    @Test
    fun `category가 all이고 한 카테고리만 실패하면 성공한 카테고리 결과는 유지된다`() {
        given(
            facilitySearchClient.search(FacilityCategory.PHARMACY, "약국", latitude, longitude)
        ).willReturn(listOf(pharmacy(name = "온누리약국", roadAddress = "테헤란로 123", distanceMeters = 300)))
        given(
            facilitySearchClient.search(FacilityCategory.EMERGENCY, "응급실", latitude, longitude)
        ).willThrow(RuntimeException("카카오 API 오류"))

        val response = mapFacilityQueryService.getFacilities(latitude, longitude, "all", "")

        assertEquals("kakao", response.source)
        assertEquals(listOf("온누리약국"), response.facilities.map { it.name })
    }

    @Test
    fun `상호명과 도로명주소가 같아도 카테고리가 다르면 둘 다 남긴다`() {
        given(
            facilitySearchClient.search(FacilityCategory.PHARMACY, "약국", latitude, longitude)
        ).willReturn(listOf(pharmacy(name = "온누리약국", roadAddress = "테헤란로 123", distanceMeters = 300)))
        given(
            facilitySearchClient.search(FacilityCategory.EMERGENCY, "응급실", latitude, longitude)
        ).willReturn(listOf(emergency(name = "온누리약국", roadAddress = "테헤란로 123", distanceMeters = 900)))

        val response = mapFacilityQueryService.getFacilities(latitude, longitude, "all", "")

        assertEquals(2, response.facilities.size)
    }

    @Test
    fun `카카오 API 호출이 실패하면 mock 데이터를 반환한다`() {
        given(
            facilitySearchClient.search(FacilityCategory.PHARMACY, "약국", latitude, longitude)
        ).willThrow(RuntimeException("카카오 API 오류"))

        val response = mapFacilityQueryService.getFacilities(latitude, longitude, "pharmacy", "")

        assertEquals("mock", response.source)
        assertEquals(true, response.facilities.isNotEmpty())
    }

    @Test
    fun `검색 결과가 없으면 mock 데이터를 반환한다`() {
        given(
            facilitySearchClient.search(FacilityCategory.PHARMACY, "약국", latitude, longitude)
        ).willReturn(emptyList())

        val response = mapFacilityQueryService.getFacilities(latitude, longitude, "pharmacy", "")

        assertEquals("mock", response.source)
    }

    private fun pharmacy(
        name: String,
        roadAddress: String,
        distanceMeters: Int,
    ) = RawFacility(
        name = name,
        category = FacilityCategory.PHARMACY,
        address = roadAddress,
        roadAddress = roadAddress,
        latitude = latitude,
        longitude = longitude,
        distanceMeters = distanceMeters,
        phoneNumber = "02-1234-5678",
        categoryName = "의료,건강 > 약국",
        placeUrl = null,
    )

    private fun emergency(
        name: String,
        roadAddress: String,
        distanceMeters: Int,
    ) = RawFacility(
        name = name,
        category = FacilityCategory.EMERGENCY,
        address = roadAddress,
        roadAddress = roadAddress,
        latitude = latitude,
        longitude = longitude,
        distanceMeters = distanceMeters,
        phoneNumber = "02-2019-2114",
        categoryName = "의료,건강 > 병원 > 응급실",
        placeUrl = null,
    )
}
