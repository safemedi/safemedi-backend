package com.safemedi.app.sefemedi.domain.auth.controller

import com.safemedi.app.sefemedi.domain.auth.dto.LoginResponse
import com.safemedi.app.sefemedi.domain.auth.dto.LogoutRequest
import com.safemedi.app.sefemedi.domain.auth.dto.LogoutResponse
import com.safemedi.app.sefemedi.domain.auth.dto.SocialLoginRequest
import com.safemedi.app.sefemedi.domain.auth.dto.TokenReissueRequest
import com.safemedi.app.sefemedi.domain.auth.dto.TokenResponse
import com.safemedi.app.sefemedi.domain.auth.service.AuthService
import com.safemedi.app.sefemedi.global.error.BusinessException
import com.safemedi.app.sefemedi.global.error.ErrorCode
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(path = ["/auth", "/api/v1/auth"])
class AuthController(

    private val authService: AuthService
) {

    @PostMapping("/login/{provider}")
    fun login(
        @PathVariable provider: String,
        @RequestBody request: SocialLoginRequest
    ): LoginResponse {
        return authService.login(
            provider = provider,
            accessToken = request.accessToken
        )
    }

    @PostMapping("/reissue")
    fun reissue(
        @RequestBody request: TokenReissueRequest
    ): TokenResponse {
        val refreshToken = requireRefreshToken(request.refreshToken)
        return authService.reissue(
            refreshToken
        )
    }

    @RequestMapping(
        "/logout",
        method = [RequestMethod.POST, RequestMethod.DELETE],
    )
    fun logout(
        authentication: Authentication?,
        @RequestHeader("Authorization", required = false) authorization: String?,
        @RequestBody request: LogoutRequest,
    ): LogoutResponse {
        return authService.logout(
            socialId = requireAuthenticatedSocialId(authentication),
            accessToken = requireBearerToken(authorization),
            request = request,
        )
    }

    private fun requireAuthenticatedSocialId(
        authentication: Authentication?,
    ): String {
        if (authentication == null || authentication is AnonymousAuthenticationToken || !authentication.isAuthenticated) {
            throw BusinessException(ErrorCode.INVALID_TOKEN)
        }

        val socialId = authentication.name.trim()
        if (socialId.isBlank() || socialId == ANONYMOUS_PRINCIPAL_NAME) {
            throw BusinessException(ErrorCode.INVALID_TOKEN)
        }

        return socialId
    }

    private fun requireBearerToken(
        authorization: String?,
    ): String {
        if (
            authorization == null ||
            !authorization.startsWith(BEARER_PREFIX)
        ) {
            throw BusinessException(ErrorCode.INVALID_TOKEN)
        }

        val token = authorization.substring(BEARER_PREFIX.length).trim()
        if (token.isBlank()) {
            throw BusinessException(ErrorCode.INVALID_TOKEN)
        }

        return token
    }

    private fun requireRefreshToken(
        refreshToken: String?,
    ): String {
        val normalizedRefreshToken = refreshToken?.trim()
        if (normalizedRefreshToken.isNullOrBlank()) {
            throw BusinessException(ErrorCode.REFRESH_TOKEN_REQUIRED)
        }

        return normalizedRefreshToken
    }

    private companion object {
        const val ANONYMOUS_PRINCIPAL_NAME = "anonymousUser"
        const val BEARER_PREFIX = "Bearer "
    }
}
