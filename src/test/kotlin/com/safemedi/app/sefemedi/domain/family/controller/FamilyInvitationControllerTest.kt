package com.safemedi.app.sefemedi.domain.family.controller

import com.safemedi.app.sefemedi.domain.family.dto.FamilyInvitationAcceptResponse
import com.safemedi.app.sefemedi.domain.family.dto.FamilyInvitationCreateResponse
import com.safemedi.app.sefemedi.domain.family.dto.FamilyInvitationInfoResponse
import com.safemedi.app.sefemedi.domain.family.entity.FamilyInvitationStatus
import com.safemedi.app.sefemedi.domain.family.service.FamilyInvitationCreateResult
import com.safemedi.app.sefemedi.domain.family.service.FamilyInvitationCreateService
import com.safemedi.app.sefemedi.domain.family.service.FamilyInvitationQueryService
import com.safemedi.app.sefemedi.domain.family.service.FamilyInvitationAcceptService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import java.time.Instant

class FamilyInvitationControllerTest {
    private val service = mock(FamilyInvitationCreateService::class.java)
    private val queryService = mock(FamilyInvitationQueryService::class.java)
    private val acceptService = mock(FamilyInvitationAcceptService::class.java)
    private val controller = FamilyInvitationController(service, queryService, acceptService)
    private val authentication = mock(Authentication::class.java)
    private val response = FamilyInvitationCreateResponse(
        invitationId = 100L,
        inviteUrl = "https://invite.example.com/invite/token",
        status = FamilyInvitationStatus.PENDING,
        createdAt = Instant.parse("2026-07-15T04:00:00Z"),
        expiresAt = Instant.parse("2026-07-16T04:00:00Z"),
    )

    @Test
    fun `createInvitation returns 201 when a new invitation is created`() {
        given(authentication.name).willReturn("kakao-123")
        given(service.create("kakao-123")).willReturn(
            FamilyInvitationCreateResult(response = response, created = true)
        )

        val result = controller.createInvitation(authentication)

        assertEquals(HttpStatus.CREATED, result.statusCode)
        assertEquals(response, result.body)
    }

    @Test
    fun `createInvitation returns 200 when an active invitation is reused`() {
        given(authentication.name).willReturn("kakao-123")
        given(service.create("kakao-123")).willReturn(
            FamilyInvitationCreateResult(response = response, created = false)
        )

        val result = controller.createInvitation(authentication)

        assertEquals(HttpStatus.OK, result.statusCode)
        assertEquals(response, result.body)
    }

    @Test
    fun `getInvitationInfo returns invitation information for authenticated user`() {
        val infoResponse = FamilyInvitationInfoResponse(
            inviterName = "홍길동",
            expiresAt = Instant.parse("2026-07-16T06:00:00Z"),
        )
        given(authentication.name).willReturn("kakao-123")
        given(queryService.getInvitationInfo("kakao-123", "invitation-token")).willReturn(infoResponse)

        val result = controller.getInvitationInfo(authentication, "invitation-token")

        assertEquals(infoResponse, result)
    }

    @Test
    fun `acceptInvitation returns accepted family information`() {
        val acceptResponse = FamilyInvitationAcceptResponse(
            familyId = 12L,
            name = "홍길동",
            relation = "가족",
            connectedAt = Instant.parse("2026-07-15T06:05:00Z"),
        )
        given(authentication.name).willReturn("kakao-123")
        given(acceptService.accept("kakao-123", "invitation-token")).willReturn(acceptResponse)

        val result = controller.acceptInvitation(authentication, "invitation-token")

        assertEquals(acceptResponse, result)
    }
}
