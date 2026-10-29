package com.safemedi.app.sefemedi.domain.user.controller

import com.safemedi.app.sefemedi.domain.user.dto.TutorialRequest
import com.safemedi.app.sefemedi.domain.user.dto.TutorialResponse
import com.safemedi.app.sefemedi.domain.user.dto.UserNotificationSettingsResponse
import com.safemedi.app.sefemedi.domain.user.dto.UserProfileResponse
import com.safemedi.app.sefemedi.domain.user.dto.UserWithdrawalResponse
import com.safemedi.app.sefemedi.domain.user.service.UserService
import com.safemedi.app.sefemedi.domain.user.service.UserWithdrawalService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.springframework.security.core.Authentication

class UserControllerTest {

    private val userService = mock(UserService::class.java)
    private val userWithdrawalService = mock(UserWithdrawalService::class.java)
    private val userController = UserController(userService, userWithdrawalService)

    @Test
    fun `completeTutorial passes authentication name to service`() {
        val authentication = mock(Authentication::class.java)
        val request = TutorialRequest(
            birthDate = "1985-03-15",
            gender = "MALE",
        )
        val response = TutorialResponse(
            isTutorialCompleted = true,
        )

        given(authentication.name).willReturn("4903042739")
        given(authentication.principal).willReturn(
            object {
                override fun toString(): String = "wrong-principal-value"
            }
        )
        given(userService.completeTutorial("4903042739", request)).willReturn(response)

        val result = userController.completeTutorial(authentication, request)

        assertEquals(response, result)
        verify(userService).completeTutorial("4903042739", request)
    }

    @Test
    fun `getMyProfile passes authentication name to service`() {
        val authentication = mock(Authentication::class.java)
        val response = UserProfileResponse(
            nickname = "홍길동",
            inviteCode = "A8F9K2",
            birthDate = "1985-03-15",
            gender = null,
            height = 180,
            weight = 75,
            bloodType = null,
            rhType = null,
            isTutorialCompleted = true,
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
        given(userService.getMyProfile("4903042739")).willReturn(response)

        val result = userController.getMyProfile(authentication)

        assertEquals(response, result)
        verify(userService).getMyProfile("4903042739")
    }

    @Test
    fun `getNotificationSettings passes authentication name to service`() {
        val authentication = mock(Authentication::class.java)
        val response = UserNotificationSettingsResponse(
            isMyReminderOn = true,
            isFamilyReminderOn = false,
            isMissedAlertOn = true,
        )

        given(authentication.name).willReturn("4903042739")
        given(userService.getNotificationSettings("4903042739")).willReturn(response)

        val result = userController.getNotificationSettings(authentication)

        assertEquals(response, result)
        verify(userService).getNotificationSettings("4903042739")
    }

    @Test
    fun `withdrawMyAccount passes authentication name to withdrawal service`() {
        val authentication = mock(Authentication::class.java)
        val response = UserWithdrawalResponse()

        given(authentication.name).willReturn("4903042739")
        given(userWithdrawalService.withdrawMyAccount("4903042739")).willReturn(response)

        val result = userController.withdrawMyAccount(authentication)

        assertEquals(response, result)
        verify(userWithdrawalService).withdrawMyAccount("4903042739")
    }
}
