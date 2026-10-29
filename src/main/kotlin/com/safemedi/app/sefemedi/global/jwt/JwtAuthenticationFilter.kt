package com.safemedi.app.sefemedi.global.jwt

import com.safemedi.app.sefemedi.domain.auth.service.AccessTokenBlacklistService
import io.jsonwebtoken.JwtException
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthenticationFilter(

    private val jwtProvider: JwtProvider,
    private val accessTokenBlacklistService: AccessTokenBlacklistService,
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val token = resolveToken(request)

        if (
            token != null &&
            jwtProvider.validateAccessToken(token) &&
            !accessTokenBlacklistService.contains(token) &&
            SecurityContextHolder.getContext().authentication == null
        ) {
            val kakaoId = try {
                jwtProvider.getKakaoId(token)
            } catch (_: JwtException) {
                null
            }

            if (kakaoId != null) {
                val authentication =
                    UsernamePasswordAuthenticationToken(
                        kakaoId,
                        null,
                        emptyList()
                    )

                SecurityContextHolder.getContext().authentication =
                    authentication
            }
        }

        filterChain.doFilter(
            request,
            response
        )
    }

    private fun resolveToken(
        request: HttpServletRequest
    ): String? {
        val authorization =
            request.getHeader("Authorization")

        if (
            authorization == null ||
            !authorization.startsWith("Bearer ")
        ) {
            return null
        }

        return authorization.substring(7)
    }
}
