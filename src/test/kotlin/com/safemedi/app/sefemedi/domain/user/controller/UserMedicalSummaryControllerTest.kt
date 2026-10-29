package com.safemedi.app.sefemedi.domain.user.controller

import com.safemedi.app.sefemedi.domain.user.dto.MedicalSummaryResponse
import com.safemedi.app.sefemedi.domain.user.service.MedicalSummaryService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.springframework.security.core.Authentication

class UserMedicalSummaryControllerTest {

    private val medicalSummaryService = mock(MedicalSummaryService::class.java)
    private val controller = UserMedicalSummaryController(medicalSummaryService)

    @Test
    fun `getMyMedicalSummary passes authentication name to service`() {
        val authentication = mock(Authentication::class.java)
        val response = MedicalSummaryResponse(
            name = "정민성",
            birthDate = null,
            gender = null,
            height = null,
            weight = null,
            bloodType = null,
            rhType = null,
            diseases = emptyList(),
            allergies = emptyList(),
            activeMedications = emptyList(),
        )

        given(authentication.name).willReturn("kakao-123")
        given(medicalSummaryService.getMyMedicalSummary("kakao-123")).willReturn(response)

        val result = controller.getMyMedicalSummary(authentication)

        assertEquals(response, result)
        verify(medicalSummaryService).getMyMedicalSummary("kakao-123")
    }
}
