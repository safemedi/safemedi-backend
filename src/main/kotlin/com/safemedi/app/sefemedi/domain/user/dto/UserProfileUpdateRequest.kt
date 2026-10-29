package com.safemedi.app.sefemedi.domain.user.dto

data class UserProfileUpdateRequest(
    val nickname: String? = null,
    val gender: String? = null,
    val bloodType: String? = null,
    val rhType: String? = null,
    val diseaseCodes: List<String>? = null,
    val allergies: List<UserProfileUpdateAllergyRequest>? = null,
)

data class UserProfileUpdateAllergyRequest(
    val type: String,
    val value: String,
    val name: String,
)
