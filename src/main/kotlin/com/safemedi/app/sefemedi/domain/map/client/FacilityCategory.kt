package com.safemedi.app.sefemedi.domain.map.client

enum class FacilityCategory(
    val code: String,
    val kakaoGroupCode: String,
    val defaultKeyword: String,
) {
    PHARMACY("pharmacy", "PM9", "약국"),
    EMERGENCY("emergency", "HP8", "응급실"),
    ;

    companion object {
        fun fromCode(code: String): FacilityCategory? {
            return entries.find { it.code == code }
        }
    }
}
