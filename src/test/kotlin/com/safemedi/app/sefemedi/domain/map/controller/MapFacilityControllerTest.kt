package com.safemedi.app.sefemedi.domain.map.controller

import com.safemedi.app.sefemedi.domain.map.dto.FacilityResponse
import com.safemedi.app.sefemedi.domain.map.dto.MapFacilitiesResponse
import com.safemedi.app.sefemedi.domain.map.service.MapFacilityQueryService
import com.safemedi.app.sefemedi.global.error.BusinessException
import com.safemedi.app.sefemedi.global.error.ErrorCode
import com.safemedi.app.sefemedi.global.error.GlobalExceptionHandler
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders

class MapFacilityControllerTest {
    private lateinit var mockMvc: MockMvc
    private lateinit var mapFacilityQueryService: MapFacilityQueryService

    @BeforeEach
    fun setUp() {
        mapFacilityQueryService = mock(MapFacilityQueryService::class.java)
        mockMvc = MockMvcBuilders.standaloneSetup(MapFacilityController(mapFacilityQueryService))
            .setControllerAdvice(GlobalExceptionHandler())
            .build()
    }

    @Test
    fun `getFacilities returns proxied facility list`() {
        val mockResponse = MapFacilitiesResponse(
            source = "kakao",
            facilities = listOf(
                FacilityResponse(
                    id = "pharmacy-온누리약국-0-37.497821-127.027102",
                    name = "온누리약국",
                    category = "pharmacy",
                    address = "서울 강남구 역삼동 123-45",
                    roadAddress = "서울 강남구 테헤란로 123",
                    latitude = 37.497821,
                    longitude = 127.027102,
                    distanceMeters = 180,
                    phoneNumber = "02-1234-5678",
                    is24Hours = false,
                    status = "unknown",
                    placeUrl = "http://place.map.kakao.com/123456",
                )
            ),
        )
        given(
            mapFacilityQueryService.getFacilities(37.497941, 127.027618, "all", "")
        ).willReturn(mockResponse)

        val resultActions = mockMvc.perform(
            get("/api/v1/map/facilities")
                .param("latitude", "37.497941")
                .param("longitude", "127.027618")
                .accept(MediaType.APPLICATION_JSON)
        )

        resultActions
            .andExpect(status().isOk)
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.source").value("kakao"))
            .andExpect(jsonPath("$.facilities[0].id").value("pharmacy-온누리약국-0-37.497821-127.027102"))
            .andExpect(jsonPath("$.facilities[0].category").value("pharmacy"))
            .andExpect(jsonPath("$.facilities[0].distanceMeters").value(180))
    }

    @Test
    fun `missing coordinates returns validation error`() {
        given(
            mapFacilityQueryService.getFacilities(null, null, "all", "")
        ).willThrow(BusinessException(ErrorCode.INVALID_COORDINATES))

        val resultActions = mockMvc.perform(
            get("/api/v1/map/facilities")
                .accept(MediaType.APPLICATION_JSON)
        )

        resultActions
            .andExpect(status().isBadRequest)
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.code").value("VAL_009"))
            .andExpect(jsonPath("$.message").value(ErrorCode.INVALID_COORDINATES.message))
    }

    @Test
    fun `latitude가 숫자 형식이 아니면 400을 반환한다`() {
        val resultActions = mockMvc.perform(
            get("/api/v1/map/facilities")
                .param("latitude", "abc")
                .param("longitude", "127.027618")
                .accept(MediaType.APPLICATION_JSON)
        )

        resultActions
            .andExpect(status().isBadRequest)
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.code").value("VAL_001"))
    }

    @Test
    fun `invalid category returns validation error`() {
        given(
            mapFacilityQueryService.getFacilities(37.497941, 127.027618, "hospital", "")
        ).willThrow(BusinessException(ErrorCode.INVALID_FACILITY_CATEGORY))

        val resultActions = mockMvc.perform(
            get("/api/v1/map/facilities")
                .param("latitude", "37.497941")
                .param("longitude", "127.027618")
                .param("category", "hospital")
                .accept(MediaType.APPLICATION_JSON)
        )

        resultActions
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("VAL_010"))
            .andExpect(jsonPath("$.message").value(ErrorCode.INVALID_FACILITY_CATEGORY.message))
    }
}
