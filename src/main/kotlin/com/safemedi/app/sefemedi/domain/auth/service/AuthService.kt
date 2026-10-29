package com.safemedi.app.sefemedi.domain.auth.service

import com.safemedi.app.sefemedi.domain.auth.client.SocialLoginVerifier
import com.safemedi.app.sefemedi.domain.auth.dto.LoginResponse
import com.safemedi.app.sefemedi.domain.auth.dto.LogoutRequest
import com.safemedi.app.sefemedi.domain.auth.dto.LogoutResponse
import com.safemedi.app.sefemedi.domain.auth.dto.TokenResponse
import com.safemedi.app.sefemedi.domain.auth.entity.RefreshToken
import com.safemedi.app.sefemedi.domain.auth.repository.RefreshTokenRepository
import com.safemedi.app.sefemedi.domain.user.entity.User
import com.safemedi.app.sefemedi.domain.user.repository.UserRepository
import com.safemedi.app.sefemedi.domain.user.repository.UserDeviceRepository
import com.safemedi.app.sefemedi.global.error.BusinessException
import com.safemedi.app.sefemedi.global.error.ErrorCode
import com.safemedi.app.sefemedi.global.jwt.JwtProvider
import com.safemedi.app.sefemedi.global.jwt.TokenParseResult
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.JwtException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.Locale

@Service
class AuthService(

    private val jwtProvider: JwtProvider,
    private val userRepository: UserRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val accessTokenBlacklistService: AccessTokenBlacklistService,
    private val socialLoginVerifier: SocialLoginVerifier,
    private val userDeviceRepository: UserDeviceRepository,
) {

    private val log = LoggerFactory.getLogger(AuthService::class.java)

    @Transactional
    fun login(
        provider: String,
        accessToken: String
    ): LoginResponse {
        requireSupportedProvider(
            provider,
        )

        val kakaoId =
            socialLoginVerifier.resolveSocialId(
                accessToken = accessToken,
            )

        val issuedTokens =
            issueTokensInternal(
                kakaoId = kakaoId,
            )

        return LoginResponse(
            accessToken = issuedTokens.tokenResponse.accessToken,
            refreshToken = issuedTokens.tokenResponse.refreshToken,
            isTutorialCompleted = issuedTokens.user.isTutorialCompleted,
        )
    }

    @Transactional
    fun reissue(
        refreshToken: String
    ): TokenResponse {
        val kakaoId =
            when (val parseResult = jwtProvider.parseToken(refreshToken)) {
                is TokenParseResult.Success -> parseResult.subject
                TokenParseResult.Expired -> throw BusinessException(ErrorCode.EXPIRED_REFRESH_TOKEN)
                TokenParseResult.Invalid -> throw BusinessException(ErrorCode.INVALID_REFRESH_TOKEN)
            }

        val user =
            userRepository.findBySocialId(
                kakaoId,
            ) ?: throw BusinessException(ErrorCode.INVALID_REFRESH_TOKEN)

        val savedRefreshToken =
            refreshTokenRepository.findByUser_Id(
                user.id ?: throw BusinessException(ErrorCode.INVALID_REFRESH_TOKEN),
            )
                ?: throw BusinessException(ErrorCode.INVALID_REFRESH_TOKEN)

        if (savedRefreshToken.token != refreshToken) {
            throw BusinessException(ErrorCode.INVALID_REFRESH_TOKEN)
        }

        val accessToken =
            jwtProvider.createAccessToken(kakaoId)

        val newRefreshToken =
            jwtProvider.createRefreshToken(kakaoId)

        saveRefreshToken(
            user = user,
            token = newRefreshToken
        )

        return TokenResponse(
            accessToken = accessToken,
            refreshToken = newRefreshToken
        )
    }

    @Transactional
    fun logout(
        socialId: String,
        accessToken: String,
        request: LogoutRequest,
    ): LogoutResponse {
        val deviceToken = validateLogoutDeviceToken(request.deviceToken)
        val user = findUserBySocialId(socialId)
        val userId = requireUserId(user)
        validateLogoutAccessToken(
            socialId = socialId,
            accessToken = accessToken,
        )

        val userDevice = deviceToken?.let(userDeviceRepository::findByDeviceToken)
        if (userDevice != null) {
            if (userDevice.user.id != userId) {
                throw BusinessException(ErrorCode.LOGOUT_DEVICE_TOKEN_ACCESS_DENIED)
            }
            userDevice.deactivate()
        }

        refreshTokenRepository.deleteByUserId(userId)
        discardAccessToken(accessToken)

        return LogoutResponse()
    }

    private fun requireSupportedProvider(
        provider: String
    ) {
        if (provider.trim().lowercase(Locale.ROOT) != KAKAO_PROVIDER) {
            throw BusinessException(ErrorCode.UNSUPPORTED_SOCIAL_LOGIN_PROVIDER)
        }
    }

    private fun issueTokensInternal(
        kakaoId: String
    ): IssuedTokens {
        val user =
            findOrCreateUserBySocialId(
                kakaoId,
            )

        val accessToken =
            jwtProvider.createAccessToken(kakaoId)

        val refreshToken =
            jwtProvider.createRefreshToken(kakaoId)

        saveRefreshToken(
            user = user,
            token = refreshToken
        )

        return IssuedTokens(
            user = user,
            tokenResponse = TokenResponse(
                accessToken = accessToken,
                refreshToken = refreshToken
            )
        )
    }

    private fun saveRefreshToken(
        user: User,
        token: String
    ) {
        val userId =
            requireUserId(
                user,
            )

        val refreshToken =
            refreshTokenRepository.findByUser_Id(
                userId,
            )?.apply {
                this.token = token
            } ?: RefreshToken(
                user = user,
                token = token
            )

        refreshTokenRepository.save(
            refreshToken
        )
    }

    private fun findOrCreateUserBySocialId(
        kakaoId: String
    ): User {
        val existingUser = userRepository.findBySocialIdIncludingDeleted(
            kakaoId
        )

        return when {
            existingUser == null -> userRepository.save(
                User(
                    socialId = kakaoId
                )
            )
            existingUser.deletedAt != null -> {
                existingUser.reactivate()
                existingUser
            }
            else -> existingUser
        }
    }

    private fun findUserBySocialId(
        kakaoId: String
    ) = userRepository.findBySocialId(
        kakaoId
    ) ?: throw BusinessException(ErrorCode.INVALID_TOKEN)

    private fun requireUserId(
        user: User
    ): Long {
        return user.id ?: throw BusinessException(ErrorCode.INVALID_TOKEN)
    }

    private fun validateLogoutDeviceToken(
        deviceToken: String?,
    ): String? {
        val trimmedToken = deviceToken?.trim()
        if (trimmedToken.isNullOrBlank()) {
            return null
        }
        if (trimmedToken.length > MAX_DEVICE_TOKEN_LENGTH) {
            throw BusinessException(ErrorCode.LOGOUT_DEVICE_TOKEN_TOO_LONG)
        }

        return trimmedToken
    }

    private fun validateLogoutAccessToken(
        socialId: String,
        accessToken: String,
    ) {
        if (!jwtProvider.validateAccessToken(accessToken)) {
            throw BusinessException(ErrorCode.INVALID_TOKEN)
        }
        val kakaoId = try {
            jwtProvider.getKakaoId(accessToken)
        } catch (_: JwtException) {
            throw BusinessException(ErrorCode.INVALID_TOKEN)
        }
        if (kakaoId != socialId) {
            throw BusinessException(ErrorCode.INVALID_TOKEN)
        }
        if (accessTokenBlacklistService.contains(accessToken)) {
            throw BusinessException(ErrorCode.INVALID_TOKEN)
        }
    }

    private fun discardAccessToken(
        accessToken: String,
    ) {
        val expiresAt = try {
            jwtProvider.getExpiration(accessToken)
        } catch (_: ExpiredJwtException) {
            // 검증과 폐기 사이의 타이밍 차이로 토큰이 막 만료된 경우, 어차피 재사용 불가능하므로 블랙리스트 등록 없이 로그아웃을 정상 종료한다.
            return
        } catch (e: JwtException) {
            // 검증을 통과한 직후라 정상 흐름에서는 발생하지 않아야 하지만, 발생 시 원인 파악을 위해 남긴다.
            log.warn("accessToken 만료 시각 파싱 실패로 블랙리스트 등록을 건너뜀", e)
            return
        }

        accessTokenBlacklistService.blacklist(
            token = accessToken,
            expiresAt = expiresAt.toInstant(),
        )
    }

    private data class IssuedTokens(
        val user: User,
        val tokenResponse: TokenResponse,
    )

    private companion object {
        const val KAKAO_PROVIDER = "kakao"
        const val MAX_DEVICE_TOKEN_LENGTH = 512
    }
}
