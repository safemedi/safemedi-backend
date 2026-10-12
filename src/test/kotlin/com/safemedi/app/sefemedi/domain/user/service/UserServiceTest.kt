package com.safemedi.app.sefemedi.domain.user.service

import com.safemedi.app.sefemedi.domain.drug.entity.DiseaseMaster
import com.safemedi.app.sefemedi.domain.drug.repository.DiseaseMasterRepository
import com.safemedi.app.sefemedi.domain.family.entity.Family
import com.safemedi.app.sefemedi.domain.family.repository.FamilyRepository
import com.safemedi.app.sefemedi.domain.user.dto.AllergyResponse
import com.safemedi.app.sefemedi.domain.user.dto.DeviceTokenDeactivateRequest
import com.safemedi.app.sefemedi.domain.user.dto.DeviceTokenRequest
import com.safemedi.app.sefemedi.domain.user.dto.DiseaseResponse
import com.safemedi.app.sefemedi.domain.user.dto.FamilyResponse
import com.safemedi.app.sefemedi.domain.user.dto.TutorialAllergyRequest
import com.safemedi.app.sefemedi.domain.user.dto.TutorialRequest
import com.safemedi.app.sefemedi.domain.user.dto.UserNotificationSettingsResponse
import com.safemedi.app.sefemedi.domain.user.dto.UserNotificationSettingsUpdateRequest
import com.safemedi.app.sefemedi.domain.user.dto.UserProfileResponse
import com.safemedi.app.sefemedi.domain.user.entity.AllergyType
import com.safemedi.app.sefemedi.domain.user.entity.BloodType
import com.safemedi.app.sefemedi.domain.user.entity.Gender
import com.safemedi.app.sefemedi.domain.user.entity.RhType
import com.safemedi.app.sefemedi.domain.user.entity.User
import com.safemedi.app.sefemedi.domain.user.entity.UserAllergy
import com.safemedi.app.sefemedi.domain.user.entity.UserDevice
import com.safemedi.app.sefemedi.domain.user.entity.UserDiseaseMap
import com.safemedi.app.sefemedi.domain.user.entity.UserHealthProfile
import com.safemedi.app.sefemedi.domain.user.repository.UserAllergyRepository
import com.safemedi.app.sefemedi.domain.user.repository.UserDeviceRepository
import com.safemedi.app.sefemedi.domain.user.repository.UserDiseaseMapRepository
import com.safemedi.app.sefemedi.domain.user.repository.UserHealthProfileRepository
import com.safemedi.app.sefemedi.domain.user.repository.UserRepository
import com.safemedi.app.sefemedi.global.error.BusinessException
import com.safemedi.app.sefemedi.global.error.ErrorCode
import org.mockito.ArgumentCaptor
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.BDDMockito.given
import org.mockito.Mockito.anyList
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import java.time.LocalDate
import java.util.Optional

class UserServiceTest {

    private lateinit var userRepository: UserRepository
    private lateinit var userHealthProfileRepository: UserHealthProfileRepository
    private lateinit var userDiseaseMapRepository: UserDiseaseMapRepository
    private lateinit var userAllergyRepository: UserAllergyRepository
    private lateinit var diseaseMasterRepository: DiseaseMasterRepository
    private lateinit var familyRepository: FamilyRepository
    private lateinit var userDeviceRepository: UserDeviceRepository
    private lateinit var userService: UserService

    @BeforeEach
    fun setUp() {
        userRepository = mock(UserRepository::class.java)
        userHealthProfileRepository = mock(UserHealthProfileRepository::class.java)
        userDiseaseMapRepository = mock(UserDiseaseMapRepository::class.java)
        userAllergyRepository = mock(UserAllergyRepository::class.java)
        diseaseMasterRepository = mock(DiseaseMasterRepository::class.java)
        familyRepository = mock(FamilyRepository::class.java)
        userDeviceRepository = mock(UserDeviceRepository::class.java)

        userService = UserService(
            userRepository = userRepository,
            userHealthProfileRepository = userHealthProfileRepository,
            userDiseaseMapRepository = userDiseaseMapRepository,
            userAllergyRepository = userAllergyRepository,
            diseaseMasterRepository = diseaseMasterRepository,
            familyRepository = familyRepository,
            userDeviceRepository = userDeviceRepository,
        )
    }

    @Test
    fun `getMyProfile returns the expected aggregate response`() {
        val user = User(
            id = 1L,
            nickname = "홍길동",
            socialId = "4903042739",
            inviteCode = "A8F9K2",
            isTutorialCompleted = true,
        )
        val profile = UserHealthProfile(
            userId = 1L,
            user = user,
            birthDate = LocalDate.of(1985, 3, 15),
            gender = Gender.MALE,
            height = 180,
            weight = 75,
            bloodType = BloodType.O,
            rhType = RhType.PLUS,
        )
        val mother = User(
            id = 2L,
            nickname = "김영희",
        )
        val father = User(
            id = 3L,
            nickname = "홍철수",
        )

        given(userRepository.findBySocialId("4903042739")).willReturn(user)
        given(userHealthProfileRepository.findById(1L)).willReturn(Optional.of(profile))
        given(userDiseaseMapRepository.findAllByUser_IdOrderByCreatedAtAsc(1L)).willReturn(
            listOf(
                UserDiseaseMap(
                    id = 10L,
                    user = user,
                    disease = DiseaseMaster(
                        diseaseCode = "I10",
                        diseaseName = "고혈압",
                    ),
                ),
                UserDiseaseMap(
                    id = 11L,
                    user = user,
                    disease = DiseaseMaster(
                        diseaseCode = "E11",
                        diseaseName = "당뇨병",
                    ),
                ),
            )
        )
        given(userAllergyRepository.findAllByUser_IdOrderByCreatedAtAsc(1L)).willReturn(
            listOf(
                UserAllergy(
                    id = 20L,
                    user = user,
                    allergyType = AllergyType.INGREDIENT,
                    allergyValue = "M249154",
                    allergyName = "페니실린",
                ),
                UserAllergy(
                    id = 21L,
                    user = user,
                    allergyType = AllergyType.ATC_GROUP,
                    allergyValue = "N02B",
                    allergyName = "아스피린",
                ),
            )
        )
        given(familyRepository.findAllByUser_IdOrderByCreatedAtAsc(1L)).willReturn(
            listOf(
                Family(
                    id = 2L,
                    user = user,
                    connectedUser = mother,
                    relation = "어머니",
                ),
                Family(
                    id = 3L,
                    user = user,
                    connectedUser = father,
                    relation = "아버지",
                ),
            )
        )
        given(userDeviceRepository.findFirstByUser_IdOrderByCreatedAtDesc(1L)).willReturn(
            UserDevice(
                id = 100L,
                user = user,
                deviceToken = "device-token",
                deviceType = "ANDROID",
                isMyReminderOn = true,
                isFamilyReminderOn = false,
                isMissedAlertOn = false,
            )
        )

        val response = userService.getMyProfile("4903042739")

        assertEquals(
            UserProfileResponse(
                nickname = "홍길동",
                inviteCode = "A8F9K2",
                birthDate = "1985-03-15",
                gender = Gender.MALE,
                height = 180,
                weight = 75,
                bloodType = BloodType.O,
                rhType = RhType.PLUS,
                isTutorialCompleted = true,
                diseases = listOf(
                    DiseaseResponse("I10", "고혈압"),
                    DiseaseResponse("E11", "당뇨병"),
                ),
                allergies = listOf(
                    AllergyResponse(
                        type = AllergyType.INGREDIENT,
                        value = "M249154",
                        name = "페니실린",
                    ),
                    AllergyResponse(
                        type = AllergyType.ATC_GROUP,
                        value = "N02B",
                        name = "아스피린",
                    ),
                ),
                families = listOf(
                    FamilyResponse(
                        familyId = 1L,
                        name = "홍길동",
                        relation = "본인",
                        isMe = true,
                    ),
                    FamilyResponse(
                        familyId = 2L,
                        name = "김영희",
                        relation = "어머니",
                        isMe = false,
                    ),
                    FamilyResponse(
                        familyId = 3L,
                        name = "홍철수",
                        relation = "아버지",
                        isMe = false,
                    ),
                ),
                settings = UserNotificationSettingsResponse(
                    isMyReminderOn = true,
                    isFamilyReminderOn = false,
                    isMissedAlertOn = false,
                ),
            ),
            response,
        )
    }

    @Test
    fun `getMyProfile falls back to default notification settings when there is no device record`() {
        val user = User(
            id = 1L,
            nickname = "홍길동",
            socialId = "4903042739",
            inviteCode = "A8F9K2",
            isTutorialCompleted = false,
        )

        given(userRepository.findBySocialId("4903042739")).willReturn(user)
        given(userHealthProfileRepository.findById(1L)).willReturn(Optional.empty())
        given(userDiseaseMapRepository.findAllByUser_IdOrderByCreatedAtAsc(1L)).willReturn(emptyList())
        given(userAllergyRepository.findAllByUser_IdOrderByCreatedAtAsc(1L)).willReturn(emptyList())
        given(familyRepository.findAllByUser_IdOrderByCreatedAtAsc(1L)).willReturn(emptyList())
        given(userDeviceRepository.findFirstByUser_IdOrderByCreatedAtDesc(1L)).willReturn(null)

        val response = userService.getMyProfile("4903042739")

        assertEquals(
            UserProfileResponse(
                nickname = "홍길동",
                inviteCode = "A8F9K2",
                birthDate = null,
                gender = null,
                height = null,
                weight = null,
                bloodType = null,
                rhType = null,
                isTutorialCompleted = false,
                diseases = emptyList(),
                allergies = emptyList(),
                families = listOf(
                    FamilyResponse(
                        familyId = 1L,
                        name = "홍길동",
                        relation = "본인",
                        isMe = true,
                    ),
                ),
                settings = UserNotificationSettingsResponse(
                    isMyReminderOn = true,
                    isFamilyReminderOn = true,
                    isMissedAlertOn = true,
                ),
            ),
            response,
        )
    }

    @Test
    fun `getNotificationSettings returns latest device notification settings`() {
        val user = User(
            id = 1L,
            socialId = "4903042739",
        )
        val userDevice = UserDevice(
            id = 10L,
            user = user,
            deviceToken = "device-token",
            deviceType = "ANDROID",
            isMyReminderOn = false,
            isFamilyReminderOn = true,
            isMissedAlertOn = false,
        )

        given(userRepository.findBySocialId("4903042739")).willReturn(user)
        given(userDeviceRepository.findFirstByUser_IdOrderByCreatedAtDesc(1L)).willReturn(userDevice)

        val response = userService.getNotificationSettings("4903042739")

        assertEquals(
            UserNotificationSettingsResponse(
                isMyReminderOn = false,
                isFamilyReminderOn = true,
                isMissedAlertOn = false,
            ),
            response,
        )
    }

    @Test
    fun `getNotificationSettings returns default settings when there is no device record`() {
        val user = User(
            id = 1L,
            socialId = "4903042739",
        )

        given(userRepository.findBySocialId("4903042739")).willReturn(user)
        given(userDeviceRepository.findFirstByUser_IdOrderByCreatedAtDesc(1L)).willReturn(null)

        val response = userService.getNotificationSettings("4903042739")

        assertEquals(
            UserNotificationSettingsResponse(
                isMyReminderOn = true,
                isFamilyReminderOn = true,
                isMissedAlertOn = true,
            ),
            response,
        )
    }

    @Test
    fun `알림 설정 수정은 활성 기기의 알림 설정을 변경한다`() {
        val user = User(
            id = 1L,
            socialId = "4903042739",
        )
        val userDevice = UserDevice(
            id = 10L,
            user = user,
            deviceToken = "device-token",
            deviceType = "ANDROID",
            isMyReminderOn = false,
            isFamilyReminderOn = true,
            isMissedAlertOn = false,
        )

        given(userRepository.findBySocialId("4903042739")).willReturn(user)
        given(userDeviceRepository.findFirstByUser_IdAndIsActiveTrueOrderByCreatedAtDesc(1L)).willReturn(userDevice)

        val response = userService.updateNotificationSettings(
            socialId = "4903042739",
            request = UserNotificationSettingsUpdateRequest(
                isMyReminderOn = true,
                isFamilyReminderOn = false,
                isMissedAlertOn = true,
            ),
        )

        assertEquals(true, userDevice.isMyReminderOn)
        assertEquals(false, userDevice.isFamilyReminderOn)
        assertEquals(true, userDevice.isMissedAlertOn)
        assertEquals(
            UserNotificationSettingsResponse(
                isMyReminderOn = true,
                isFamilyReminderOn = false,
                isMissedAlertOn = true,
            ),
            response,
        )
    }

    @Test
    fun `알림 설정 수정은 요청에서 생략된 필드를 기존 값으로 유지한다`() {
        val user = User(
            id = 1L,
            socialId = "4903042739",
        )
        val userDevice = UserDevice(
            id = 10L,
            user = user,
            deviceToken = "device-token",
            deviceType = "ANDROID",
            isMyReminderOn = true,
            isFamilyReminderOn = true,
            isMissedAlertOn = false,
        )

        given(userRepository.findBySocialId("4903042739")).willReturn(user)
        given(userDeviceRepository.findFirstByUser_IdAndIsActiveTrueOrderByCreatedAtDesc(1L)).willReturn(userDevice)

        val response = userService.updateNotificationSettings(
            socialId = "4903042739",
            request = UserNotificationSettingsUpdateRequest(
                isFamilyReminderOn = false,
            ),
        )

        assertEquals(true, userDevice.isMyReminderOn)
        assertEquals(false, userDevice.isFamilyReminderOn)
        assertEquals(false, userDevice.isMissedAlertOn)
        assertEquals(
            UserNotificationSettingsResponse(
                isMyReminderOn = true,
                isFamilyReminderOn = false,
                isMissedAlertOn = false,
            ),
            response,
        )
    }

    @Test
    fun `알림 설정 수정은 비활성 기기를 제외하고 최신 활성 기기만 변경한다`() {
        val user = User(
            id = 1L,
            socialId = "4903042739",
        )
        val inactiveDevice = UserDevice(
            id = 10L,
            user = user,
            deviceToken = "inactive-device-token",
            deviceType = "ANDROID",
            isMyReminderOn = true,
            isFamilyReminderOn = true,
            isMissedAlertOn = true,
            isActive = false,
        )
        val activeDevice = UserDevice(
            id = 11L,
            user = user,
            deviceToken = "active-device-token",
            deviceType = "IOS",
            isMyReminderOn = true,
            isFamilyReminderOn = true,
            isMissedAlertOn = true,
            isActive = true,
        )

        given(userRepository.findBySocialId("4903042739")).willReturn(user)
        given(userDeviceRepository.findFirstByUser_IdAndIsActiveTrueOrderByCreatedAtDesc(1L)).willReturn(activeDevice)

        val response = userService.updateNotificationSettings(
            socialId = "4903042739",
            request = UserNotificationSettingsUpdateRequest(
                isMyReminderOn = false,
                isFamilyReminderOn = false,
                isMissedAlertOn = false,
            ),
        )

        assertEquals(false, activeDevice.isMyReminderOn)
        assertEquals(false, activeDevice.isFamilyReminderOn)
        assertEquals(false, activeDevice.isMissedAlertOn)
        assertEquals(true, inactiveDevice.isMyReminderOn)
        assertEquals(true, inactiveDevice.isFamilyReminderOn)
        assertEquals(true, inactiveDevice.isMissedAlertOn)
        assertEquals(
            UserNotificationSettingsResponse(
                isMyReminderOn = false,
                isFamilyReminderOn = false,
                isMissedAlertOn = false,
            ),
            response,
        )
    }

    @Test
    fun `알림 설정 수정은 활성 기기가 없으면 DEVICE_TOKEN_NOT_FOUND 예외를 던진다`() {
        val user = User(
            id = 1L,
            socialId = "4903042739",
        )

        given(userRepository.findBySocialId("4903042739")).willReturn(user)
        given(userDeviceRepository.findFirstByUser_IdAndIsActiveTrueOrderByCreatedAtDesc(1L)).willReturn(null)

        val exception = assertThrows(BusinessException::class.java) {
            userService.updateNotificationSettings(
                socialId = "4903042739",
                request = UserNotificationSettingsUpdateRequest(
                    isMyReminderOn = false,
                ),
            )
        }

        assertEquals(ErrorCode.DEVICE_TOKEN_NOT_FOUND, exception.errorCode)
    }

    @Test
    fun `getMyProfile throws USER_NOT_FOUND when the member record is missing`() {
        given(userRepository.findBySocialId("4903042739")).willReturn(null)

        val exception = assertThrows(BusinessException::class.java) {
            userService.getMyProfile("4903042739")
        }

        assertEquals(ErrorCode.USER_NOT_FOUND, exception.errorCode)
    }

    @Test
    fun `completeTutorial stores health profile and marks tutorial complete`() {
        val user = User(
            id = 1L,
            socialId = "4903042739",
            isTutorialCompleted = false,
        )
        val request = TutorialRequest(
            birthDate = "1985-03-15",
            gender = "male",
            height = 180,
            weight = 75,
            bloodType = "o",
            rhType = "plus",
            diseaseCodes = listOf("D001", "D002"),
            allergies = listOf(
                TutorialAllergyRequest(
                    type = "food",
                    value = "Peanut",
                    name = "Peanut",
                )
            ),
        )

        given(userRepository.findBySocialId("4903042739")).willReturn(user)
        given(userHealthProfileRepository.findById(1L)).willReturn(Optional.empty())
        given(diseaseMasterRepository.findAllById(listOf("D001", "D002"))).willReturn(
            listOf(
                DiseaseMaster(
                    diseaseCode = "D001",
                    diseaseName = "Disease 1",
                ),
                DiseaseMaster(
                    diseaseCode = "D002",
                    diseaseName = "Disease 2",
                ),
            )
        )

        val response = userService.completeTutorial("4903042739", request)

        assertEquals(true, response.isTutorialCompleted)
        assertEquals(true, user.isTutorialCompleted)
        verify(diseaseMasterRepository).findAllById(listOf("D001", "D002"))
        verify(userDiseaseMapRepository).saveAll(anyList())
        verify(userAllergyRepository).saveAll(anyList())
    }

    @Test
    fun `completeTutorial deduplicates disease mappings before saving`() {
        val user = User(
            id = 1L,
            socialId = "4903042739",
            isTutorialCompleted = false,
        )
        val request = TutorialRequest(
            birthDate = "1985-03-15",
            gender = "MALE",
            height = 180,
            weight = 75,
            bloodType = "O",
            rhType = "PLUS",
            diseaseCodes = listOf("D001", "D001", "D002"),
            allergies = emptyList(),
        )

        given(userRepository.findBySocialId("4903042739")).willReturn(user)
        given(userHealthProfileRepository.findById(1L)).willReturn(Optional.empty())
        given(diseaseMasterRepository.findAllById(listOf("D001", "D002"))).willReturn(
            listOf(
                DiseaseMaster(
                    diseaseCode = "D001",
                    diseaseName = "Disease 1",
                ),
                DiseaseMaster(
                    diseaseCode = "D002",
                    diseaseName = "Disease 2",
                ),
            )
        )

        val response = userService.completeTutorial("4903042739", request)

        @Suppress("UNCHECKED_CAST")
        val diseaseMapsCaptor = ArgumentCaptor.forClass(Iterable::class.java) as ArgumentCaptor<Iterable<UserDiseaseMap>>

        assertEquals(true, response.isTutorialCompleted)
        verify(userDiseaseMapRepository).saveAll(diseaseMapsCaptor.capture())
        assertEquals(
            listOf("D001", "D002"),
            diseaseMapsCaptor.value.map { it.disease.diseaseCode },
        )
    }

    @Test
    fun `invalid birth date format throws INVALID_DATE_FORMAT`() {
        val user = User(
            id = 1L,
            socialId = "4903042739",
            isTutorialCompleted = false,
        )
        val request = TutorialRequest(
            birthDate = "1985/03/15",
            gender = "MALE",
        )

        given(userRepository.findBySocialId("4903042739")).willReturn(user)
        given(userHealthProfileRepository.findById(1L)).willReturn(Optional.empty())

        val exception = assertThrows(BusinessException::class.java) {
            userService.completeTutorial("4903042739", request)
        }

        assertEquals(ErrorCode.INVALID_DATE_FORMAT, exception.errorCode)
    }

    @Test
    fun `already completed tutorial throws TUTORIAL_ALREADY_COMPLETED`() {
        val user = User(
            id = 1L,
            socialId = "4903042739",
            isTutorialCompleted = true,
        )
        val request = TutorialRequest(
            birthDate = "1985-03-15",
            gender = "MALE",
        )

        given(userRepository.findBySocialId("4903042739")).willReturn(user)

        val exception = assertThrows(BusinessException::class.java) {
            userService.completeTutorial("4903042739", request)
        }

        assertEquals(ErrorCode.TUTORIAL_ALREADY_COMPLETED, exception.errorCode)
    }

    @Test
    fun `completeTutorial는 FOOD 알러지 value가 20자 이상이면 INVALID_ALLERGY_FORMAT 예외를 던진다`() {
        val user = User(
            id = 1L,
            socialId = "4903042739",
            isTutorialCompleted = false,
        )
        val request = TutorialRequest(
            birthDate = "1985-03-15",
            gender = "MALE",
            allergies = listOf(
                TutorialAllergyRequest(
                    type = "FOOD",
                    value = "가".repeat(20),
                    name = "땅콩",
                ),
            ),
        )

        given(userRepository.findBySocialId("4903042739")).willReturn(user)
        given(userHealthProfileRepository.findById(1L)).willReturn(Optional.empty())

        val exception = assertThrows(BusinessException::class.java) {
            userService.completeTutorial("4903042739", request)
        }

        assertEquals(ErrorCode.INVALID_ALLERGY_FORMAT, exception.errorCode)
    }

    @Test
    fun `registerDeviceToken creates a new device token`() {
        val user = User(
            id = 1L,
            socialId = "4903042739",
        )
        val savedDevice = UserDevice(
            id = 10L,
            user = user,
            deviceToken = "device-token",
            deviceType = "ANDROID",
        )

        given(userRepository.findBySocialId("4903042739")).willReturn(user)
        given(userDeviceRepository.findByDeviceToken("device-token")).willReturn(null)
        given(userDeviceRepository.save(any(UserDevice::class.java))).willReturn(savedDevice)

        val response = userService.registerDeviceToken(
            socialId = "4903042739",
            request = DeviceTokenRequest(
                deviceToken = "device-token",
                deviceType = "ANDROID",
            ),
        )

        assertEquals(10L, response.deviceId)
    }

    @Test
    fun `registerDeviceToken transfers existing token to current user and activates it`() {
        val currentUser = User(
            id = 1L,
            socialId = "4903042739",
        )
        val previousUser = User(
            id = 2L,
            socialId = "previous",
        )
        val existingDevice = UserDevice(
            id = 10L,
            user = previousUser,
            deviceToken = "device-token",
            deviceType = "IOS",
            isActive = false,
            isMyReminderOn = false,
            isFamilyReminderOn = false,
            isMissedAlertOn = false,
        )

        given(userRepository.findBySocialId("4903042739")).willReturn(currentUser)
        given(userDeviceRepository.findByDeviceToken("device-token")).willReturn(existingDevice)
        given(userDeviceRepository.save(existingDevice)).willReturn(existingDevice)

        val response = userService.registerDeviceToken(
            socialId = "4903042739",
            request = DeviceTokenRequest(
                deviceToken = "device-token",
                deviceType = "ANDROID",
            ),
        )

        assertEquals(10L, response.deviceId)
        assertEquals(currentUser, existingDevice.user)
        assertEquals("ANDROID", existingDevice.deviceType)
        assertEquals(true, existingDevice.isActive)
        assertEquals(true, existingDevice.isMyReminderOn)
        assertEquals(true, existingDevice.isFamilyReminderOn)
        assertEquals(true, existingDevice.isMissedAlertOn)
    }

    @Test
    fun `registerDeviceToken throws INVALID_REQUEST when token is blank`() {
        val exception = assertThrows(BusinessException::class.java) {
            userService.registerDeviceToken(
                socialId = "4903042739",
                request = DeviceTokenRequest(
                    deviceToken = " ",
                    deviceType = "ANDROID",
                ),
            )
        }

        assertEquals(ErrorCode.INVALID_REQUEST, exception.errorCode)
    }

    @Test
    fun `registerDeviceToken throws DEVICE_TOKEN_TOO_LONG when token is too long`() {
        val user = User(
            id = 1L,
            socialId = "4903042739",
        )

        given(userRepository.findBySocialId("4903042739")).willReturn(user)

        val exception = assertThrows(BusinessException::class.java) {
            userService.registerDeviceToken(
                socialId = "4903042739",
                request = DeviceTokenRequest(
                    deviceToken = "a".repeat(513),
                    deviceType = "ANDROID",
                ),
            )
        }

        assertEquals(ErrorCode.DEVICE_TOKEN_TOO_LONG, exception.errorCode)
    }

    @Test
    fun `registerDeviceToken throws UNSUPPORTED_DEVICE_TYPE when type is invalid`() {
        val user = User(
            id = 1L,
            socialId = "4903042739",
        )

        given(userRepository.findBySocialId("4903042739")).willReturn(user)

        val exception = assertThrows(BusinessException::class.java) {
            userService.registerDeviceToken(
                socialId = "4903042739",
                request = DeviceTokenRequest(
                    deviceToken = "device-token",
                    deviceType = "WEB",
                ),
            )
        }

        assertEquals(ErrorCode.UNSUPPORTED_DEVICE_TYPE, exception.errorCode)
    }

    @Test
    fun `deactivateDeviceToken deactivates current user's device token`() {
        val user = User(
            id = 1L,
            socialId = "4903042739",
        )
        val userDevice = UserDevice(
            id = 10L,
            user = user,
            deviceToken = "device-token",
            deviceType = "ANDROID",
            isActive = true,
        )

        given(userRepository.findBySocialId("4903042739")).willReturn(user)
        given(userDeviceRepository.findByDeviceToken("device-token")).willReturn(userDevice)

        val response = userService.deactivateDeviceToken(
            socialId = "4903042739",
            request = DeviceTokenDeactivateRequest(
                deviceToken = "device-token",
            ),
        )

        assertEquals("기기 푸시 토큰이 성공적으로 해제되었습니다.", response.message)
        assertEquals(false, userDevice.isActive)
    }

    @Test
    fun `deactivateDeviceToken succeeds when token is already inactive`() {
        val user = User(
            id = 1L,
            socialId = "4903042739",
        )
        val userDevice = UserDevice(
            id = 10L,
            user = user,
            deviceToken = "device-token",
            deviceType = "ANDROID",
            isActive = false,
        )

        given(userRepository.findBySocialId("4903042739")).willReturn(user)
        given(userDeviceRepository.findByDeviceToken("device-token")).willReturn(userDevice)

        val response = userService.deactivateDeviceToken(
            socialId = "4903042739",
            request = DeviceTokenDeactivateRequest(
                deviceToken = "device-token",
            ),
        )

        assertEquals("기기 푸시 토큰이 성공적으로 해제되었습니다.", response.message)
        assertEquals(false, userDevice.isActive)
    }

    @Test
    fun `deactivateDeviceToken throws DEVICE_TOKEN_NOT_FOUND when token does not exist`() {
        val user = User(
            id = 1L,
            socialId = "4903042739",
        )

        given(userRepository.findBySocialId("4903042739")).willReturn(user)
        given(userDeviceRepository.findByDeviceToken("device-token")).willReturn(null)

        val exception = assertThrows(BusinessException::class.java) {
            userService.deactivateDeviceToken(
                socialId = "4903042739",
                request = DeviceTokenDeactivateRequest(
                    deviceToken = "device-token",
                ),
            )
        }

        assertEquals(ErrorCode.DEVICE_TOKEN_NOT_FOUND, exception.errorCode)
    }

    @Test
    fun `deactivateDeviceToken throws DEVICE_TOKEN_ACCESS_DENIED when token belongs to another user`() {
        val currentUser = User(
            id = 1L,
            socialId = "4903042739",
        )
        val otherUser = User(
            id = 2L,
            socialId = "other",
        )
        val userDevice = UserDevice(
            id = 10L,
            user = otherUser,
            deviceToken = "device-token",
            deviceType = "ANDROID",
            isActive = true,
        )

        given(userRepository.findBySocialId("4903042739")).willReturn(currentUser)
        given(userDeviceRepository.findByDeviceToken("device-token")).willReturn(userDevice)

        val exception = assertThrows(BusinessException::class.java) {
            userService.deactivateDeviceToken(
                socialId = "4903042739",
                request = DeviceTokenDeactivateRequest(
                    deviceToken = "device-token",
                ),
            )
        }

        assertEquals(ErrorCode.DEVICE_TOKEN_ACCESS_DENIED, exception.errorCode)
        assertEquals(true, userDevice.isActive)
        assertEquals(2L, userDevice.user.id)
    }
}
