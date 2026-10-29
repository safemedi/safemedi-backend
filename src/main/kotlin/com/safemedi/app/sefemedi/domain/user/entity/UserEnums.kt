package com.safemedi.app.sefemedi.domain.user.entity

@Suppress("unused")
enum class SocialProvider {
    GOOGLE
}

enum class Gender {
    MALE, FEMALE
}

@Suppress("unused")
enum class BloodType {
    A, B, O, AB
}

enum class RhType {
    PLUS, MINUS
}

enum class AllergyType {
    ATC_GROUP, INGREDIENT, CUSTOM, FOOD
}
