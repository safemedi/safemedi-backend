package com.safemedi.app.sefemedi.domain.map.client

import org.junit.jupiter.api.Test
import tools.jackson.databind.json.JsonMapper
import kotlin.test.assertEquals

class KakaoMedicalFacilitySearchClientTest {
    private val client = KakaoMedicalFacilitySearchClient(
        restApiKey = "test-rest-api-key",
        jsonMapper = JsonMapper(),
    )

    @Test
    fun `거리 또는 좌표 값이 없는 document는 결과에서 제외한다`() {
        val body = """
            {
              "documents": [
                {
                  "place_name": "정상약국",
                  "address_name": "서울 강남구 역삼동",
                  "road_address_name": "서울 강남구 테헤란로 1",
                  "x": "127.027102",
                  "y": "37.497821",
                  "distance": "180",
                  "phone": "02-1234-5678",
                  "category_name": "의료,건강 > 약국",
                  "place_url": "http://place.map.kakao.com/1"
                },
                {
                  "place_name": "거리정보없는약국",
                  "address_name": "서울 강남구",
                  "road_address_name": "",
                  "x": "127.0",
                  "y": "37.5",
                  "phone": "",
                  "category_name": "의료,건강 > 약국"
                },
                {
                  "place_name": "좌표정보없는약국",
                  "address_name": "서울 강남구",
                  "road_address_name": "",
                  "distance": "300",
                  "phone": "",
                  "category_name": "의료,건강 > 약국"
                }
              ]
            }
        """.trimIndent()

        val facilities = client.parseDocuments(body, FacilityCategory.PHARMACY)

        assertEquals(1, facilities.size)
        assertEquals("정상약국", facilities[0].name)
        assertEquals(180, facilities[0].distanceMeters)
    }

    @Test
    fun `documents 필드가 없으면 빈 리스트를 반환한다`() {
        val facilities = client.parseDocuments("""{ "meta": {} }""", FacilityCategory.EMERGENCY)

        assertEquals(emptyList(), facilities)
    }
}
