package com.safemedi.app.sefemedi.domain.family.controller

import com.safemedi.app.sefemedi.domain.family.dto.FamilyListItemResponse
import com.safemedi.app.sefemedi.domain.family.dto.FamilyListResponse
import com.safemedi.app.sefemedi.domain.family.dto.FamilyRelationUpdateRequest
import com.safemedi.app.sefemedi.domain.family.dto.FamilyRelationUpdateResponse
import com.safemedi.app.sefemedi.domain.family.service.FamilyDisconnectService
import com.safemedi.app.sefemedi.domain.family.service.FamilyListQueryService
import com.safemedi.app.sefemedi.domain.family.service.FamilyRelationUpdateService
import com.safemedi.app.sefemedi.domain.user.dto.MedicalSummaryResponse
import com.safemedi.app.sefemedi.domain.user.service.MedicalSummaryService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.ResponseStatus
import java.time.Instant

class FamilyControllerTest {

    private val medicalSummaryService = mock(MedicalSummaryService::class.java)
    private val familyRelationUpdateService = mock(FamilyRelationUpdateService::class.java)
    private val familyDisconnectService = mock(FamilyDisconnectService::class.java)
    private val familyListQueryService = mock(FamilyListQueryService::class.java)
    private val controller = FamilyController(
        medicalSummaryService,
        familyRelationUpdateService,
        familyDisconnectService,
        familyListQueryService,
    )

    @Test
    fun `가족 목록 조회 요청을 인증 사용자로 서비스에 전달한다`() {
        val authentication = mock(Authentication::class.java)
        val response = FamilyListResponse(
            families = listOf(
                FamilyListItemResponse(
                    familyId = null,
                    name = "홍길동",
                    relation = "본인",
                )
            )
        )
        given(authentication.name).willReturn("kakao-123")
        given(familyListQueryService.getFamilies("kakao-123")).willReturn(response)

        val result = controller.getFamilies(authentication)

        assertEquals(response, result)
        verify(familyListQueryService).getFamilies("kakao-123")
    }

    @Test
    fun `가족 의료정보 조회 요청을 인증 사용자와 가족 ID로 서비스에 전달한다`() {
        val authentication = mock(Authentication::class.java)
        val response = MedicalSummaryResponse(
            name = "김영희",
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
        given(medicalSummaryService.getFamilyMedicalSummary("kakao-123", 9L)).willReturn(response)

        val result = controller.getFamilyMedicalSummary(authentication, 9L)

        assertEquals(response, result)
        verify(medicalSummaryService).getFamilyMedicalSummary("kakao-123", 9L)
    }

    @Test
    fun `호칭 수정 요청을 인증 사용자와 가족 ID로 서비스에 전달한다`() {
        val authentication = mock(Authentication::class.java)
        val request = FamilyRelationUpdateRequest(relation = "어머니")
        val response = FamilyRelationUpdateResponse(
            familyId = 12L,
            name = "김영희",
            relation = "어머니",
            updatedAt = Instant.parse("2026-07-15T06:10:00Z"),
        )

        given(authentication.name).willReturn("kakao-123")
        given(familyRelationUpdateService.update("kakao-123", 12L, request)).willReturn(response)

        val result = controller.updateRelation(authentication, 12L, request)

        assertEquals(response, result)
        verify(familyRelationUpdateService).update("kakao-123", 12L, request)
    }

    @Test
    fun `가족 연동 해제 요청을 인증 사용자와 가족 ID로 서비스에 전달한다`() {
        val authentication = mock(Authentication::class.java)
        given(authentication.name).willReturn("kakao-123")

        controller.disconnect(authentication, 12L)

        verify(familyDisconnectService).disconnect("kakao-123", 12L)
    }

    @Test
    fun `가족 연동 해제 성공 상태는 204이다`() {
        val method = FamilyController::class.java.getMethod(
            "disconnect",
            Authentication::class.java,
            Long::class.javaPrimitiveType,
        )

        val responseStatus = method.getAnnotation(ResponseStatus::class.java)

        assertEquals(HttpStatus.NO_CONTENT, responseStatus.value)
    }
}
