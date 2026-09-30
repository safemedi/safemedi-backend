package com.safemedi.app.sefemedi.domain.medication.controller

import com.safemedi.app.sefemedi.domain.medication.dto.TodayMedicationScheduleResponse
import com.safemedi.app.sefemedi.domain.medication.dto.TodayMedicationSummaryResponse
import com.safemedi.app.sefemedi.domain.medication.service.MedicationRecordQueryService
import com.safemedi.app.sefemedi.domain.medication.service.MedicationRecordUpdateService
import com.safemedi.app.sefemedi.domain.medication.service.MedicationStatisticsService
import com.safemedi.app.sefemedi.domain.medication.service.TodayMedicationScheduleService
import com.safemedi.app.sefemedi.global.error.BusinessException
import com.safemedi.app.sefemedi.global.error.ErrorCode
import com.safemedi.app.sefemedi.global.error.GlobalExceptionHandler
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.time.LocalDate

class MedicationRecordControllerTest {
    private val service = mock(TodayMedicationScheduleService::class.java)
    private lateinit var mockMvc: MockMvc
    private val response = TodayMedicationScheduleResponse(
        date = LocalDate.of(2026, 9, 30),
        summary = TodayMedicationSummaryResponse(0, 0, 0),
        schedules = emptyList(),
    )

    @BeforeEach
    fun setUp() {
        val controller = MedicationRecordController(
            service,
            mock(MedicationRecordQueryService::class.java),
            mock(MedicationRecordUpdateService::class.java),
            mock(MedicationStatisticsService::class.java),
        )
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setCustomArgumentResolvers(AuthenticationPrincipalArgumentResolver())
            .setControllerAdvice(GlobalExceptionHandler())
            .build()
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken("kakao-123", null, emptyList())
    }

    @AfterEach
    fun tearDown() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `가족 파라미터를 생략하면 본인 스케줄을 조회한다`() {
        given(service.findTodaySchedules("kakao-123", null)).willReturn(response)

        mockMvc.perform(get("/api/v1/medication-records/today"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.summary.totalCount").value(0))
            .andExpect(jsonPath("$.schedules").isEmpty)

        verify(service).findTodaySchedules("kakao-123", null)
    }

    @Test
    fun `가족 파라미터를 서비스에 전달한다`() {
        given(service.findTodaySchedules("kakao-123", 10L)).willReturn(response)

        mockMvc.perform(get("/api/v1/medication-records/today").param("familyId", "10"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.summary.completionRate").value(0))

        verify(service).findTodaySchedules("kakao-123", 10L)
    }

    @Test
    fun `가족 접근 거부 시 403을 반환한다`() {
        given(service.findTodaySchedules("kakao-123", 10L))
            .willThrow(BusinessException(ErrorCode.FAMILY_ACCESS_DENIED))

        mockMvc.perform(get("/api/v1/medication-records/today").param("familyId", "10"))
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.code").value("FAM_003"))
    }

    @Test
    fun `인증 사용자가 없으면 401을 반환한다`() {
        SecurityContextHolder.clearContext()

        mockMvc.perform(get("/api/v1/medication-records/today").param("familyId", "10"))
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.code").value("AUTH_001"))

        verifyNoInteractions(service)
    }
}
