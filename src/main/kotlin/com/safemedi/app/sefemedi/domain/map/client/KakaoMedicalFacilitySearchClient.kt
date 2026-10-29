package com.safemedi.app.sefemedi.domain.map.client

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import tools.jackson.databind.json.JsonMapper
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.time.Duration

@Component
class KakaoMedicalFacilitySearchClient(
    @param:Value("\${kakao.local-api.rest-api-key}")
    private val restApiKey: String,
    private val jsonMapper: JsonMapper,
) : MedicalFacilitySearchClient {

    private val httpClient =
        HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(REQUEST_TIMEOUT_SECONDS))
            .build()

    override fun search(
        category: FacilityCategory,
        query: String,
        latitude: Double,
        longitude: Double,
    ): List<RawFacility> {
        val request =
            HttpRequest.newBuilder()
                .uri(URI.create(buildRequestUrl(category, query, latitude, longitude)))
                .header("Authorization", "KakaoAK $restApiKey")
                .timeout(Duration.ofSeconds(REQUEST_TIMEOUT_SECONDS))
                .GET()
                .build()

        val response =
            try {
                httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                throw e
            }

        if (response.statusCode() != HttpURLConnection.HTTP_OK) {
            throw IOException("카카오 로컬 API 응답 오류: ${response.statusCode()}")
        }

        return parseDocuments(response.body(), category)
    }

    private fun buildRequestUrl(
        category: FacilityCategory,
        query: String,
        latitude: Double,
        longitude: Double,
    ): String {
        val encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8)
        return "$KEYWORD_SEARCH_URL" +
            "?query=$encodedQuery" +
            "&category_group_code=${category.kakaoGroupCode}" +
            "&x=$longitude&y=$latitude" +
            "&radius=$SEARCH_RADIUS_METERS" +
            "&sort=distance"
    }

    internal fun parseDocuments(
        body: String,
        category: FacilityCategory,
    ): List<RawFacility> {
        val documents = jsonMapper.readTree(body).get("documents") ?: return emptyList()
        val facilities = mutableListOf<RawFacility>()
        for (document in documents) {
            val latitude = document.get("y")?.asString()?.toDoubleOrNull()
            val longitude = document.get("x")?.asString()?.toDoubleOrNull()
            val distanceMeters = document.get("distance")?.asString()?.toIntOrNull()
            if (latitude == null || longitude == null || distanceMeters == null) {
                log.warn(
                    "카카오 로컬 API 응답에 좌표/거리 값이 없어 항목을 제외합니다: {}",
                    document.get("place_name")?.asString(),
                )
                continue
            }

            facilities.add(
                RawFacility(
                    name = document.get("place_name")?.asString().orEmpty(),
                    category = category,
                    address = document.get("address_name")?.asString().orEmpty(),
                    roadAddress = document.get("road_address_name")?.asString().orEmpty(),
                    latitude = latitude,
                    longitude = longitude,
                    distanceMeters = distanceMeters,
                    phoneNumber = document.get("phone")?.asString().orEmpty(),
                    categoryName = document.get("category_name")?.asString().orEmpty(),
                    placeUrl = document.get("place_url")?.asString(),
                )
            )
        }
        return facilities
    }

    private companion object {
        val log = LoggerFactory.getLogger(KakaoMedicalFacilitySearchClient::class.java)
        const val KEYWORD_SEARCH_URL = "https://dapi.kakao.com/v2/local/search/keyword.json"
        const val SEARCH_RADIUS_METERS = 5000
        const val REQUEST_TIMEOUT_SECONDS = 7L
    }
}
