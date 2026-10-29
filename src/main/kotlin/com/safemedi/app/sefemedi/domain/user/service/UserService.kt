package com.safemedi.app.sefemedi.domain.user.service

import com.safemedi.app.sefemedi.domain.drug.repository.DiseaseMasterRepository
import com.safemedi.app.sefemedi.domain.family.repository.FamilyRepository
import com.safemedi.app.sefemedi.domain.user.dto.AllergyResponse
import com.safemedi.app.sefemedi.domain.user.dto.DeviceTokenDeactivateRequest
import com.safemedi.app.sefemedi.domain.user.dto.DeviceTokenDeactivateResponse
import com.safemedi.app.sefemedi.domain.user.dto.DeviceTokenRequest
import com.safemedi.app.sefemedi.domain.user.dto.DeviceTokenResponse
import com.safemedi.app.sefemedi.domain.user.dto.DiseaseResponse
import com.safemedi.app.sefemedi.domain.user.dto.FamilyResponse
import com.safemedi.app.sefemedi.domain.user.dto.TutorialRequest
import com.safemedi.app.sefemedi.domain.user.dto.TutorialResponse
import com.safemedi.app.sefemedi.domain.user.dto.UserNotificationSettingsResponse
import com.safemedi.app.sefemedi.domain.user.dto.UserNotificationSettingsUpdateRequest
import com.safemedi.app.sefemedi.domain.user.dto.UserProfileUpdateAllergyRequest
import com.safemedi.app.sefemedi.domain.user.dto.UserProfileUpdateRequest
import com.safemedi.app.sefemedi.domain.user.dto.UserProfileResponse
import com.safemedi.app.sefemedi.domain.user.entity.AllergyType
import com.safemedi.app.sefemedi.domain.user.entity.BloodType
import com.safemedi.app.sefemedi.domain.user.entity.DeviceType
import com.safemedi.app.sefemedi.domain.user.entity.Gender
import com.safemedi.app.sefemedi.domain.user.entity.RhType
import com.safemedi.app.sefemedi.domain.user.entity.UserDevice
import com.safemedi.app.sefemedi.domain.user.entity.UserAllergy
import com.safemedi.app.sefemedi.domain.user.entity.UserDiseaseMap
import com.safemedi.app.sefemedi.domain.user.entity.UserHealthProfile
import com.safemedi.app.sefemedi.domain.user.repository.UserAllergyRepository
import com.safemedi.app.sefemedi.domain.user.repository.UserDeviceRepository
import com.safemedi.app.sefemedi.domain.user.repository.UserDiseaseMapRepository
import com.safemedi.app.sefemedi.domain.user.repository.UserHealthProfileRepository
import com.safemedi.app.sefemedi.domain.user.repository.UserRepository
import com.safemedi.app.sefemedi.global.error.BusinessException
import com.safemedi.app.sefemedi.global.error.ErrorCode
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.format.DateTimeParseException

@Service
class UserService(
    private val userRepository: UserRepository,
    private val userHealthProfileRepository: UserHealthProfileRepository,
    private val userDiseaseMapRepository: UserDiseaseMapRepository,
    private val userAllergyRepository: UserAllergyRepository,
    private val diseaseMasterRepository: DiseaseMasterRepository,
    private val familyRepository: FamilyRepository,
    private val userDeviceRepository: UserDeviceRepository,
) {

    @Transactional(readOnly = true)
    fun getMyProfile(
        socialId: String,
    ): UserProfileResponse {
        val user = findUserBySocialId(socialId)
        return buildMyProfileResponse(user)
    }

    @Transactional
    fun updateMyProfile(
        socialId: String,
        request: UserProfileUpdateRequest,
    ): UserProfileResponse {
        val user = findUserBySocialId(socialId)
        val userId = requireUserId(user)

        request.nickname?.let { user.nickname = validateNickname(it) }

        val profile = userHealthProfileRepository.findByIdOrNull(userId)
            ?: UserHealthProfile(
                userId = userId,
                user = user,
            )

        request.gender?.let { profile.gender = parseEnum<Gender>(it) }
        request.bloodType?.let { profile.bloodType = parseEnum<BloodType>(it) }
        request.rhType?.let { profile.rhType = parseEnum<RhType>(it) }

        request.diseaseCodes?.let { replaceDiseases(user, userId, it) }
        request.allergies?.let { replaceAllergies(user, userId, it) }

        userHealthProfileRepository.save(profile)

        return buildMyProfileResponse(user)
    }

    private fun buildMyProfileResponse(
        user: com.safemedi.app.sefemedi.domain.user.entity.User,
    ): UserProfileResponse {
        val userId = requireUserId(user)
        val profile = userHealthProfileRepository.findByIdOrNull(userId)

        val diseases = userDiseaseMapRepository.findAllByUser_IdOrderByCreatedAtAsc(userId)
            .map { diseaseMap ->
                DiseaseResponse(
                    code = diseaseMap.disease.diseaseCode,
                    name = diseaseMap.disease.diseaseName,
                )
            }

        val allergies = userAllergyRepository.findAllByUser_IdOrderByCreatedAtAsc(userId)
            .map { allergy ->
                AllergyResponse(
                    type = allergy.allergyType,
                    value = allergy.allergyValue,
                    name = allergy.allergyName,
                )
            }

        val families = listOf(
            FamilyResponse(
                familyId = userId,
                name = user.nickname,
                relation = "본인",
                isMe = true,
            ),
        ) + familyRepository.findAllByUser_IdOrderByCreatedAtAsc(userId)
            .map { family ->
                FamilyResponse(
                    familyId = family.id ?: 0L,
                    name = family.connectedUser.nickname,
                    relation = family.relation,
                    isMe = false,
                )
            }

        val latestDevice = userDeviceRepository.findFirstByUser_IdOrderByCreatedAtDesc(userId)
        val settings = convertToNotificationSettings(latestDevice)

        return UserProfileResponse(
            nickname = user.nickname,
            inviteCode = user.inviteCode,
            birthDate = profile?.birthDate?.toString(),
            gender = profile?.gender,
            height = profile?.height,
            weight = profile?.weight,
            bloodType = profile?.bloodType,
            rhType = profile?.rhType,
            isTutorialCompleted = user.isTutorialCompleted,
            diseases = diseases,
            allergies = allergies,
            families = families,
            settings = settings,
        )
    }

    private fun replaceDiseases(
        user: com.safemedi.app.sefemedi.domain.user.entity.User,
        userId: Long,
        diseaseCodes: List<String>,
    ) {
        val requestedDiseaseCodes = normalizeDiseaseCodes(diseaseCodes)

        val diseasesByCode = diseaseMasterRepository.findAllById(requestedDiseaseCodes)
            .associateBy { it.diseaseCode }

        if (diseasesByCode.size != requestedDiseaseCodes.size) {
            throw BusinessException(ErrorCode.INVALID_DISEASE_CODE)
        }

        val existingDiseaseMaps = userDiseaseMapRepository.findAllByUser_IdOrderByCreatedAtAsc(userId)
        val existingDiseaseMapsByCode = existingDiseaseMaps.groupBy { it.disease.diseaseCode }
        val requestedDiseaseCodeSet = requestedDiseaseCodes.toSet()

        val diseaseMapsToRemove = existingDiseaseMapsByCode
            .filter { (diseaseCode, diseaseMaps) ->
                diseaseCode !in requestedDiseaseCodeSet || diseaseMaps.size > 1
            }
            .flatMap { (diseaseCode, diseaseMaps) ->
                if (diseaseCode in requestedDiseaseCodeSet) {
                    diseaseMaps.drop(1)
                } else {
                    diseaseMaps
                }
            }

        if (diseaseMapsToRemove.isNotEmpty()) {
            userDiseaseMapRepository.deleteAllInBatch(diseaseMapsToRemove)
        }

        val diseaseCodesToAdd = requestedDiseaseCodes.filter { !existingDiseaseMapsByCode.containsKey(it) }
        if (diseaseCodesToAdd.isNotEmpty()) {
            userDiseaseMapRepository.saveAll(
                diseaseCodesToAdd.map { diseaseCode ->
                    UserDiseaseMap(
                        user = user,
                        disease = diseasesByCode.getValue(diseaseCode),
                    )
                },
            )
        }
    }

    private fun replaceAllergies(
        user: com.safemedi.app.sefemedi.domain.user.entity.User,
        userId: Long,
        allergyRequests: List<UserProfileUpdateAllergyRequest>,
    ) {
        val requestedAllergies = allergyRequests
            .map { parseProfileAllergy(it) }
            .distinctBy { it.key() }
        val existingAllergies = userAllergyRepository.findAllByUser_IdOrderByCreatedAtAsc(userId)
        val existingAllergiesByKey = existingAllergies.groupBy { it.key() }
        val requestedAllergyKeys = requestedAllergies.map { it.key() }.toSet()

        val allergiesToRemove = existingAllergiesByKey.flatMap { (key, allergies) ->
            if (key in requestedAllergyKeys) {
                allergies.drop(1)
            } else {
                allergies
            }
        }

        if (allergiesToRemove.isNotEmpty()) {
            userAllergyRepository.deleteAllInBatch(allergiesToRemove)
        }

        val allergiesToAdd = requestedAllergies.filter { !existingAllergiesByKey.containsKey(it.key()) }
        if (allergiesToAdd.isNotEmpty()) {
            userAllergyRepository.saveAll(
                allergiesToAdd.map { allergy ->
                    UserAllergy(
                        user = user,
                        allergyType = allergy.type,
                        allergyValue = allergy.value,
                        allergyName = allergy.name,
                    )
                },
            )
        }
    }

    private fun normalizeDiseaseCodes(diseaseCodes: List<String>): List<String> {
        val normalizedCodes = diseaseCodes.map { it.trim().uppercase() }
        if (normalizedCodes.any { it.isBlank() }) {
            throw BusinessException(ErrorCode.INVALID_DISEASE_CODE)
        }

        return normalizedCodes.distinct()
    }

    private fun validateNickname(nickname: String): String {
        val trimmedNickname = nickname.trim()
        if (trimmedNickname.length !in 5..20) {
            throw BusinessException(ErrorCode.INVALID_NICKNAME_LENGTH)
        }

        return trimmedNickname
    }

    private fun parseProfileAllergy(
        request: UserProfileUpdateAllergyRequest,
    ): ParsedProfileAllergy {
        val type = parseProfileAllergyType(request.type)
        val value = request.value.trim()
        val name = request.name.trim()

        if (value.isBlank() || name.isBlank()) {
            throw BusinessException(ErrorCode.INVALID_ALLERGY_FORMAT)
        }

        validateFoodAllergyLength(type, value, name)

        return ParsedProfileAllergy(
            type = type,
            value = value,
            name = name,
        )
    }

    private fun parseProfileAllergyType(value: String): AllergyType {
        val parsedType = try {
            enumValueOf<AllergyType>(value.trim().uppercase())
        } catch (_: IllegalArgumentException) {
            throw BusinessException(ErrorCode.INVALID_ALLERGY_FORMAT)
        }

        return when (parsedType) {
            AllergyType.ATC_GROUP,
            AllergyType.INGREDIENT,
            AllergyType.CUSTOM,
            AllergyType.FOOD -> parsedType
        }
    }

    private fun validateFoodAllergyLength(
        type: AllergyType,
        value: String,
        name: String,
    ) {
        if (type != AllergyType.FOOD) {
            return
        }

        if (value.length >= FOOD_ALLERGY_MAX_LENGTH || name.length >= FOOD_ALLERGY_MAX_LENGTH) {
            throw BusinessException(ErrorCode.INVALID_ALLERGY_FORMAT)
        }
    }

    private fun findUserBySocialId(socialId: String): com.safemedi.app.sefemedi.domain.user.entity.User {
        return userRepository.findBySocialId(socialId)
            ?: throw BusinessException(ErrorCode.USER_NOT_FOUND)
    }

    private fun requireUserId(user: com.safemedi.app.sefemedi.domain.user.entity.User): Long {
        return user.id ?: throw BusinessException(ErrorCode.USER_NOT_FOUND)
    }

    private data class ParsedProfileAllergy(
        val type: AllergyType,
        val value: String,
        val name: String,
    )

    private data class ParsedProfileAllergyKey(
        val type: AllergyType,
        val value: String,
        val name: String,
    )

    private fun ParsedProfileAllergy.key(): ParsedProfileAllergyKey {
        return ParsedProfileAllergyKey(
            type = type,
            value = value,
            name = name,
        )
    }

    private fun UserAllergy.key(): ParsedProfileAllergyKey {
        return ParsedProfileAllergyKey(
            type = allergyType,
            value = allergyValue,
            name = allergyName,
        )
    }

    @Transactional(readOnly = true)
    fun getNotificationSettings(
        socialId: String,
    ): UserNotificationSettingsResponse {
        val user = userRepository.findBySocialId(socialId)
            ?: throw BusinessException(ErrorCode.USER_NOT_FOUND)
        val userId = user.id
            ?: throw BusinessException(ErrorCode.USER_NOT_FOUND)
        val latestDevice = userDeviceRepository.findFirstByUser_IdOrderByCreatedAtDesc(userId)

        return convertToNotificationSettings(latestDevice)
    }

    @Transactional
    fun updateNotificationSettings(
        socialId: String,
        request: UserNotificationSettingsUpdateRequest,
    ): UserNotificationSettingsResponse {
        val user = userRepository.findBySocialId(socialId)
            ?: throw BusinessException(ErrorCode.USER_NOT_FOUND)
        val userId = user.id
            ?: throw BusinessException(ErrorCode.USER_NOT_FOUND)
        val latestDevice = userDeviceRepository.findFirstByUser_IdAndIsActiveTrueOrderByCreatedAtDesc(userId)
            ?: throw BusinessException(ErrorCode.DEVICE_TOKEN_NOT_FOUND)

        latestDevice.updateNotificationSettings(
            isMyReminderOn = request.isMyReminderOn,
            isFamilyReminderOn = request.isFamilyReminderOn,
            isMissedAlertOn = request.isMissedAlertOn,
        )

        return convertToNotificationSettings(latestDevice)
    }

    @Transactional
    fun completeTutorial(
        socialId: String,
        request: TutorialRequest,
    ): TutorialResponse {
        val user = userRepository.findBySocialId(socialId)
            ?: throw BusinessException(ErrorCode.INVALID_TOKEN)

        if (user.isTutorialCompleted) {
            throw BusinessException(ErrorCode.TUTORIAL_ALREADY_COMPLETED)
        }

        val userId = user.id
            ?: throw BusinessException(ErrorCode.INVALID_TOKEN)

        val profile = userHealthProfileRepository.findByIdOrNull(userId)
            ?: UserHealthProfile(
                user = user,
            )

        profile.birthDate = parseBirthDate(request.birthDate)
        profile.gender = parseEnum<Gender>(request.gender)
        profile.height = request.height
        profile.weight = request.weight
        profile.bloodType = request.bloodType?.let { parseEnum<BloodType>(it) }
        profile.rhType = request.rhType?.let { parseEnum<RhType>(it) } ?: RhType.PLUS

        userHealthProfileRepository.save(profile)

        val requestedDiseaseCodes = request.diseaseCodes.distinct()
        val diseasesByCode = diseaseMasterRepository.findAllById(requestedDiseaseCodes)
            .associateBy { it.diseaseCode }

        if (diseasesByCode.size != requestedDiseaseCodes.size) {
            throw BusinessException(ErrorCode.INVALID_DISEASE_CODE)
        }

        userDiseaseMapRepository.saveAll(
            requestedDiseaseCodes.map { diseaseCode ->
                UserDiseaseMap(
                    user = user,
                    disease = diseasesByCode.getValue(diseaseCode),
                )
            }
        )

        userAllergyRepository.saveAll(
            request.allergies.map { allergy ->
                val allergyType = parseEnum<AllergyType>(allergy.type)
                validateFoodAllergyLength(allergyType, allergy.value, allergy.name)

                UserAllergy(
                    user = user,
                    allergyType = allergyType,
                    allergyValue = allergy.value,
                    allergyName = allergy.name,
                )
            }
        )

        user.isTutorialCompleted = true

        return TutorialResponse(
            isTutorialCompleted = true,
        )
    }

    @Transactional
    fun registerDeviceToken(
        socialId: String,
        request: DeviceTokenRequest,
    ): DeviceTokenResponse {
        val deviceToken = validateDeviceToken(request.deviceToken)
        val deviceType = validateDeviceType(request.deviceType)
        val user = userRepository.findBySocialId(socialId)
            ?: throw BusinessException(ErrorCode.INVALID_TOKEN)
        val userDevice = userDeviceRepository.findByDeviceToken(deviceToken)
            ?: UserDevice(
                user = user,
                deviceToken = deviceToken,
                deviceType = deviceType.name,
            )

        userDevice.register(
            user = user,
            deviceType = deviceType.name,
        )

        val savedDevice = userDeviceRepository.save(userDevice)

        return DeviceTokenResponse(
            deviceId = savedDevice.id ?: throw BusinessException(ErrorCode.INTERNAL_SERVER_ERROR),
        )
    }

    @Transactional
    fun deactivateDeviceToken(
        socialId: String,
        request: DeviceTokenDeactivateRequest,
    ): DeviceTokenDeactivateResponse {
        val deviceToken = validateDeviceToken(request.deviceToken)
        val user = userRepository.findBySocialId(socialId)
            ?: throw BusinessException(ErrorCode.INVALID_TOKEN)
        val userId = user.id
            ?: throw BusinessException(ErrorCode.INVALID_TOKEN)
        val userDevice = userDeviceRepository.findByDeviceToken(deviceToken)
            ?: throw BusinessException(ErrorCode.DEVICE_TOKEN_NOT_FOUND)

        if (userDevice.user.id != userId) {
            throw BusinessException(ErrorCode.DEVICE_TOKEN_ACCESS_DENIED)
        }

        userDevice.deactivate()

        return DeviceTokenDeactivateResponse()
    }

    private fun parseBirthDate(birthDate: String): LocalDate {
        return try {
            LocalDate.parse(birthDate)
        } catch (_: DateTimeParseException) {
            throw BusinessException(ErrorCode.INVALID_DATE_FORMAT)
        }
    }

    private inline fun <reified T : Enum<T>> parseEnum(value: String): T {
        return try {
            enumValueOf<T>(value.trim().uppercase())
        } catch (_: IllegalArgumentException) {
            throw BusinessException(ErrorCode.INVALID_ENUM_VALUE)
        }
    }

    private fun convertToNotificationSettings(
        latestDevice: UserDevice?,
    ): UserNotificationSettingsResponse {
        return UserNotificationSettingsResponse(
            isMyReminderOn = latestDevice?.isMyReminderOn ?: true,
            isFamilyReminderOn = latestDevice?.isFamilyReminderOn ?: true,
            isMissedAlertOn = latestDevice?.isMissedAlertOn ?: true,
        )
    }

    private fun validateDeviceToken(deviceToken: String?): String {
        val trimmedToken = deviceToken?.trim()
        if (trimmedToken.isNullOrBlank()) {
            throw BusinessException(ErrorCode.INVALID_REQUEST)
        }
        if (trimmedToken.length > MAX_DEVICE_TOKEN_LENGTH) {
            throw BusinessException(ErrorCode.DEVICE_TOKEN_TOO_LONG)
        }

        return trimmedToken
    }

    private fun validateDeviceType(deviceType: String?): DeviceType {
        val trimmedType = deviceType?.trim()
        if (trimmedType.isNullOrBlank()) {
            throw BusinessException(ErrorCode.INVALID_REQUEST)
        }

        return try {
            enumValueOf<DeviceType>(trimmedType.uppercase())
        } catch (_: IllegalArgumentException) {
            throw BusinessException(ErrorCode.UNSUPPORTED_DEVICE_TYPE)
        }
    }

    private companion object {
        const val MAX_DEVICE_TOKEN_LENGTH = 512
        const val FOOD_ALLERGY_MAX_LENGTH = 20
    }
}
