package com.safemedi.app.sefemedi.domain.user.dto

data class TutorialRequest(
    val birthDate: String,
    val gender: String,
    val height: Int? = null,
    val weight: Int? = null,
    val bloodType: String? = null,
    val rhType: String? = null,
    val diseaseCodes: List<String> = emptyList(),
    val allergies: List<TutorialAllergyRequest> = emptyList(),
)

data class TutorialAllergyRequest(
    val type: String,
    val value: String,
    val name: String,
)
