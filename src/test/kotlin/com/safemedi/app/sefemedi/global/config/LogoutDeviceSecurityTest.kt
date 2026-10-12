package com.safemedi.app.sefemedi.global.config

import com.safemedi.app.sefemedi.domain.auth.dto.LogoutRequest
import com.safemedi.app.sefemedi.domain.auth.dto.LogoutResponse
import com.safemedi.app.sefemedi.domain.auth.service.AccessTokenBlacklistService
import com.safemedi.app.sefemedi.domain.auth.service.AuthService
import com.safemedi.app.sefemedi.domain.user.dto.DeviceTokenDeactivateRequest
import com.safemedi.app.sefemedi.domain.user.dto.DeviceTokenDeactivateResponse
import com.safemedi.app.sefemedi.domain.user.service.UserService
import com.safemedi.app.sefemedi.global.error.BusinessException
import com.safemedi.app.sefemedi.global.error.ErrorCode
import com.safemedi.app.sefemedi.global.jwt.JwtProvider
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.doThrow
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.util.Date

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LogoutDeviceSecurityTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var jwtProvider: JwtProvider

    @Value("\${jwt.secret}")
    private lateinit var secret: String

    @MockitoBean
    private lateinit var authService: AuthService

    @MockitoBean
    private lateinit var userService: UserService

    @MockitoBean
    private lateinit var accessTokenBlacklistService: AccessTokenBlacklistService

    @Test
    fun `로그아웃 두 경로와 두 메서드는 인증 누락 또는 만료 시 표준 401을 반환한다`() {
        val expired = Jwts.builder().subject("user").claim("tokenType", "access")
            .expiration(Date(System.currentTimeMillis() - 60_000))
            .signWith(Keys.hmacShaKeyFor("$secret:access".toByteArray())).compact()

        for (path in listOf("/auth/logout", "/api/v1/auth/logout")) {
            for (method in listOf(HttpMethod.POST, HttpMethod.DELETE)) {
                for (token in listOf(null, expired)) {
                    val request = request(method, path).contentType(MediaType.APPLICATION_JSON)
                        .content("""{"deviceToken":"device-token"}""")
                    if (token != null) request.header("Authorization", "Bearer $token")
                    mockMvc.perform(request)
                        .andExpect(status().isUnauthorized)
                        .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_ACCESS_TOKEN.code))
                        .andExpect(jsonPath("$.message").value(ErrorCode.INVALID_ACCESS_TOKEN.message))
                }
            }
        }
        verifyNoInteractions(authService)
    }

    @Test
    fun `로그아웃은 토큰 필드가 없어도 인증된 요청을 처리하고 실제 서버 오류는 500을 유지한다`() {
        val token = jwtProvider.createAccessToken("user")
        given(authService.logout("user", token, LogoutRequest())).willReturn(LogoutResponse())
        mockMvc.perform(post("/api/v1/auth/logout").header("Authorization", "Bearer $token")
            .contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isOk)

        given(authService.logout("user", token, LogoutRequest())).willThrow(IllegalStateException("test failure"))
        mockMvc.perform(post("/api/v1/auth/logout").header("Authorization", "Bearer $token")
            .contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.code").value("SYS_500"))
    }

    @Test
    fun `로그인과 재발급은 인증 없이 기존 검증에 도달한다`() {
        given(authService.login("unsupported", "social-token"))
            .willThrow(BusinessException(ErrorCode.UNSUPPORTED_SOCIAL_LOGIN_PROVIDER))
        for (prefix in listOf("/auth", "/api/v1/auth")) {
            mockMvc.perform(post("$prefix/login/unsupported").contentType(MediaType.APPLICATION_JSON)
                .content("""{"accessToken":"social-token"}"""))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value(ErrorCode.UNSUPPORTED_SOCIAL_LOGIN_PROVIDER.code))
            mockMvc.perform(post("$prefix/reissue").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value(ErrorCode.REFRESH_TOKEN_REQUIRED.code))
        }
    }

    @Test
    fun `디바이스 삭제는 인증을 요구하고 기존 성공 및 소유권 오류 응답을 유지한다`() {
        mockMvc.perform(delete("/api/v1/users/device-token").param("deviceToken", "device-token"))
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_ACCESS_TOKEN.code))
        verifyNoInteractions(userService)

        val token = jwtProvider.createAccessToken("user")
        val request = DeviceTokenDeactivateRequest("device-token")
        given(userService.deactivateDeviceToken("user", request)).willReturn(DeviceTokenDeactivateResponse())
        mockMvc.perform(delete("/api/v1/users/device-token").param("deviceToken", "device-token")
            .header("Authorization", "Bearer $token"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.message").value(DeviceTokenDeactivateResponse().message))

        for (error in listOf(ErrorCode.DEVICE_TOKEN_NOT_FOUND, ErrorCode.DEVICE_TOKEN_ACCESS_DENIED)) {
            doThrow(BusinessException(error)).`when`(userService).deactivateDeviceToken("user", request)
            mockMvc.perform(delete("/api/v1/users/device-token").param("deviceToken", "device-token")
                .header("Authorization", "Bearer $token"))
                .andExpect(status().`is`(error.status.value()))
                .andExpect(jsonPath("$.code").value(error.code))
        }
    }
}
