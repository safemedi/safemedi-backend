package com.safemedi.app.sefemedi.domain.user.service

import com.safemedi.app.sefemedi.domain.drug.entity.DiseaseMaster
import com.safemedi.app.sefemedi.domain.family.entity.Family
import com.safemedi.app.sefemedi.domain.family.repository.FamilyRepository
import com.safemedi.app.sefemedi.domain.medication.entity.Prescription
import com.safemedi.app.sefemedi.domain.medication.entity.PrescriptionDrug
import com.safemedi.app.sefemedi.domain.medication.repository.PrescriptionDrugRepository
import com.safemedi.app.sefemedi.domain.medication.repository.PrescriptionRepository
import com.safemedi.app.sefemedi.domain.user.dto.AllergyResponse
import com.safemedi.app.sefemedi.domain.user.dto.DiseaseResponse
import com.safemedi.app.sefemedi.domain.user.dto.MedicalSummaryActiveMedicationResponse
import com.safemedi.app.sefemedi.domain.user.dto.MedicalSummaryMedicationResponse
import com.safemedi.app.sefemedi.domain.user.dto.MedicalSummaryResponse
import com.safemedi.app.sefemedi.domain.user.entity.AllergyType
import com.safemedi.app.sefemedi.domain.user.entity.BloodType
import com.safemedi.app.sefemedi.domain.user.entity.Gender
import com.safemedi.app.sefemedi.domain.user.entity.RhType
import com.safemedi.app.sefemedi.domain.user.entity.User
import com.safemedi.app.sefemedi.domain.user.entity.UserAllergy
import com.safemedi.app.sefemedi.domain.user.entity.UserDiseaseMap
import com.safemedi.app.sefemedi.domain.user.entity.UserHealthProfile
import com.safemedi.app.sefemedi.domain.user.repository.UserAllergyRepository
import com.safemedi.app.sefemedi.domain.user.repository.UserDiseaseMapRepository
import com.safemedi.app.sefemedi.domain.user.repository.UserHealthProfileRepository
import com.safemedi.app.sefemedi.domain.user.repository.UserRepository
import com.safemedi.app.sefemedi.global.error.BusinessException
import com.safemedi.app.sefemedi.global.error.ErrorCode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import java.time.LocalDate
import java.time.ZoneId
import java.util.Optional

class MedicalSummaryServiceTest {

    private lateinit var userRepository: UserRepository
    private lateinit var userHealthProfileRepository: UserHealthProfileRepository
    private lateinit var userDiseaseMapRepository: UserDiseaseMapRepository
    private lateinit var userAllergyRepository: UserAllergyRepository
    private lateinit var familyRepository: FamilyRepository
    private lateinit var prescriptionRepository: PrescriptionRepository
    private lateinit var prescriptionDrugRepository: PrescriptionDrugRepository
    private lateinit var service: MedicalSummaryService

    private val currentUser = User(
        id = 1L,
        nickname = "정민성",
        socialId = "kakao-123",
    )
    private val serviceZoneId: ZoneId = ZoneId.of("Asia/Seoul")

    @BeforeEach
    fun setUp() {
        userRepository = mock(UserRepository::class.java)
        userHealthProfileRepository = mock(UserHealthProfileRepository::class.java)
        userDiseaseMapRepository = mock(UserDiseaseMapRepository::class.java)
        userAllergyRepository = mock(UserAllergyRepository::class.java)
        familyRepository = mock(FamilyRepository::class.java)
        prescriptionRepository = mock(PrescriptionRepository::class.java)
        prescriptionDrugRepository = mock(PrescriptionDrugRepository::class.java)

        service = MedicalSummaryService(
            userRepository = userRepository,
            userHealthProfileRepository = userHealthProfileRepository,
            userDiseaseMapRepository = userDiseaseMapRepository,
            userAllergyRepository = userAllergyRepository,
            familyRepository = familyRepository,
            prescriptionRepository = prescriptionRepository,
            prescriptionDrugRepository = prescriptionDrugRepository,
        )
    }

    @Test
    fun `getMyMedicalSummary returns aggregated summary with active medications`() {
        val today = LocalDate.now(serviceZoneId)
        val profile = UserHealthProfile(
            userId = 1L,
            user = currentUser,
            birthDate = LocalDate.of(1998, 5, 15),
            gender = Gender.MALE,
            height = 178,
            weight = 72,
            bloodType = BloodType.A,
            rhType = RhType.PLUS,
        )
        val activePrescription = Prescription(
            id = 42L,
            user = currentUser,
            title = "이비인후과 감기약",
            startDate = today.minusDays(3),
            endDate = today.plusDays(4),
        )
        val prescriptionDrugs = listOf(
            PrescriptionDrug(
                id = 1L,
                prescription = activePrescription,
                drugName = "타이레놀정500mg",
            ),
            PrescriptionDrug(
                id = 2L,
                prescription = activePrescription,
                drugName = "아목시실린캡슐",
            ),
        )

        given(userRepository.findBySocialId("kakao-123")).willReturn(currentUser)
        given(userHealthProfileRepository.findById(1L)).willReturn(Optional.of(profile))
        given(userDiseaseMapRepository.findAllByUser_IdOrderByCreatedAtAsc(1L)).willReturn(
            listOf(
                UserDiseaseMap(
                    id = 10L,
                    user = currentUser,
                    disease = DiseaseMaster(
                        diseaseCode = "J30",
                        diseaseName = "알레르기성 비염",
                    ),
                ),
            )
        )
        given(userAllergyRepository.findAllByUser_IdOrderByCreatedAtAsc(1L)).willReturn(
            listOf(
                UserAllergy(
                    id = 20L,
                    user = currentUser,
                    allergyType = AllergyType.ATC_GROUP,
                    allergyValue = "J01C",
                    allergyName = "페니실린계 항생제",
                ),
            )
        )
        given(
            prescriptionRepository.findActiveByUserId(
                userId = 1L,
                today = today,
            )
        ).willReturn(listOf(activePrescription))
        given(prescriptionDrugRepository.findAllByPrescriptionIds(listOf(42L))).willReturn(prescriptionDrugs)

        val response = service.getMyMedicalSummary("kakao-123")

        assertEquals(
            MedicalSummaryResponse(
                name = "정민성",
                birthDate = LocalDate.of(1998, 5, 15),
                gender = Gender.MALE,
                height = 178,
                weight = 72,
                bloodType = BloodType.A,
                rhType = RhType.PLUS,
                diseases = listOf(
                    DiseaseResponse(
                        code = "J30",
                        name = "알레르기성 비염",
                    ),
                ),
                allergies = listOf(
                    AllergyResponse(
                        type = AllergyType.ATC_GROUP,
                        value = "J01C",
                        name = "페니실린계 항생제",
                    ),
                ),
                activeMedications = listOf(
                    MedicalSummaryActiveMedicationResponse(
                        prescriptionId = 42L,
                        prescriptionTitle = "이비인후과 감기약",
                        startDate = today.minusDays(3),
                        endDate = today.plusDays(4),
                        medications = listOf(
                            MedicalSummaryMedicationResponse("타이레놀정500mg"),
                            MedicalSummaryMedicationResponse("아목시실린캡슐"),
                        ),
                    ),
                ),
            ),
            response,
        )
    }

    @Test
    fun `getMyMedicalSummary returns empty arrays when related data is missing`() {
        given(userRepository.findBySocialId("kakao-123")).willReturn(currentUser)
        given(userHealthProfileRepository.findById(1L)).willReturn(Optional.empty())
        given(userDiseaseMapRepository.findAllByUser_IdOrderByCreatedAtAsc(1L)).willReturn(emptyList())
        given(userAllergyRepository.findAllByUser_IdOrderByCreatedAtAsc(1L)).willReturn(emptyList())
        given(prescriptionRepository.findActiveByUserId(1L, LocalDate.now(serviceZoneId))).willReturn(emptyList())

        val response = service.getMyMedicalSummary("kakao-123")

        assertEquals(
            MedicalSummaryResponse(
                name = "정민성",
                birthDate = null,
                gender = null,
                height = null,
                weight = null,
                bloodType = null,
                rhType = null,
                diseases = emptyList(),
                allergies = emptyList(),
                activeMedications = emptyList(),
            ),
            response,
        )
    }

    @Test
    fun `getFamilyMedicalSummary returns family member summary when allowed`() {
        val today = LocalDate.now(serviceZoneId)
        val familyMember = User(
            id = 2L,
            nickname = "정민수",
            socialId = "family-123",
        )
        val family = Family(
            id = 9L,
            user = currentUser,
            connectedUser = familyMember,
            relation = "부",
            isAllowMyInfo = false,
        )
        val profile = UserHealthProfile(
            userId = 2L,
            user = familyMember,
            birthDate = LocalDate.of(1970, 1, 1),
            gender = Gender.MALE,
            height = 170,
            weight = 68,
            bloodType = BloodType.O,
            rhType = RhType.MINUS,
        )
        val activePrescription = Prescription(
            id = 100L,
            user = familyMember,
            title = "만성질환 약",
            startDate = today.minusDays(5),
            endDate = today.plusDays(10),
        )

        given(userRepository.findBySocialId("kakao-123")).willReturn(currentUser)
        given(familyRepository.findByIdAndUser_Id(9L, 1L)).willReturn(family)
        given(familyRepository.findByUser_IdAndConnectedUser_Id(2L, 1L)).willReturn(
            Family(user = family.connectedUser, connectedUser = currentUser, relation = "가족", isAllowMyInfo = true),
        )
        given(userHealthProfileRepository.findById(2L)).willReturn(Optional.of(profile))
        given(userDiseaseMapRepository.findAllByUser_IdOrderByCreatedAtAsc(2L)).willReturn(emptyList())
        given(userAllergyRepository.findAllByUser_IdOrderByCreatedAtAsc(2L)).willReturn(emptyList())
        given(prescriptionRepository.findActiveByUserId(2L, today)).willReturn(listOf(activePrescription))
        given(prescriptionDrugRepository.findAllByPrescriptionIds(listOf(100L))).willReturn(emptyList())

        val response = service.getFamilyMedicalSummary("kakao-123", 9L)

        assertEquals(
            MedicalSummaryResponse(
                name = "정민수",
                birthDate = LocalDate.of(1970, 1, 1),
                gender = Gender.MALE,
                height = 170,
                weight = 68,
                bloodType = BloodType.O,
                rhType = RhType.MINUS,
                diseases = emptyList(),
                allergies = emptyList(),
                activeMedications = listOf(
                    MedicalSummaryActiveMedicationResponse(
                        prescriptionId = 100L,
                        prescriptionTitle = "만성질환 약",
                        startDate = today.minusDays(5),
                        endDate = today.plusDays(10),
                        medications = emptyList(),
                    ),
                ),
            ),
            response,
        )
    }

    @Test
    fun `getFamilyMedicalSummary throws FAMILY_NOT_FOUND when family relation is missing`() {
        given(userRepository.findBySocialId("kakao-123")).willReturn(currentUser)
        given(familyRepository.findByIdAndUser_Id(99L, 1L)).willReturn(null)

        val exception = kotlin.test.assertFailsWith<BusinessException> {
            service.getFamilyMedicalSummary("kakao-123", 99L)
        }

        assertEquals(ErrorCode.FAMILY_NOT_FOUND, exception.errorCode)
    }

    @Test
    fun `요청자가 공개해도 대상이 공유를 거부하면 조회를 차단한다`() {
        val target = User(id = 2L, socialId = "family-user")
        val family = Family(
            id = 9L, user = currentUser, connectedUser = target, relation = "가족", isAllowMyInfo = true,
        )
        given(userRepository.findBySocialId("kakao-123")).willReturn(currentUser)
        given(familyRepository.findByIdAndUser_Id(9L, 1L)).willReturn(family)
        given(familyRepository.findByUser_IdAndConnectedUser_Id(2L, 1L)).willReturn(Family(user = target, connectedUser = currentUser, relation = "가족", isAllowMyInfo = false))

        val exception = kotlin.test.assertFailsWith<BusinessException> {
            service.getFamilyMedicalSummary("kakao-123", 9L)
        }

        assertEquals(ErrorCode.FAMILY_INFO_ACCESS_DENIED, exception.errorCode)
        org.mockito.Mockito.verifyNoInteractions(userHealthProfileRepository, userDiseaseMapRepository, userAllergyRepository, prescriptionRepository, prescriptionDrugRepository)
    }

    @Test
    fun `역방향 가족 연결이 없으면 조회를 차단한다`() {
        val target = User(id = 2L, socialId = "family-user")
        val family = Family(
            id = 9L, user = currentUser, connectedUser = target, relation = "가족", isAllowMyInfo = true,
        )
        given(userRepository.findBySocialId("kakao-123")).willReturn(currentUser)
        given(familyRepository.findByIdAndUser_Id(9L, 1L)).willReturn(family)
        given(familyRepository.findByUser_IdAndConnectedUser_Id(2L, 1L)).willReturn(null)

        val exception = kotlin.test.assertFailsWith<BusinessException> {
            service.getFamilyMedicalSummary("kakao-123", 9L)
        }

        assertEquals(ErrorCode.FAMILY_INFO_ACCESS_DENIED, exception.errorCode)
        org.mockito.Mockito.verifyNoInteractions(userHealthProfileRepository, userDiseaseMapRepository, userAllergyRepository, prescriptionRepository, prescriptionDrugRepository)
    }

}
