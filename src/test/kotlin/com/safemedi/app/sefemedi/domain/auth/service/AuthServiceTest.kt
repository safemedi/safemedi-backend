package com.safemedi.app.sefemedi.domain.auth.service

import com.safemedi.app.sefemedi.domain.auth.client.SocialLoginVerifier
import com.safemedi.app.sefemedi.domain.auth.entity.RefreshToken
import com.safemedi.app.sefemedi.domain.auth.dto.LogoutRequest
import com.safemedi.app.sefemedi.domain.auth.repository.RefreshTokenRepository
import com.safemedi.app.sefemedi.domain.user.entity.User
import com.safemedi.app.sefemedi.domain.user.entity.UserDevice
import com.safemedi.app.sefemedi.domain.user.repository.UserRepository
import com.safemedi.app.sefemedi.domain.user.repository.UserDeviceRepository
import com.safemedi.app.sefemedi.global.error.BusinessException
import com.safemedi.app.sefemedi.global.error.ErrorCode
import com.safemedi.app.sefemedi.global.jwt.JwtProvider
import com.safemedi.app.sefemedi.global.jwt.TokenParseResult
import io.jsonwebtoken.ExpiredJwtException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockingDetails
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import java.time.Instant
import java.util.Date
import org.mockito.Mockito.times

class AuthServiceTest {

    private lateinit var jwtProvider: JwtProvider
    private lateinit var userRepository: UserRepository
    private lateinit var refreshTokenRepository: RefreshTokenRepository
    private lateinit var accessTokenBlacklistService: AccessTokenBlacklistService
    private lateinit var socialLoginVerifier: SocialLoginVerifier
    private lateinit var userDeviceRepository: UserDeviceRepository
    private lateinit var authService: AuthService

    @BeforeEach
    fun setUp() {
        jwtProvider = mock(JwtProvider::class.java)
        userRepository = mock(UserRepository::class.java)
        refreshTokenRepository = mock(RefreshTokenRepository::class.java)
        accessTokenBlacklistService = mock(AccessTokenBlacklistService::class.java)
        userDeviceRepository = mock(UserDeviceRepository::class.java)
        socialLoginVerifier = object : SocialLoginVerifier {
            override fun resolveSocialId(accessToken: String): String {
                return "4903042739"
            }
        }

        authService = AuthService(
            jwtProvider = jwtProvider,
            userRepository = userRepository,
            refreshTokenRepository = refreshTokenRepository,
            accessTokenBlacklistService = accessTokenBlacklistService,
            socialLoginVerifier = socialLoginVerifier,
            userDeviceRepository = userDeviceRepository,
        )
    }

    private fun givenValidLogoutAccessToken(
        socialId: String = "4903042739",
        accessToken: String = "access-token",
    ): Instant {
        val expiresAt = Date(System.currentTimeMillis() + 60_000)

        given(jwtProvider.validateAccessToken(accessToken)).willReturn(true)
        given(jwtProvider.getKakaoId(accessToken)).willReturn(socialId)
        given(jwtProvider.getExpiration(accessToken)).willReturn(expiresAt)
        given(accessTokenBlacklistService.contains(accessToken)).willReturn(false)

        return expiresAt.toInstant()
    }

    @Test
    fun `login creates a user and returns JWT tokens`() {
        val savedUser = User(
            id = 1L,
            socialId = "4903042739",
            isTutorialCompleted = false,
        )

        given(userRepository.findBySocialIdIncludingDeleted("4903042739")).willReturn(null)
        given(userRepository.save(any(User::class.java))).willReturn(savedUser)
        given(jwtProvider.createAccessToken("4903042739")).willReturn("app-access-token")
        given(jwtProvider.createRefreshToken("4903042739")).willReturn("app-refresh-token")
        given(refreshTokenRepository.findByUser_Id(1L)).willReturn(null)
        given(refreshTokenRepository.save(any(RefreshToken::class.java))).willReturn(
            RefreshToken(
                user = savedUser,
                token = "app-refresh-token",
            )
        )

        val response = authService.login(
            provider = "kakao",
            accessToken = "kakao-access-token",
        )

        assertEquals("app-access-token", response.accessToken)
        assertEquals("app-refresh-token", response.refreshToken)
        assertEquals(false, response.isTutorialCompleted)
        verify(userRepository).save(any(User::class.java))
    }

    @Test
    fun `login rejects unsupported provider`() {
        val exception = assertThrows(BusinessException::class.java) {
            authService.login(
                provider = "naver",
                accessToken = "kakao-access-token",
            )
        }

        assertEquals(ErrorCode.UNSUPPORTED_SOCIAL_LOGIN_PROVIDER, exception.errorCode)
    }

    @Test
    fun `reissue rotates access and refresh tokens`() {
        val user = User(
            id = 1L,
            socialId = "4903042739",
        )
        val refreshToken = "refresh-token"
        val storedRefreshToken = RefreshToken(
            user = user,
            token = refreshToken,
        )

        given(jwtProvider.parseToken(refreshToken)).willReturn(
            TokenParseResult.Success("4903042739")
        )
        given(userRepository.findBySocialId("4903042739")).willReturn(user)
        given(refreshTokenRepository.findByUser_Id(1L)).willReturn(storedRefreshToken)
        given(jwtProvider.createAccessToken("4903042739")).willReturn("new-access-token")
        given(jwtProvider.createRefreshToken("4903042739")).willReturn("new-refresh-token")
        given(refreshTokenRepository.save(any(RefreshToken::class.java))).willReturn(storedRefreshToken)

        val response = authService.reissue(refreshToken)

        assertEquals("new-access-token", response.accessToken)
        assertEquals("new-refresh-token", response.refreshToken)
        assertEquals("new-refresh-token", storedRefreshToken.token)
        verify(jwtProvider, times(1)).parseToken(refreshToken)
    }

    @Test
    fun `reissue throws INVALID_REFRESH_TOKEN when refresh token is malformed`() {
        val refreshToken = "refresh-token"
        given(jwtProvider.parseToken(refreshToken)).willReturn(TokenParseResult.Invalid)

        val exception = assertThrows(BusinessException::class.java) {
            authService.reissue(refreshToken)
        }

        assertEquals(ErrorCode.INVALID_REFRESH_TOKEN, exception.errorCode)
    }

    @Test
    fun `reissue throws EXPIRED_REFRESH_TOKEN when refresh token is expired`() {
        val refreshToken = "refresh-token"
        given(jwtProvider.parseToken(refreshToken)).willReturn(TokenParseResult.Expired)

        val exception = assertThrows(BusinessException::class.java) {
            authService.reissue(refreshToken)
        }

        assertEquals(ErrorCode.EXPIRED_REFRESH_TOKEN, exception.errorCode)
    }

    @Test
    fun `logout deactivates current user's active device token and discards tokens`() {
        val user = User(
            id = 1L,
            socialId = "4903042739",
        )
        val userDevice = UserDevice(
            id = 10L,
            user = user,
            deviceToken = "device-token",
            deviceType = "ANDROID",
            isActive = true,
        )

        given(userRepository.findBySocialId("4903042739")).willReturn(user)
        given(userDeviceRepository.findByDeviceToken("device-token")).willReturn(userDevice)
        val expiresAt = givenValidLogoutAccessToken()

        val response = authService.logout(
            socialId = "4903042739",
            accessToken = "access-token",
            request = LogoutRequest(
                deviceToken = "device-token",
            ),
        )

        assertEquals("로그아웃이 성공적으로 진행되었습니다.", response.message)
        assertEquals(false, userDevice.isActive)
        verify(refreshTokenRepository).deleteByUserId(1L)
        verify(accessTokenBlacklistService).blacklist("access-token", expiresAt)
    }

    @Test
    fun `logout succeeds when the device token is already inactive`() {
        val user = User(
            id = 1L,
            socialId = "4903042739",
        )
        val userDevice = UserDevice(
            id = 10L,
            user = user,
            deviceToken = "device-token",
            deviceType = "ANDROID",
            isActive = false,
        )

        given(userRepository.findBySocialId("4903042739")).willReturn(user)
        given(userDeviceRepository.findByDeviceToken("device-token")).willReturn(userDevice)
        givenValidLogoutAccessToken()

        val response = authService.logout(
            socialId = "4903042739",
            accessToken = "access-token",
            request = LogoutRequest(
                deviceToken = "device-token",
            ),
        )

        assertEquals("로그아웃이 성공적으로 진행되었습니다.", response.message)
        assertEquals(false, userDevice.isActive)
    }

    @Test
    fun `디바이스 토큰이 누락되거나 비어 있어도 다른 기기 변경 없이 로그아웃한다`() {
        given(userRepository.findBySocialId("4903042739"))
            .willReturn(User(id = 1L, socialId = "4903042739"))
        val expiresAt = givenValidLogoutAccessToken()

        listOf(null, "", " ").forEach { token ->
            authService.logout("4903042739", "access-token", LogoutRequest(token))
        }

        org.mockito.Mockito.verifyNoInteractions(userDeviceRepository)
        verify(refreshTokenRepository, org.mockito.Mockito.times(3)).deleteByUserId(1L)
        verify(accessTokenBlacklistService, org.mockito.Mockito.times(3)).blacklist("access-token", expiresAt)
    }

    @Test
    fun `logout throws LOGOUT_DEVICE_TOKEN_TOO_LONG when device token exceeds length limit`() {
        val exception = assertThrows(BusinessException::class.java) {
            authService.logout(
                socialId = "4903042739",
                accessToken = "access-token",
                request = LogoutRequest(
                    deviceToken = "a".repeat(513),
                ),
            )
        }

        assertEquals(ErrorCode.LOGOUT_DEVICE_TOKEN_TOO_LONG, exception.errorCode)
    }

    @Test
    fun `logout succeeds idempotently when device token does not exist`() {
        val user = User(
            id = 1L,
            socialId = "4903042739",
        )

        given(userRepository.findBySocialId("4903042739")).willReturn(user)
        given(userDeviceRepository.findByDeviceToken("device-token")).willReturn(null)
        given(userDeviceRepository.findFirstByUser_IdAndIsActiveTrueOrderByCreatedAtDesc(1L)).willReturn(null)
        val expiresAt = givenValidLogoutAccessToken()

        val response = authService.logout(
            socialId = "4903042739",
            accessToken = "access-token",
            request = LogoutRequest(
                deviceToken = "device-token",
            ),
        )

        assertEquals("로그아웃이 성공적으로 진행되었습니다.", response.message)
        verify(refreshTokenRepository).deleteByUserId(1L)
        verify(accessTokenBlacklistService).blacklist("access-token", expiresAt)
    }

    @Test
    fun `등록되지 않은 토큰으로 로그아웃해도 다른 활성 기기를 비활성화하지 않는다`() {
        val user = User(
            id = 1L,
            socialId = "4903042739",
        )
        val otherActiveDevice = UserDevice(
            id = 20L,
            user = user,
            deviceToken = "rotated-device-token",
            deviceType = "ANDROID",
            isActive = true,
        )

        given(userRepository.findBySocialId("4903042739")).willReturn(user)
        given(userDeviceRepository.findByDeviceToken("stale-device-token")).willReturn(null)
        given(userDeviceRepository.findFirstByUser_IdAndIsActiveTrueOrderByCreatedAtDesc(1L))
            .willReturn(otherActiveDevice)
        givenValidLogoutAccessToken()

        val response = authService.logout(
            socialId = "4903042739",
            accessToken = "access-token",
            request = LogoutRequest(
                deviceToken = "stale-device-token",
            ),
        )

        assertEquals("로그아웃이 성공적으로 진행되었습니다.", response.message)
        assertEquals(true, otherActiveDevice.isActive)
        verify(userDeviceRepository, org.mockito.Mockito.never())
            .findFirstByUser_IdAndIsActiveTrueOrderByCreatedAtDesc(1L)
    }

    @Test
    fun `logout throws INVALID_TOKEN when access token expires right before identity check`() {
        val user = User(
            id = 1L,
            socialId = "4903042739",
        )

        given(userRepository.findBySocialId("4903042739")).willReturn(user)
        given(jwtProvider.validateAccessToken("access-token")).willReturn(true)
        given(jwtProvider.getKakaoId("access-token"))
            .willThrow(ExpiredJwtException(null, null, "expired"))

        val exception = assertThrows(BusinessException::class.java) {
            authService.logout(
                socialId = "4903042739",
                accessToken = "access-token",
                request = LogoutRequest(
                    deviceToken = "device-token",
                ),
            )
        }

        assertEquals(ErrorCode.INVALID_TOKEN, exception.errorCode)
    }

    @Test
    fun `logout succeeds without blacklisting when access token expires right before discard`() {
        val user = User(
            id = 1L,
            socialId = "4903042739",
        )
        val userDevice = UserDevice(
            id = 10L,
            user = user,
            deviceToken = "device-token",
            deviceType = "ANDROID",
            isActive = true,
        )

        given(userRepository.findBySocialId("4903042739")).willReturn(user)
        given(userDeviceRepository.findByDeviceToken("device-token")).willReturn(userDevice)
        given(jwtProvider.validateAccessToken("access-token")).willReturn(true)
        given(jwtProvider.getKakaoId("access-token")).willReturn("4903042739")
        given(accessTokenBlacklistService.contains("access-token")).willReturn(false)
        given(jwtProvider.getExpiration("access-token"))
            .willThrow(ExpiredJwtException(null, null, "expired"))

        val response = authService.logout(
            socialId = "4903042739",
            accessToken = "access-token",
            request = LogoutRequest(
                deviceToken = "device-token",
            ),
        )

        assertEquals("로그아웃이 성공적으로 진행되었습니다.", response.message)
        assertEquals(false, userDevice.isActive)
        verify(refreshTokenRepository).deleteByUserId(1L)
        assertEquals(
            emptyList<String>(),
            mockingDetails(accessTokenBlacklistService).invocations
                .map { it.method.name }
                .filter { it == "blacklist" },
        )
    }

    @Test
    fun `logout throws LOGOUT_DEVICE_TOKEN_ACCESS_DENIED when token belongs to another user`() {
        val currentUser = User(
            id = 1L,
            socialId = "4903042739",
        )
        val otherUser = User(
            id = 2L,
            socialId = "other",
        )
        val userDevice = UserDevice(
            id = 10L,
            user = otherUser,
            deviceToken = "device-token",
            deviceType = "ANDROID",
            isActive = true,
        )

        given(userRepository.findBySocialId("4903042739")).willReturn(currentUser)
        given(userDeviceRepository.findByDeviceToken("device-token")).willReturn(userDevice)
        givenValidLogoutAccessToken()

        val exception = assertThrows(BusinessException::class.java) {
            authService.logout(
                socialId = "4903042739",
                accessToken = "access-token",
                request = LogoutRequest(
                    deviceToken = "device-token",
                ),
            )
        }

        assertEquals(ErrorCode.LOGOUT_DEVICE_TOKEN_ACCESS_DENIED, exception.errorCode)
    }

    @Test
    fun `login reactivates withdrawn user and returns JWT tokens`() {
        val withdrawnUser = User(
            id = 1L,
            socialId = "4903042739",
            deletedAt = java.time.LocalDateTime.now(),
        )

        given(userRepository.findBySocialIdIncludingDeleted("4903042739")).willReturn(withdrawnUser)
        given(jwtProvider.createAccessToken("4903042739")).willReturn("app-access-token")
        given(jwtProvider.createRefreshToken("4903042739")).willReturn("app-refresh-token")
        given(refreshTokenRepository.findByUser_Id(1L)).willReturn(null)
        given(refreshTokenRepository.save(any(RefreshToken::class.java))).willReturn(
            RefreshToken(
                user = withdrawnUser,
                token = "app-refresh-token",
            )
        )

        val response = authService.login(
            provider = "kakao",
            accessToken = "kakao-access-token",
        )

        assertEquals("app-access-token", response.accessToken)
        assertEquals("app-refresh-token", response.refreshToken)
        assertNull(withdrawnUser.deletedAt)
        assertEquals(false, response.isTutorialCompleted)
        verify(userRepository, never()).save(withdrawnUser)
    }
}
