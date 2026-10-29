package com.safemedi.app.sefemedi.domain.auth.controller

import com.safemedi.app.sefemedi.domain.auth.dto.LogoutRequest
import com.safemedi.app.sefemedi.domain.auth.dto.LogoutResponse
import com.safemedi.app.sefemedi.domain.auth.dto.TokenReissueRequest
import com.safemedi.app.sefemedi.domain.auth.service.AuthService
import com.safemedi.app.sefemedi.global.error.BusinessException
import com.safemedi.app.sefemedi.global.error.ErrorCode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.springframework.security.core.Authentication

class AuthControllerTest {

    private val authService = mock(AuthService::class.java)
    private val authController = AuthController(authService)

    @Test
    fun `logout passes authenticated social id to service`() {
        val authentication = mock(Authentication::class.java)
        val request = LogoutRequest(
            deviceToken = "device-token",
        )
        val response = LogoutResponse()

        given(authentication.name).willReturn("4903042739")
        given(authentication.isAuthenticated).willReturn(true)
        given(authService.logout("4903042739", "access-token", request)).willReturn(response)

        val result = authController.logout(authentication, "Bearer access-token", request)

        assertEquals(response, result)
        verify(authService).logout("4903042739", "access-token", request)
    }

    @Test
    fun `logout rejects anonymous authentication`() {
        val authentication = mock(Authentication::class.java)
        val request = LogoutRequest(
            deviceToken = "device-token",
        )

        given(authentication.isAuthenticated).willReturn(false)

        val exception = assertThrows(BusinessException::class.java) {
            authController.logout(authentication, "Bearer access-token", request)
        }

        assertEquals(ErrorCode.INVALID_TOKEN, exception.errorCode)
    }

    @Test
    fun `인증 객체가 없으면 표준 인증 예외를 반환한다`() {
        val exception = assertThrows(BusinessException::class.java) {
            authController.logout(null, null, LogoutRequest())
        }
        assertEquals(ErrorCode.INVALID_TOKEN, exception.errorCode)
    }

    @Test
    fun `reissue rejects missing refresh token`() {
        val exception = assertThrows(BusinessException::class.java) {
            authController.reissue(TokenReissueRequest())
        }

        assertEquals(ErrorCode.REFRESH_TOKEN_REQUIRED, exception.errorCode)
    }

    @Test
    fun `reissue rejects blank refresh token`() {
        val exception = assertThrows(BusinessException::class.java) {
            authController.reissue(
                TokenReissueRequest(
                    refreshToken = "   ",
                ),
            )
        }

        assertEquals(ErrorCode.REFRESH_TOKEN_REQUIRED, exception.errorCode)
    }
}
