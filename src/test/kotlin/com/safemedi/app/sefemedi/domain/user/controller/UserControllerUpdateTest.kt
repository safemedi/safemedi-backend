package com.safemedi.app.sefemedi.domain.user.controller

import com.safemedi.app.sefemedi.domain.user.dto.UserNotificationSettingsResponse
import com.safemedi.app.sefemedi.domain.user.dto.UserProfileUpdateRequest
import com.safemedi.app.sefemedi.domain.user.dto.UserProfileResponse
import com.safemedi.app.sefemedi.domain.user.service.UserService
import com.safemedi.app.sefemedi.domain.user.service.UserWithdrawalService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.springframework.security.core.Authentication

class UserControllerUpdateTest {

    private val userService = mock(UserService::class.java)
    private val userWithdrawalService = mock(UserWithdrawalService::class.java)
    private val userController = UserController(userService, userWithdrawalService)

    @Test
    fun `updateMyProfile passes authentication name to service`() {
        val authentication = mock(Authentication::class.java)
        val request = UserProfileUpdateRequest(
            nickname = "홍길동",
            gender = "MALE",
            bloodType = "O",
            rhType = "PLUS",
            diseaseCodes = listOf("J30"),
            allergies = emptyList(),
        )
        val response = UserProfileResponse(
            nickname = "홍길동",
            inviteCode = "A8F9K2",
            birthDate = "1985-03-15",
            gender = null,
            height = null,
            weight = null,
            bloodType = null,
            rhType = null,
            isTutorialCompleted = false,
            diseases = emptyList(),
            allergies = emptyList(),
            families = emptyList(),
            settings = UserNotificationSettingsResponse(
                isMyReminderOn = true,
                isFamilyReminderOn = true,
                isMissedAlertOn = true,
            ),
        )

        given(authentication.name).willReturn("4903042739")
        given(userService.updateMyProfile("4903042739", request)).willReturn(response)

        val result = userController.updateMyProfile(authentication, request)

        assertEquals(response, result)
        verify(userService).updateMyProfile("4903042739", request)
    }
}
