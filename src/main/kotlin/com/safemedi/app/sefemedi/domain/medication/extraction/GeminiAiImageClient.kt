package com.safemedi.app.sefemedi.domain.medication.extraction

import com.safemedi.app.sefemedi.global.error.BusinessException
import com.safemedi.app.sefemedi.global.error.ErrorCode
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import tools.jackson.databind.json.JsonMapper
import java.net.http.HttpClient
import java.time.Duration
import java.util.Base64

/** Gemini 전용 요청과 응답 외피만 처리하며, 처방전 필드 검증은 호출자에게 맡긴다. */
@Component
class GeminiAiImageClient(
    private val mapper: JsonMapper,
    @param:Value("\${app.prescription-extraction.api-key:}") private val apiKey: String,
    @param:Value("\${app.prescription-extraction.model:}") private val model: String,
    @Value("\${app.prescription-extraction.timeout-seconds:45}") timeoutSeconds: Long,
) : AiImageClient {
    private val client = RestClient.builder()
        .baseUrl("https://generativelanguage.googleapis.com/v1beta")
        .requestFactory(JdkClientHttpRequestFactory(
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build(),
        ).apply { setReadTimeout(Duration.ofSeconds(timeoutSeconds)) })
        .build()

    override fun generateJson(image: ByteArray, mimeType: String, prompt: String, schema: String): String {
        if (apiKey.isBlank() || !model.matches(Regex("[a-zA-Z0-9.-]+"))) {
            throw BusinessException(ErrorCode.EXTRACTION_UNAVAILABLE)
        }
        val request = mapOf(
            "systemInstruction" to mapOf("parts" to listOf(mapOf("text" to prompt))),
            "contents" to listOf(mapOf("role" to "user", "parts" to listOf(
                mapOf("inlineData" to mapOf("mimeType" to mimeType, "data" to Base64.getEncoder().encodeToString(image))),
            ))),
            "generationConfig" to mapOf(
                "responseMimeType" to "application/json",
                "responseJsonSchema" to mapper.readTree(schema),
                "maxOutputTokens" to 8192,
            ),
        )
        // 공급자 오류 본문에는 의료 정보가 포함될 수 있으므로 로그·예외 원인으로 전파하지 않는다.
        val response = try {
            client.post().uri("/models/{model}:generateContent", model)
                .header("x-goog-api-key", apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request).retrieve().body(String::class.java)
        } catch (_: Exception) {
            throw BusinessException(ErrorCode.EXTRACTION_UNAVAILABLE)
        }
        return parseResponse(response)
    }

    internal fun parseResponse(response: String?): String {
        try {
            val candidate = mapper.readTree(response ?: "").path("candidates").path(0)
            require(candidate.path("finishReason").asString() == "STOP")
            val text = candidate.path("content").path("parts")
                .filter { !it.path("thought").asBoolean(false) }
                .joinToString("") { it.path("text").asString("") }
            require(text.isNotBlank())
            return text
        } catch (_: Exception) {
            throw BusinessException(ErrorCode.PRESCRIPTION_NOT_READABLE)
        }
    }
}
