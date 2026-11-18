package com.safemedi.app.sefemedi.domain.medication.extraction

import com.safemedi.app.sefemedi.global.error.BusinessException
import com.safemedi.app.sefemedi.global.error.ErrorCode
import org.junit.jupiter.api.Test
import tools.jackson.databind.json.JsonMapper
import kotlin.test.*

class AiPrescriptionExtractorTest {
    private val mapper = JsonMapper.builder().build()
    private val image = byteArrayOf(1)
    private fun extractor(json: String) = AiPrescriptionExtractor(mapper, AiImageClient { _, _, _, _ -> json })

    @Test
    fun `공급자와 무관하게 4회 복용을 보존하고 AI가 제공한 약품 코드는 무시한다`() {
        val result = extractor("""
            {"startDate":null,"endDate":null,"medications":[{
              "drugName":"테스트정 10mg","dailyDoseCount":4,"instructions":"하루 4회",
              "drugCode":"invented","atcCode":"invented"}]}
        """).extract(image, "image/jpeg")
        assertNull(result.startDate)
        assertEquals(4, result.medications.single().dailyDoseCount)
        assertNull(result.medications.single().drugCode)
        assertNull(result.medications.single().atcCode)
    }

    @Test
    fun `잘못된 숫자 및 누락된 필드는 공통 검증에서 거부한다`() {
        val valid = """{"startDate":null,"endDate":null,"medications":[{"drugName":"약","dailyDoseCount":2,"instructions":null}]}"""
        listOf(valid.replace(":2", ":2.5"),
            valid.replace("\"instructions\":null", "\"other\":null"), "not-json").forEach {
            assertEquals(ErrorCode.PRESCRIPTION_NOT_READABLE, assertFailsWith<BusinessException> {
                extractor(it).extract(image, "image/jpeg")
            }.errorCode)
        }
    }

    @Test
    fun `공통 프롬프트와 스키마를 공급자에 전달하고 공급자 오류는 보존한다`() {
        var called = false
        val client = AiImageClient { bytes, mimeType, prompt, schema ->
            called = true
            assertContentEquals(image, bytes)
            assertEquals("image/jpeg", mimeType)
            assertTrue(prompt.contains("Never guess"))
            assertTrue(mapper.readTree(schema).path("properties").has("medications"))
            throw BusinessException(ErrorCode.EXTRACTION_UNAVAILABLE)
        }
        assertEquals(ErrorCode.EXTRACTION_UNAVAILABLE, assertFailsWith<BusinessException> {
            AiPrescriptionExtractor(mapper, client).extract(image, "image/jpeg")
        }.errorCode)
        assertTrue(called)
    }
}

class GeminiAiImageClientTest {
    private val mapper = JsonMapper.builder().build()
    private val client = GeminiAiImageClient(mapper, "", "", 45)
    private fun response(parts: List<Map<String, Any>>, finish: String = "STOP"): String =
        mapper.writeValueAsString(mapOf("candidates" to listOf(mapOf(
            "finishReason" to finish, "content" to mapOf("parts" to parts),
        ))))

    @Test
    fun `Gemini 응답 외피와 사고 내용을 제거하고 JSON 텍스트를 반환한다`() {
        val json = """{"medications":[]}"""
        assertEquals(json, client.parseResponse(response(listOf(
            mapOf("thought" to true, "text" to "internal thought"), mapOf("text" to json),
        ))))
    }

    @Test
    fun `잘린 응답과 차단된 응답 및 빈 응답은 거부한다`() {
        listOf(response(listOf(mapOf("text" to "{}")), "MAX_TOKENS"),
            response(emptyList()), "{\"candidates\":[]}", "not-json", null).forEach {
            assertEquals(ErrorCode.PRESCRIPTION_NOT_READABLE, assertFailsWith<BusinessException> {
                client.parseResponse(it)
            }.errorCode)
        }
    }

    @Test
    fun `키가 없으면 외부 호출 없이 사용 불가 오류를 반환한다`() {
        assertEquals(ErrorCode.EXTRACTION_UNAVAILABLE, assertFailsWith<BusinessException> {
            client.generateJson(byteArrayOf(1), "image/jpeg", "prompt", "{}")
        }.errorCode)
    }
}
