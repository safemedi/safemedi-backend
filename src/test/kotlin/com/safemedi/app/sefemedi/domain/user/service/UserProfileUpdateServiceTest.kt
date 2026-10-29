package com.safemedi.app.sefemedi.domain.user.service

import com.safemedi.app.sefemedi.domain.drug.entity.DiseaseMaster
import com.safemedi.app.sefemedi.domain.drug.repository.DiseaseMasterRepository
import com.safemedi.app.sefemedi.domain.family.repository.FamilyRepository
import com.safemedi.app.sefemedi.domain.user.dto.AllergyResponse
import com.safemedi.app.sefemedi.domain.user.dto.DiseaseResponse
import com.safemedi.app.sefemedi.domain.user.dto.FamilyResponse
import com.safemedi.app.sefemedi.domain.user.dto.UserNotificationSettingsResponse
import com.safemedi.app.sefemedi.domain.user.dto.UserProfileUpdateAllergyRequest
import com.safemedi.app.sefemedi.domain.user.dto.UserProfileUpdateRequest
import com.safemedi.app.sefemedi.domain.user.dto.UserProfileResponse
import com.safemedi.app.sefemedi.domain.user.entity.AllergyType
import com.safemedi.app.sefemedi.domain.user.entity.BloodType
import com.safemedi.app.sefemedi.domain.user.entity.Gender
import com.safemedi.app.sefemedi.domain.user.entity.RhType
import com.safemedi.app.sefemedi.domain.user.entity.User
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
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import java.time.LocalDate
import java.util.Optional

class UserProfileUpdateServiceTest {

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
    fun `updateMyProfile replaces profile fields allergies and diseases`() {
        val user = User(
            id = 1L,
            nickname = "기존닉네임",
            socialId = "4903042739",
            inviteCode = "A8F9K2",
            isTutorialCompleted = true,
        )
        val profile = UserHealthProfile(
            userId = 1L,
            user = user,
            birthDate = LocalDate.of(1985, 3, 15),
            gender = Gender.FEMALE,
            height = 180,
            weight = 75,
            bloodType = BloodType.A,
            rhType = RhType.MINUS,
        )
        val oldDiseaseMap = UserDiseaseMap(
            id = 10L,
            user = user,
            disease = DiseaseMaster(
                diseaseCode = "I10",
                diseaseName = "고혈압",
            ),
        )
        val removedDiseaseMap = UserDiseaseMap(
            id = 11L,
            user = user,
            disease = DiseaseMaster(
                diseaseCode = "E11",
                diseaseName = "당뇨병",
            ),
        )
        val addedDiseaseMap = UserDiseaseMap(
            id = 12L,
            user = user,
            disease = DiseaseMaster(
                diseaseCode = "J30",
                diseaseName = "비염",
            ),
        )
        val oldAllergy = UserAllergy(
            id = 20L,
            user = user,
            allergyType = AllergyType.FOOD,
            allergyValue = "Peanut",
            allergyName = "땅콩",
        )
        val retainedAllergy = UserAllergy(
            id = 21L,
            user = user,
            allergyType = AllergyType.INGREDIENT,
            allergyValue = "M249154",
            allergyName = "아세트아미노펜",
        )
        val addedAllergy = UserAllergy(
            id = 22L,
            user = user,
            allergyType = AllergyType.CUSTOM,
            allergyValue = "꽃가루",
            allergyName = "꽃가루",
        )

        given(userRepository.findBySocialId("4903042739")).willReturn(user)
        given(userHealthProfileRepository.findById(1L)).willReturn(Optional.of(profile))
        given(userDiseaseMapRepository.findAllByUser_IdOrderByCreatedAtAsc(1L)).willReturn(
            listOf(oldDiseaseMap, removedDiseaseMap),
            listOf(oldDiseaseMap, addedDiseaseMap),
        )
        given(userAllergyRepository.findAllByUser_IdOrderByCreatedAtAsc(1L)).willReturn(
            listOf(oldAllergy, retainedAllergy),
            listOf(retainedAllergy, addedAllergy),
        )
        given(diseaseMasterRepository.findAllById(listOf("J30", "I10"))).willReturn(
            listOf(
                DiseaseMaster(
                    diseaseCode = "J30",
                    diseaseName = "비염",
                ),
                DiseaseMaster(
                    diseaseCode = "I10",
                    diseaseName = "고혈압",
                ),
            )
        )
        given(familyRepository.findAllByUser_IdOrderByCreatedAtAsc(1L)).willReturn(emptyList())
        given(userDeviceRepository.findFirstByUser_IdOrderByCreatedAtDesc(1L)).willReturn(null)

        val response = userService.updateMyProfile(
            socialId = "4903042739",
            request = UserProfileUpdateRequest(
                nickname = "홍길동01",
                gender = " MALE ",
                bloodType = " O ",
                rhType = " PLUS ",
                diseaseCodes = listOf("j30", "I10", "J30"),
                allergies = listOf(
                    UserProfileUpdateAllergyRequest(
                        type = "ingredient",
                        value = "M249154",
                        name = "아세트아미노펜",
                    ),
                    UserProfileUpdateAllergyRequest(
                        type = "CUSTOM",
                        value = "꽃가루",
                        name = "꽃가루",
                    ),
                ),
            ),
        )

        assertEquals("홍길동01", user.nickname)
        assertEquals(Gender.MALE, profile.gender)
        assertEquals(BloodType.O, profile.bloodType)
        assertEquals(RhType.PLUS, profile.rhType)
        verify(userDiseaseMapRepository).deleteAllInBatch(listOf(removedDiseaseMap))
        verify(userAllergyRepository).deleteAllInBatch(listOf(oldAllergy))

        @Suppress("UNCHECKED_CAST")
        val diseaseCaptor = ArgumentCaptor.forClass(Iterable::class.java) as ArgumentCaptor<Iterable<UserDiseaseMap>>
        verify(userDiseaseMapRepository).saveAll(diseaseCaptor.capture())
        assertEquals(
            listOf("J30"),
            diseaseCaptor.value.map { it.disease.diseaseCode },
        )

        @Suppress("UNCHECKED_CAST")
        val allergyCaptor = ArgumentCaptor.forClass(Iterable::class.java) as ArgumentCaptor<Iterable<UserAllergy>>
        verify(userAllergyRepository).saveAll(allergyCaptor.capture())
        assertEquals(
            listOf(AllergyType.CUSTOM),
            allergyCaptor.value.map { it.allergyType },
        )

        assertEquals(
            UserProfileResponse(
                nickname = "홍길동01",
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
                    DiseaseResponse("J30", "비염"),
                ),
                allergies = listOf(
                    AllergyResponse(
                        type = AllergyType.INGREDIENT,
                        value = "M249154",
                        name = "아세트아미노펜",
                    ),
                    AllergyResponse(
                        type = AllergyType.CUSTOM,
                        value = "꽃가루",
                        name = "꽃가루",
                    ),
                ),
                families = listOf(
                    FamilyResponse(
                        familyId = 1L,
                        name = "홍길동01",
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
    fun `updateMyProfile throws INVALID_NICKNAME_LENGTH when nickname is too short`() {
        val user = User(
            id = 1L,
            socialId = "4903042739",
        )

        given(userRepository.findBySocialId("4903042739")).willReturn(user)

        val exception = assertThrows(BusinessException::class.java) {
            userService.updateMyProfile(
                socialId = "4903042739",
                request = UserProfileUpdateRequest(
                    nickname = "홍길",
                ),
            )
        }

        assertEquals(ErrorCode.INVALID_NICKNAME_LENGTH, exception.errorCode)
    }

    @Test
    fun `updateMyProfile throws INVALID_DISEASE_CODE when disease code does not exist`() {
        val user = User(
            id = 1L,
            socialId = "4903042739",
        )

        given(userRepository.findBySocialId("4903042739")).willReturn(user)
        given(userHealthProfileRepository.findById(1L)).willReturn(Optional.empty())
        given(diseaseMasterRepository.findAllById(listOf("J30", "XXXX"))).willReturn(
            listOf(
                DiseaseMaster(
                    diseaseCode = "J30",
                    diseaseName = "비염",
                ),
            )
        )

        val exception = assertThrows(BusinessException::class.java) {
            userService.updateMyProfile(
                socialId = "4903042739",
                request = UserProfileUpdateRequest(
                    diseaseCodes = listOf("J30", "XXXX"),
                ),
            )
        }

        assertEquals(ErrorCode.INVALID_DISEASE_CODE, exception.errorCode)
    }

    @Test
    fun `updateMyProfile throws INVALID_ALLERGY_FORMAT when allergy type is unsupported`() {
        val user = User(
            id = 1L,
            socialId = "4903042739",
        )

        given(userRepository.findBySocialId("4903042739")).willReturn(user)
        given(userHealthProfileRepository.findById(1L)).willReturn(Optional.empty())

        val exception = assertThrows(BusinessException::class.java) {
            userService.updateMyProfile(
                socialId = "4903042739",
                request = UserProfileUpdateRequest(
                    allergies = listOf(
                        UserProfileUpdateAllergyRequest(
                            type = "UNKNOWN",
                            value = "Peanut",
                            name = "땅콩",
                        ),
                    ),
                ),
            )
        }

        assertEquals(ErrorCode.INVALID_ALLERGY_FORMAT, exception.errorCode)
    }

    @Test
    fun `updateMyProfile는 FOOD 타입 알러지를 사용자가 입력한 값으로 등록한다`() {
        val user = User(
            id = 1L,
            socialId = "4903042739",
        )

        given(userRepository.findBySocialId("4903042739")).willReturn(user)
        given(userHealthProfileRepository.findById(1L)).willReturn(Optional.empty())
        given(userAllergyRepository.findAllByUser_IdOrderByCreatedAtAsc(1L)).willReturn(
            emptyList(),
            emptyList(),
        )
        given(userDiseaseMapRepository.findAllByUser_IdOrderByCreatedAtAsc(1L)).willReturn(emptyList())
        given(familyRepository.findAllByUser_IdOrderByCreatedAtAsc(1L)).willReturn(emptyList())
        given(userDeviceRepository.findFirstByUser_IdOrderByCreatedAtDesc(1L)).willReturn(null)

        userService.updateMyProfile(
            socialId = "4903042739",
            request = UserProfileUpdateRequest(
                allergies = listOf(
                    UserProfileUpdateAllergyRequest(
                        type = "FOOD",
                        value = "Peanut",
                        name = "땅콩",
                    ),
                ),
            ),
        )

        @Suppress("UNCHECKED_CAST")
        val allergyCaptor = ArgumentCaptor.forClass(Iterable::class.java) as ArgumentCaptor<Iterable<UserAllergy>>
        verify(userAllergyRepository).saveAll(allergyCaptor.capture())
        assertEquals(
            listOf(AllergyType.FOOD),
            allergyCaptor.value.map { it.allergyType },
        )
    }

    @Test
    fun `updateMyProfile는 FOOD 알러지 value가 20자 이상이면 INVALID_ALLERGY_FORMAT 예외를 던진다`() {
        val user = User(
            id = 1L,
            socialId = "4903042739",
        )

        given(userRepository.findBySocialId("4903042739")).willReturn(user)
        given(userHealthProfileRepository.findById(1L)).willReturn(Optional.empty())

        val exception = assertThrows(BusinessException::class.java) {
            userService.updateMyProfile(
                socialId = "4903042739",
                request = UserProfileUpdateRequest(
                    allergies = listOf(
                        UserProfileUpdateAllergyRequest(
                            type = "FOOD",
                            value = "가".repeat(20),
                            name = "땅콩",
                        ),
                    ),
                ),
            )
        }

        assertEquals(ErrorCode.INVALID_ALLERGY_FORMAT, exception.errorCode)
    }

    @Test
    fun `updateMyProfile는 FOOD 알러지 name이 20자 이상이면 INVALID_ALLERGY_FORMAT 예외를 던진다`() {
        val user = User(
            id = 1L,
            socialId = "4903042739",
        )

        given(userRepository.findBySocialId("4903042739")).willReturn(user)
        given(userHealthProfileRepository.findById(1L)).willReturn(Optional.empty())

        val exception = assertThrows(BusinessException::class.java) {
            userService.updateMyProfile(
                socialId = "4903042739",
                request = UserProfileUpdateRequest(
                    allergies = listOf(
                        UserProfileUpdateAllergyRequest(
                            type = "FOOD",
                            value = "Peanut",
                            name = "땅콩".repeat(10),
                        ),
                    ),
                ),
            )
        }

        assertEquals(ErrorCode.INVALID_ALLERGY_FORMAT, exception.errorCode)
    }
}
