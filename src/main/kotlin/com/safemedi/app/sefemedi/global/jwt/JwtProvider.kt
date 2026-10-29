package com.safemedi.app.sefemedi.global.jwt

import io.jsonwebtoken.Claims
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.*

sealed interface TokenParseResult {
    data class Success(
        val subject: String,
    ) : TokenParseResult

    object Expired : TokenParseResult

    object Invalid : TokenParseResult
}

@Component
class JwtProvider(

    @param:Value("\${jwt.secret}")
    private val secretKey: String
) {

    private val accessKey =
        Keys.hmacShaKeyFor(
            "$secretKey:access".toByteArray()
        )

    private val refreshKey =
        Keys.hmacShaKeyFor(
            "$secretKey:refresh".toByteArray()
        )

    private val accessTokenExpirationMillis =
        1000L * 60 * 60 * 24

    private val refreshTokenExpirationMillis =
        1000L * 60 * 60 * 24 * 14

    fun createAccessToken(
        kakaoId: String
    ): String {
        return createToken(
            kakaoId,
            accessTokenExpirationMillis,
            TokenType.ACCESS
        )
    }

    fun createRefreshToken(
        kakaoId: String
    ): String {
        return createToken(
            kakaoId,
            refreshTokenExpirationMillis,
            TokenType.REFRESH
        )
    }

    fun parseToken(
        token: String
    ): TokenParseResult {
        return try {
            val claims =
                getClaims(
                    token = token,
                    tokenType = TokenType.REFRESH
                )

            if (claims[TOKEN_TYPE_CLAIM] != TokenType.REFRESH.value) {
                return TokenParseResult.Invalid
            }

            TokenParseResult.Success(
                subject = claims.subject
            )
        } catch (_: ExpiredJwtException) {
            TokenParseResult.Expired
        } catch (_: JwtException) {
            TokenParseResult.Invalid
        } catch (_: IllegalArgumentException) {
            TokenParseResult.Invalid
        }
    }

    fun getKakaoId(
        token: String
    ): String {
        return getClaimsFromAnyToken(token).subject
    }

    fun getExpiration(
        token: String
    ): Date {
        return getClaimsFromAnyToken(token).expiration
    }

    fun validateAccessToken(
        token: String
    ): Boolean {
        return validateToken(
            token = token,
            tokenType = TokenType.ACCESS
        )
    }

    fun validateRefreshToken(
        token: String
    ): Boolean {
        return validateToken(
            token = token,
            tokenType = TokenType.REFRESH
        )
    }

    private fun validateToken(
        token: String,
        tokenType: TokenType
    ): Boolean {
        return try {
            val claims =
                getClaims(
                    token = token,
                    tokenType = tokenType
                )

            claims[TOKEN_TYPE_CLAIM] == tokenType.value
        } catch (_: JwtException) {
            false
        } catch (_: IllegalArgumentException) {
            false
        }
    }

    private fun createToken(
        kakaoId: String,
        expirationMillis: Long,
        tokenType: TokenType
    ): String {
        val now = Date()

        val expiredDate =
            Date(
                now.time + expirationMillis
            )

        return Jwts.builder()
            .subject(kakaoId)
            .claim(TOKEN_TYPE_CLAIM, tokenType.value)
            .issuedAt(now)
            .expiration(expiredDate)
            .signWith(tokenType.signingKey())
            .compact()
    }

    private fun getClaims(
        token: String,
        tokenType: TokenType
    ): Claims {
        return Jwts.parser()
            .verifyWith(tokenType.signingKey())
            .build()
            .parseSignedClaims(token)
            .payload
    }

    private fun getClaimsFromAnyToken(
        token: String
    ): Claims {
        return TokenType.entries.firstNotNullOfOrNull { tokenType ->
            runCatching {
                getClaims(
                    token = token,
                    tokenType = tokenType
                )
            }.getOrNull()
        } ?: throw JwtException("Invalid token")
    }

    private fun TokenType.signingKey() =
        when (this) {
            TokenType.ACCESS -> accessKey
            TokenType.REFRESH -> refreshKey
        }

    private enum class TokenType(
        val value: String
    ) {
        ACCESS("access"),
        REFRESH("refresh")
    }

    private companion object {
        const val TOKEN_TYPE_CLAIM = "tokenType"
    }
}
