package com.safemedi.app.sefemedi.global.jwt

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class JwtProviderTest {

    private val jwtProvider =
        JwtProvider(
            secretKey = "01234567890123456789012345678901",
        )

    @Test
    fun `access token and refresh token are validated by different token types`() {
        val accessToken =
            jwtProvider.createAccessToken(
                "4903042739",
            )
        val refreshToken =
            jwtProvider.createRefreshToken(
                "4903042739",
            )

        assertTrue(jwtProvider.validateAccessToken(accessToken))
        assertFalse(jwtProvider.validateRefreshToken(accessToken))
        assertTrue(jwtProvider.validateRefreshToken(refreshToken))
        assertFalse(jwtProvider.validateAccessToken(refreshToken))
        assertEquals(
            "4903042739",
            jwtProvider.getKakaoId(refreshToken),
        )
    }
}
