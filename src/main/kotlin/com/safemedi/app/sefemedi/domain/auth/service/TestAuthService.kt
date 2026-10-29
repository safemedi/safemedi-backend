package com.safemedi.app.sefemedi.domain.auth.service

import com.safemedi.app.sefemedi.domain.auth.dto.LoginResponse
import com.safemedi.app.sefemedi.domain.auth.entity.RefreshToken
import com.safemedi.app.sefemedi.domain.auth.repository.RefreshTokenRepository
import com.safemedi.app.sefemedi.domain.user.entity.User
import com.safemedi.app.sefemedi.domain.user.repository.UserRepository
import com.safemedi.app.sefemedi.global.error.BusinessException
import com.safemedi.app.sefemedi.global.error.ErrorCode
import com.safemedi.app.sefemedi.global.jwt.JwtProvider
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@ConditionalOnProperty(
    prefix = "app.test-login",
    name = ["enabled"],
    havingValue = "true",
)
class TestAuthService(

    private val jwtProvider: JwtProvider,
    private val userRepository: UserRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
) {

    @Transactional
    fun testLogin(
        socialId: String,
    ): LoginResponse {
        val normalizedSocialId = socialId.trim()
        if (normalizedSocialId.isBlank()) {
            throw BusinessException(ErrorCode.INVALID_REQUEST)
        }

        val existingUser = userRepository.findBySocialIdIncludingDeleted(
            normalizedSocialId,
        )
        val user = when {
            existingUser == null -> userRepository.save(
                User(
                    socialId = normalizedSocialId,
                ),
            )
            existingUser.deletedAt != null -> {
                existingUser.reactivate()
                existingUser
            }
            else -> existingUser
        }

        val accessToken = jwtProvider.createAccessToken(
            normalizedSocialId,
        )
        val refreshToken = jwtProvider.createRefreshToken(
            normalizedSocialId,
        )

        saveRefreshToken(
            user = user,
            token = refreshToken,
        )

        return LoginResponse(
            accessToken = accessToken,
            refreshToken = refreshToken,
            isTutorialCompleted = user.isTutorialCompleted,
        )
    }

    private fun saveRefreshToken(
        user: User,
        token: String,
    ) {
        val userId = user.id ?: throw BusinessException(ErrorCode.INTERNAL_SERVER_ERROR)
        val refreshToken = refreshTokenRepository.findByUser_Id(
            userId,
        )?.apply {
            this.token = token
        } ?: RefreshToken(
            user = user,
            token = token,
        )

        refreshTokenRepository.save(
            refreshToken,
        )
    }
}
