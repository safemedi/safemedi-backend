package com.safemedi.app.sefemedi.domain.medication.service

import com.safemedi.app.sefemedi.domain.drug.entity.DrugIngredientMap
import com.safemedi.app.sefemedi.domain.drug.entity.DrugMaster
import com.safemedi.app.sefemedi.domain.drug.entity.DurElderly
import com.safemedi.app.sefemedi.domain.drug.entity.DurElderlyCategory
import com.safemedi.app.sefemedi.domain.drug.entity.DurInteraction
import com.safemedi.app.sefemedi.domain.drug.entity.DurPregnancy
import com.safemedi.app.sefemedi.domain.drug.entity.IngredientMaster
import com.safemedi.app.sefemedi.domain.drug.repository.DrugIngredientMapRepository
import com.safemedi.app.sefemedi.domain.drug.repository.DrugMasterRepository
import com.safemedi.app.sefemedi.domain.drug.repository.DurAgeRepository
import com.safemedi.app.sefemedi.domain.drug.repository.DurElderlyRepository
import com.safemedi.app.sefemedi.domain.drug.repository.DurInteractionRepository
import com.safemedi.app.sefemedi.domain.drug.repository.DurPregnancyRepository
import com.safemedi.app.sefemedi.domain.medication.analyzer.AtcCodeAllergyAnalyzerImpl
import com.safemedi.app.sefemedi.domain.medication.analyzer.DurSafetyAnalyzerImpl
import com.safemedi.app.sefemedi.domain.medication.analyzer.IngredientAllergyAnalyzerImpl
import com.safemedi.app.sefemedi.domain.medication.dto.MedicationAnalyzeRequest
import com.safemedi.app.sefemedi.domain.medication.dto.MedicationSafetyStatus
import com.safemedi.app.sefemedi.domain.medication.dto.MedicationWarningType
import com.safemedi.app.sefemedi.domain.medication.dto.PrescriptionAnalyzeRequest
import com.safemedi.app.sefemedi.domain.notification.dto.NotificationCreateCommand
import com.safemedi.app.sefemedi.domain.notification.entity.NotificationType
import com.safemedi.app.sefemedi.domain.notification.service.NotificationCreateService
import com.safemedi.app.sefemedi.domain.user.entity.AllergyType
import com.safemedi.app.sefemedi.domain.user.entity.User
import com.safemedi.app.sefemedi.domain.user.entity.UserAllergy
import com.safemedi.app.sefemedi.domain.user.entity.UserHealthProfile
import com.safemedi.app.sefemedi.domain.user.repository.UserAllergyRepository
import com.safemedi.app.sefemedi.domain.user.repository.UserHealthProfileRepository
import com.safemedi.app.sefemedi.domain.user.repository.UserRepository
import com.safemedi.app.sefemedi.global.error.BusinessException
import com.safemedi.app.sefemedi.global.error.ErrorCode
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockingDetails
import org.mockito.Mockito.verifyNoInteractions
import java.time.Clock
import com.safemedi.app.sefemedi.domain.drug.entity.DurCoverage
import com.safemedi.app.sefemedi.domain.drug.entity.DurType
import com.safemedi.app.sefemedi.domain.drug.repository.DurCoverageRepository
import org.mockito.Mockito.RETURNS_DEFAULTS
import java.time.LocalDate
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class PrescriptionAnalyzeServiceTest {
    private lateinit var userRepository: UserRepository
    private lateinit var userAllergyRepository: UserAllergyRepository
    private lateinit var userHealthProfileRepository: UserHealthProfileRepository
    private lateinit var drugIngredientMapRepository: DrugIngredientMapRepository
    private lateinit var drugMasterRepository: DrugMasterRepository
    private lateinit var durElderlyRepository: DurElderlyRepository
    private lateinit var durAgeRepository: DurAgeRepository
    private lateinit var durPregnancyRepository: DurPregnancyRepository
    private lateinit var durInteractionRepository: DurInteractionRepository
    private lateinit var durCoverageRepository: DurCoverageRepository
    private lateinit var notificationCreateService: NotificationCreateService
    private lateinit var service: PrescriptionAnalyzeService

    private val user = User(
        id = 1L,
        socialId = "kakao-123",
    )

    @BeforeEach
    fun setUp() {
        userRepository = mock(UserRepository::class.java)
        userAllergyRepository = mock(UserAllergyRepository::class.java)
        userHealthProfileRepository = mock(UserHealthProfileRepository::class.java)
        drugIngredientMapRepository = mock(DrugIngredientMapRepository::class.java)
        drugMasterRepository = mock(DrugMasterRepository::class.java)
        durElderlyRepository = mock(DurElderlyRepository::class.java)
        durAgeRepository = mock(DurAgeRepository::class.java)
        durPregnancyRepository = mock(DurPregnancyRepository::class.java)
        durInteractionRepository = mock(DurInteractionRepository::class.java)
        notificationCreateService = mock(NotificationCreateService::class.java)

        durCoverageRepository = mock(DurCoverageRepository::class.java) { invocation ->
            if (invocation.method.name == "findByDrugCodeInAndVerifiedTrue") {
                val codes = invocation.arguments[0] as Collection<*>
                codes.flatMap { code ->
                    DurType.entries.map { type -> DurCoverage("$code:$type", code.toString(), type, "test", true) }
                }
            } else RETURNS_DEFAULTS.answer(invocation)
        }

        service = PrescriptionAnalyzeService(
            userRepository = userRepository,
            userAllergyRepository = userAllergyRepository,
            userHealthProfileRepository = userHealthProfileRepository,
            drugIngredientMapRepository = drugIngredientMapRepository,
            drugMasterRepository = drugMasterRepository,
            notificationCreateService = notificationCreateService,
            analyzers = listOf(
                DurSafetyAnalyzerImpl(
                    durElderlyRepository = durElderlyRepository,
                    durAgeRepository = durAgeRepository,
                    durPregnancyRepository = durPregnancyRepository,
                    durInteractionRepository = durInteractionRepository,
                    durCoverageRepository = durCoverageRepository,
                    clock = Clock.systemUTC(),
                ),
                AtcCodeAllergyAnalyzerImpl(),
                IngredientAllergyAnalyzerImpl(),
            ),
        )
    }

    @Test
    fun `에러 테스트`() {
        val exception = assertFailsWith<BusinessException> {
            service.analyze(
                socialId = "kakao-123",
                request = PrescriptionAnalyzeRequest(medications = emptyList()),
            )
        }

        assertEquals(ErrorCode.EMPTY_MEDICATIONS, exception.errorCode)
        assertEquals("MED_002", exception.errorCode.code)
    }

    @Test
    fun `분석 테스트`() {
        val medications = listOf(
            MedicationAnalyzeRequest(drugCode = "D001"),
            MedicationAnalyzeRequest(drugCode = "D002"),
        )
        val tylenolDrug = DrugMaster(
            drugCode = "D001",
            drugName = "Tylenol 500mg",
            atcCode = "N02BE01",
        )
        val amlodipineDrug = DrugMaster(
            drugCode = "D002",
            drugName = "Amlodipine 5mg",
            atcCode = "C08CA01",
        )
        val ingredientMap = DrugIngredientMap(
            drug = tylenolDrug,
            ingredient = IngredientMaster(
                ingredientCode = "I001",
                ingredientName = "Acetaminophen",
            ),
        )
        val allergies = listOf(
            UserAllergy(
                user = user,
                allergyType = AllergyType.INGREDIENT,
                allergyValue = "I001",
                allergyName = "Acetaminophen",
            ),
            UserAllergy(
                user = user,
                allergyType = AllergyType.ATC_GROUP,
                allergyValue = "C08",
                allergyName = "Calcium channel blocker",
            ),
        )
        val drugCodes = listOf("D001", "D002")
        val drugNames = listOf("Tylenol 500mg", "Amlodipine 5mg")
        val interaction = DurInteraction(
            drugCodeA = "D001", drugCodeB = "D002", runtimeVerified = true,
            drugNameA = "Tylenol 500mg",
            drugNameB = "Amlodipine 5mg",
            noticeNumber = "DUR-001",
            noticeDate = LocalDate.of(2026, 1, 1),
            warningMessage = "Contraindicated interaction.",
        )

        given(userRepository.findBySocialId("kakao-123")).willReturn(user)
        given(userAllergyRepository.findAllByUser_IdOrderByCreatedAtAsc(1L)).willReturn(allergies)
        given(userHealthProfileRepository.findById(1L)).willReturn(Optional.of(UserHealthProfile(userId = 1L, user = user, birthDate = LocalDate.of(1990, 1, 1))))
        given(drugMasterRepository.findAllById(listOf("D001", "D002"))).willReturn(listOf(tylenolDrug, amlodipineDrug))
        given(drugIngredientMapRepository.findAllByDrugCodes(listOf("D001", "D002"))).willReturn(listOf(ingredientMap))
        given(durAgeRepository.findByDrugCodeInAndRuntimeVerifiedTrue(drugCodes)).willReturn(emptyList())
        given(durPregnancyRepository.findByDrugCodeInAndRuntimeVerifiedTrue(drugCodes)).willReturn(emptyList())
        given(durInteractionRepository.findInteractions(drugCodes)).willReturn(listOf(interaction))

        val response = service.analyze(
            socialId = "kakao-123",
            request = PrescriptionAnalyzeRequest(medications = medications),
        )

        assertEquals(0, response.safetySummary.safeCount)
        assertEquals(0, response.safetySummary.warningCount)
        assertEquals(2, response.safetySummary.dangerCount)

        val tylenol = response.analyzedMedications.first { it.drugName == "Tylenol 500mg" }
        assertEquals("N02BE01", tylenol.atcCode)
        assertEquals(MedicationSafetyStatus.DANGER, tylenol.status)
        assertTrue(tylenol.warnings.any { it.type == MedicationWarningType.ALLERGY })
        assertTrue(tylenol.warnings.any { it.type == MedicationWarningType.DUR_INTERACTION })

        val amlodipine = response.analyzedMedications.first { it.drugName == "Amlodipine 5mg" }
        assertEquals("C08CA01", amlodipine.atcCode)
        assertEquals(MedicationSafetyStatus.DANGER, amlodipine.status)
        assertTrue(amlodipine.warnings.any { it.type == MedicationWarningType.ALLERGY })
        assertTrue(amlodipine.warnings.any { it.type == MedicationWarningType.DUR_INTERACTION })

        val command = mockingDetails(notificationCreateService)
            .invocations
            .single()
            .arguments[0] as NotificationCreateCommand

        assertEquals(1L, command.userId)
        assertEquals(NotificationType.DRUG_INTERACTION_WARNING, command.type)
        assertEquals("약물 위험 경고", command.title)
        assertEquals("Tylenol 500mg, Amlodipine 5mg 약물에 위험 경고가 있어요", command.content)
        assertEquals("DRUG_RISK_WARNING:ANALYZE:1:D001,D002", command.deduplicationKey)
    }

    @Test
    fun `임신 중이면 약품 코드로 M등급 임부 금기 경고를 반환한다`() {
        val drug = DrugMaster(drugCode = "D001", drugName = "금기 약물", atcCode = "A01AA01")
        given(userRepository.findBySocialId("kakao-123")).willReturn(user)
        given(userHealthProfileRepository.findById(1L)).willReturn(
            Optional.of(UserHealthProfile(userId = 1L, user = user, isPregnant = true))
        )
        given(drugMasterRepository.findAllById(listOf("D001"))).willReturn(listOf(drug))
        given(drugIngredientMapRepository.findAllByDrugCodes(listOf("D001"))).willReturn(emptyList())
        given(durPregnancyRepository.findByDrugCodeInAndRuntimeVerifiedTrue(listOf("D001"))).willReturn(
            listOf(
                DurPregnancy(
                    drugCode = "D001",
                    durProductCode = "645000001",
                    runtimeVerified = true,
                    drugName = "이름이 다른 DUR 원본 약물",
                    ingredientCode = "100001ABC",
                    grade = "M",
                    noticeNumber = "DUR-001",
                    noticeDate = LocalDate.of(2026, 1, 1),
                    warningMessage = null,
                )
            )
        )

        val response = service.analyze(
            socialId = "kakao-123",
            request = PrescriptionAnalyzeRequest(listOf(MedicationAnalyzeRequest("D001"))),
        )

        assertEquals(MedicationSafetyStatus.DANGER, response.analyzedMedications.single().status)
        assertTrue(
            response.analyzedMedications.single().warnings.any {
                it.type == MedicationWarningType.DUR_PREGNANCY &&
                    it.message == "임부금기 조건부 대상 의약품입니다. 치료 목적에 따라 금기 등급이 달라질 수 있습니다."
            }
        )
    }

    @Test
    fun `65세 이상이면 약품 코드로 노인주의 경고를 반환한다`() {
        val drug = DrugMaster(drugCode = "D001", drugName = "노인주의 약물", atcCode = "A01AA01")
        given(userRepository.findBySocialId("kakao-123")).willReturn(user)
        given(userHealthProfileRepository.findById(1L)).willReturn(
            Optional.of(UserHealthProfile(userId = 1L, user = user, birthDate = LocalDate.of(1950, 1, 1)))
        )
        given(drugMasterRepository.findAllById(listOf("D001"))).willReturn(listOf(drug))
        given(drugIngredientMapRepository.findAllByDrugCodes(listOf("D001"))).willReturn(emptyList())
        given(durElderlyRepository.findByDrugCodeInAndRuntimeVerifiedTrue(listOf("D001"))).willReturn(
            listOf(
                DurElderly(
                    drugCode = "D001",
                    durProductCode = "645000002",
                    runtimeVerified = true,
                    drugName = "이름이 다른 DUR 원본 약물",
                    ingredientCode = "100002ABC",
                    category = DurElderlyCategory.NSAID,
                    warningMessage = null,
                )
            )
        )

        val response = service.analyze(
            socialId = "kakao-123",
            request = PrescriptionAnalyzeRequest(listOf(MedicationAnalyzeRequest("D001"))),
        )

        assertEquals(MedicationSafetyStatus.WARNING, response.analyzedMedications.single().status)
        assertTrue(
            response.analyzedMedications.single().warnings.any {
                it.type == MedicationWarningType.DUR_ELDERLY &&
                    it.message == "노인주의 대상 의약품입니다."
            }
        )
    }

    @Test
    fun `임신 중이 아니면 임부 금기 경고를 반환하지 않는다`() {
        val drug = DrugMaster(drugCode = "D001", drugName = "금기 약물", atcCode = "A01AA01")
        given(userRepository.findBySocialId("kakao-123")).willReturn(user)
        given(userHealthProfileRepository.findById(1L)).willReturn(
            Optional.of(UserHealthProfile(userId = 1L, user = user, isPregnant = false, birthDate = LocalDate.of(1990, 1, 1)))
        )
        given(drugMasterRepository.findAllById(listOf("D001"))).willReturn(listOf(drug))
        given(drugIngredientMapRepository.findAllByDrugCodes(listOf("D001"))).willReturn(emptyList())

        val response = service.analyze(
            socialId = "kakao-123",
            request = PrescriptionAnalyzeRequest(listOf(MedicationAnalyzeRequest("D001"))),
        )

        assertEquals(MedicationSafetyStatus.SAFE, response.analyzedMedications.single().status)
        assertTrue(response.analyzedMedications.single().warnings.none { it.type == MedicationWarningType.DUR_PREGNANCY })
        verifyNoInteractions(durPregnancyRepository)
    }

    @Test
    fun `이름이 같아도 다른 코드의 약에는 병용금기를 전파하지 않는다`() {
        val medications = listOf(
            MedicationAnalyzeRequest(drugCode = "D001"),
            MedicationAnalyzeRequest(drugCode = "D002"),
            MedicationAnalyzeRequest(drugCode = "D003"),
        )
        val firstSameNameDrug = DrugMaster(
            drugCode = "D001",
            drugName = "Same Drug",
            atcCode = "A01AA01",
        )
        val otherDrug = DrugMaster(
            drugCode = "D002",
            drugName = "Other Drug",
            atcCode = "B01BB01",
        )
        val secondSameNameDrug = DrugMaster(
            drugCode = "D003",
            drugName = "Same Drug",
            atcCode = "A01AA02",
        )
        val drugCodes = listOf("D001", "D002", "D003")
        val interaction = DurInteraction(
            drugCodeA = "D001", drugCodeB = "D002", runtimeVerified = true,
            drugNameA = "Same Drug",
            drugNameB = "Other Drug",
            noticeNumber = "DUR-002",
            noticeDate = LocalDate.of(2026, 1, 1),
            warningMessage = "Duplicate name interaction.",
        )

        given(userRepository.findBySocialId("kakao-123")).willReturn(user)
        given(userAllergyRepository.findAllByUser_IdOrderByCreatedAtAsc(1L)).willReturn(emptyList())
        given(userHealthProfileRepository.findById(1L)).willReturn(Optional.of(UserHealthProfile(userId = 1L, user = user, birthDate = LocalDate.of(1990, 1, 1))))
        given(drugMasterRepository.findAllById(listOf("D001", "D002", "D003")))
            .willReturn(listOf(firstSameNameDrug, otherDrug, secondSameNameDrug))
        given(drugIngredientMapRepository.findAllByDrugCodes(listOf("D001", "D002", "D003"))).willReturn(emptyList())
        given(durInteractionRepository.findInteractions(drugCodes)).willReturn(listOf(interaction))

        val response = service.analyze(
            socialId = "kakao-123",
            request = PrescriptionAnalyzeRequest(medications = medications),
        )

        assertEquals(1, response.safetySummary.safeCount)
        assertEquals(0, response.safetySummary.warningCount)
        assertEquals(2, response.safetySummary.dangerCount)
        assertEquals(
            1,
            response.analyzedMedications.count {
                it.drugName == "Same Drug" &&
                    it.status == MedicationSafetyStatus.DANGER &&
                    it.warnings.any { warning -> warning.type == MedicationWarningType.DUR_INTERACTION }
            },
        )
    }

    @Test
    fun `약품코드 대소문자`() {
        val medications = listOf(MedicationAnalyzeRequest(drugCode = "d001"))
        val drug = DrugMaster(
            drugCode = "D001",
            drugName = "Tylenol 500mg",
            atcCode = "N02BE01",
        )

        given(userRepository.findBySocialId("kakao-123")).willReturn(user)
        given(userAllergyRepository.findAllByUser_IdOrderByCreatedAtAsc(1L)).willReturn(emptyList())
        given(userHealthProfileRepository.findById(1L)).willReturn(Optional.of(UserHealthProfile(userId = 1L, user = user, birthDate = LocalDate.of(1990, 1, 1))))
        given(drugMasterRepository.findAllById(listOf("d001"))).willReturn(listOf(drug))
        given(drugIngredientMapRepository.findAllByDrugCodes(listOf("d001"))).willReturn(emptyList())
        given(durInteractionRepository.findInteractions(listOf("D001"))).willReturn(emptyList())

        val response = service.analyze(
            socialId = "kakao-123",
            request = PrescriptionAnalyzeRequest(medications = medications),
        )

        assertEquals(1, response.safetySummary.safeCount)
        assertEquals("N02BE01", response.analyzedMedications.single().atcCode)
        assertEquals("Tylenol 500mg", response.analyzedMedications.single().drugName)
        verifyNoInteractions(notificationCreateService)
    }

    @Test
    fun `빈 ATC 알러지 무시`() {
        val medications = listOf(MedicationAnalyzeRequest(drugCode = "D001"))
        val drug = DrugMaster(
            drugCode = "D001",
            drugName = "Tylenol 500mg",
            atcCode = "N02BE01",
        )
        val blankAtcAllergy = UserAllergy(
            user = user,
            allergyType = AllergyType.ATC_GROUP,
            allergyValue = " ",
            allergyName = "Invalid ATC allergy",
        )

        given(userRepository.findBySocialId("kakao-123")).willReturn(user)
        given(userAllergyRepository.findAllByUser_IdOrderByCreatedAtAsc(1L)).willReturn(listOf(blankAtcAllergy))
        given(userHealthProfileRepository.findById(1L)).willReturn(Optional.of(UserHealthProfile(userId = 1L, user = user, birthDate = LocalDate.of(1990, 1, 1))))
        given(drugMasterRepository.findAllById(listOf("D001"))).willReturn(listOf(drug))
        given(drugIngredientMapRepository.findAllByDrugCodes(listOf("D001"))).willReturn(emptyList())
        given(durInteractionRepository.findInteractions(listOf("D001"))).willReturn(emptyList())

        val response = service.analyze(
            socialId = "kakao-123",
            request = PrescriptionAnalyzeRequest(medications = medications),
        )

        assertEquals(1, response.safetySummary.safeCount)
        assertEquals(MedicationSafetyStatus.SAFE, response.analyzedMedications.single().status)
    }
}
