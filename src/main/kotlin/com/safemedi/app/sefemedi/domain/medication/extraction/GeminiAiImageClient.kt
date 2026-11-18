package com.safemedi.app.sefemedi.domain.medication.extraction

import com.safemedi.app.sefemedi.global.error.BusinessException
import com.safemedi.app.sefemedi.global.error.ErrorCode
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException
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
    @param:Value("\${app.prescription-extraction.timeout-seconds:30}") private val timeoutSeconds: Long,
) : AiImageClient {
    private val log = LoggerFactory.getLogger(GeminiAiImageClient::class.java)

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
            "generationConfig" to buildMap<String, Any> {
                put("responseMimeType", "application/json")
                put("responseJsonSchema", mapper.readTree(schema))
                put("maxOutputTokens", 8192)
                if (model == "gemini-3.8-flash") {
                    put("thinkingConfig", mapOf("thinkingLevel" to "LOW"))
                }
            },
        )
        // 공급자 오류 본문에는 의료 정보가 포함될 수 있으므로 로그·예외 원인으로 전파하지 않는다.
        val startedAt = System.nanoTime()
        val response = try {
            client.post().uri("/models/{model}:generateContent", model)
                .header("x-goog-api-key", apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request).retrieve().body(String::class.java)
        } catch (exception: RestClientResponseException) {
            log.warn("Gemini extraction request failed: HTTP {}", exception.statusCode.value())
            throw BusinessException(ErrorCode.EXTRACTION_UNAVAILABLE)
        } catch (exception: Exception) {
            val exceptionTypes = generateSequence<Throwable>(exception) { it.cause }
                .take(8)
                .joinToString(" -> ") { it.javaClass.simpleName }
            val elapsedMs = Duration.ofNanos(System.nanoTime() - startedAt).toMillis()
            log.warn(
                "Gemini extraction request failed: {} (elapsedMs={}, timeoutSeconds={})",
                exceptionTypes, elapsedMs, timeoutSeconds,
            )
            throw BusinessException(ErrorCode.EXTRACTION_UNAVAILABLE)
        }
        val json = parseResponse(response)
        log.info("Gemini extraction response received (elapsedMs={})",
            Duration.ofNanos(System.nanoTime() - startedAt).toMillis())
        return json
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
