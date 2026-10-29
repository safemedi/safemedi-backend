package com.safemedi.app.sefemedi.domain.user.dto

import com.safemedi.app.sefemedi.domain.user.entity.AllergyType

data class AllergyResponse(
    val type: AllergyType,
    val value: String,
    val name: String,
)
