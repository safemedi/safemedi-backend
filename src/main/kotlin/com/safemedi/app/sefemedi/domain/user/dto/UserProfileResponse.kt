package com.safemedi.app.sefemedi.domain.user.dto

import com.safemedi.app.sefemedi.domain.user.entity.BloodType
import com.safemedi.app.sefemedi.domain.user.entity.Gender
import com.safemedi.app.sefemedi.domain.user.entity.RhType

data class UserProfileResponse(
    val nickname: String?,
    val inviteCode: String?,
    val birthDate: String?,
    val gender: Gender?,
    val height: Int?,
    val weight: Int?,
    val bloodType: BloodType?,
    val rhType: RhType?,
    val isTutorialCompleted: Boolean,
    val diseases: List<DiseaseResponse>,
    val allergies: List<AllergyResponse>,
    val families: List<FamilyResponse>,
    val settings: UserNotificationSettingsResponse,
)

data class DiseaseResponse(
    val code: String,
    val name: String,
)

data class FamilyResponse(
    val familyId: Long,
    val name: String?,
    val relation: String,
    val isMe: Boolean,
)

data class UserNotificationSettingsResponse(
    val isMyReminderOn: Boolean,
    val isFamilyReminderOn: Boolean,
    val isMissedAlertOn: Boolean,
)

data class UserNotificationSettingsUpdateRequest(
    val isMyReminderOn: Boolean? = null,
    val isFamilyReminderOn: Boolean? = null,
    val isMissedAlertOn: Boolean? = null,
)
