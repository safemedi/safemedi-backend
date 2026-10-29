package com.safemedi.app.sefemedi.domain.auth.client

import com.safemedi.app.sefemedi.global.error.BusinessException
import com.safemedi.app.sefemedi.global.error.ErrorCode
import org.springframework.stereotype.Component
import tools.jackson.databind.json.JsonMapper
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

@Component
class KakaoSocialLoginVerifier(
    private val jsonMapper: JsonMapper
) : SocialLoginVerifier {

    private val httpClient =
        HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build()

    override fun resolveSocialId(
        accessToken: String
    ): String {
        val normalizedAccessToken =
            accessToken.removePrefix("Bearer ")
                .trim()

        if (normalizedAccessToken.isBlank()) {
            throw BusinessException(ErrorCode.INVALID_TOKEN)
        }

        val request =
            HttpRequest.newBuilder()
                .uri(URI.create(KAKAO_ACCESS_TOKEN_INFO_URL))
                .header("Authorization", "Bearer $normalizedAccessToken")
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build()

        val response =
            try {
                httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
                )
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
                throw BusinessException(ErrorCode.INTERNAL_SERVER_ERROR)
            } catch (_: IOException) {
                throw BusinessException(ErrorCode.INTERNAL_SERVER_ERROR)
            }

        if (response.statusCode() != HttpURLConnection.HTTP_OK) {
            throw BusinessException(ErrorCode.INVALID_TOKEN)
        }

        val kakaoId =
            try {
                jsonMapper.readTree(
                    response.body()
                ).get("id")
                    ?.asLong()
            } catch (_: Exception) {
                throw BusinessException(ErrorCode.INTERNAL_SERVER_ERROR)
            } ?: throw BusinessException(ErrorCode.INVALID_TOKEN)

        return kakaoId.toString()
    }

    private companion object {
        const val KAKAO_ACCESS_TOKEN_INFO_URL = "https://kapi.kakao.com/v1/user/access_token_info"
    }
}
