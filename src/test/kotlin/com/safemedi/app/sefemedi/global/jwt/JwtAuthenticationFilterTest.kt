package com.safemedi.app.sefemedi.global.jwt

import com.safemedi.app.sefemedi.domain.auth.service.AccessTokenBlacklistService
import io.jsonwebtoken.ExpiredJwtException
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.springframework.security.core.context.SecurityContextHolder

class JwtAuthenticationFilterTest {

    private val jwtProvider = JwtProvider(secretKey = "01234567890123456789012345678901")
    private val accessTokenBlacklistService = mock(AccessTokenBlacklistService::class.java)
    private val filter = JwtAuthenticationFilter(jwtProvider, accessTokenBlacklistService)

    @AfterEach
    fun clearContext() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `valid non-blacklisted access token authenticates the request`() {
        val accessToken = jwtProvider.createAccessToken("4903042739")
        val request = mock(HttpServletRequest::class.java)
        val response = mock(HttpServletResponse::class.java)
        val filterChain = mock(FilterChain::class.java)

        given(request.getHeader("Authorization")).willReturn("Bearer $accessToken")
        given(accessTokenBlacklistService.contains(accessToken)).willReturn(false)

        filter.doFilter(request, response, filterChain)

        assertEquals(
            "4903042739",
            SecurityContextHolder.getContext().authentication?.name,
        )
        verify(filterChain).doFilter(request, response)
    }

    @Test
    fun `refresh token does not authenticate the request`() {
        val refreshToken = jwtProvider.createRefreshToken("4903042739")
        val request = mock(HttpServletRequest::class.java)
        val response = mock(HttpServletResponse::class.java)
        val filterChain = mock(FilterChain::class.java)

        given(request.getHeader("Authorization")).willReturn("Bearer $refreshToken")

        filter.doFilter(request, response, filterChain)

        assertNull(SecurityContextHolder.getContext().authentication)
        verify(filterChain).doFilter(request, response)
    }

    @Test
    fun `blacklisted access token does not authenticate the request`() {
        val accessToken = jwtProvider.createAccessToken("4903042739")
        val request = mock(HttpServletRequest::class.java)
        val response = mock(HttpServletResponse::class.java)
        val filterChain = mock(FilterChain::class.java)

        given(request.getHeader("Authorization")).willReturn("Bearer $accessToken")
        given(accessTokenBlacklistService.contains(accessToken)).willReturn(true)

        filter.doFilter(request, response, filterChain)

        assertNull(SecurityContextHolder.getContext().authentication)
        verify(filterChain).doFilter(request, response)
    }

    @Test
    fun `token expiring between validate and identity check does not authenticate or break the chain`() {
        val racyJwtProvider = mock(JwtProvider::class.java)
        val racyFilter = JwtAuthenticationFilter(racyJwtProvider, accessTokenBlacklistService)
        val request = mock(HttpServletRequest::class.java)
        val response = mock(HttpServletResponse::class.java)
        val filterChain = mock(FilterChain::class.java)

        given(request.getHeader("Authorization")).willReturn("Bearer access-token")
        given(racyJwtProvider.validateAccessToken("access-token")).willReturn(true)
        given(accessTokenBlacklistService.contains("access-token")).willReturn(false)
        given(racyJwtProvider.getKakaoId("access-token"))
            .willThrow(ExpiredJwtException(null, null, "expired"))

        racyFilter.doFilter(request, response, filterChain)

        assertNull(SecurityContextHolder.getContext().authentication)
        verify(filterChain).doFilter(request, response)
    }
}
