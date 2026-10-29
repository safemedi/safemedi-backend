package com.safemedi.app.sefemedi.domain.medication.service

import com.safemedi.app.sefemedi.domain.drug.entity.DrugIngredientMap
import com.safemedi.app.sefemedi.domain.drug.entity.DrugMaster
import com.safemedi.app.sefemedi.domain.drug.entity.DurInteraction
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
        val drugNames = listOf("Tylenol 500mg", "Amlodipine 5mg")
        val interaction = DurInteraction(
            drugNameA = "Tylenol 500mg",
            drugNameB = "Amlodipine 5mg",
            noticeNumber = "DUR-001",
            noticeDate = LocalDate.of(2026, 1, 1),
            warningMessage = "Contraindicated interaction.",
        )

        given(userRepository.findBySocialId("kakao-123")).willReturn(user)
        given(userAllergyRepository.findAllByUser_IdOrderByCreatedAtAsc(1L)).willReturn(allergies)
        given(userHealthProfileRepository.findById(1L)).willReturn(Optional.empty())
        given(drugMasterRepository.findAllById(listOf("D001", "D002"))).willReturn(listOf(tylenolDrug, amlodipineDrug))
        given(drugIngredientMapRepository.findAllByDrugCodes(listOf("D001", "D002"))).willReturn(listOf(ingredientMap))
        given(durAgeRepository.findByDrugNameIn(drugNames)).willReturn(emptyList())
        given(durPregnancyRepository.findByDrugNameIn(drugNames)).willReturn(emptyList())
        given(durInteractionRepository.findInteractions(drugNames)).willReturn(listOf(interaction))

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
    fun `중복약물 경고`() {
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
        val drugNames = listOf("Same Drug", "Other Drug", "Same Drug")
        val interaction = DurInteraction(
            drugNameA = "Same Drug",
            drugNameB = "Other Drug",
            noticeNumber = "DUR-002",
            noticeDate = LocalDate.of(2026, 1, 1),
            warningMessage = "Duplicate name interaction.",
        )

        given(userRepository.findBySocialId("kakao-123")).willReturn(user)
        given(userAllergyRepository.findAllByUser_IdOrderByCreatedAtAsc(1L)).willReturn(emptyList())
        given(userHealthProfileRepository.findById(1L)).willReturn(Optional.empty())
        given(drugMasterRepository.findAllById(listOf("D001", "D002", "D003")))
            .willReturn(listOf(firstSameNameDrug, otherDrug, secondSameNameDrug))
        given(drugIngredientMapRepository.findAllByDrugCodes(listOf("D001", "D002", "D003"))).willReturn(emptyList())
        given(durInteractionRepository.findInteractions(drugNames)).willReturn(listOf(interaction))

        val response = service.analyze(
            socialId = "kakao-123",
            request = PrescriptionAnalyzeRequest(medications = medications),
        )

        assertEquals(0, response.safetySummary.safeCount)
        assertEquals(0, response.safetySummary.warningCount)
        assertEquals(3, response.safetySummary.dangerCount)
        assertEquals(
            2,
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
        given(userHealthProfileRepository.findById(1L)).willReturn(Optional.empty())
        given(drugMasterRepository.findAllById(listOf("d001"))).willReturn(listOf(drug))
        given(drugIngredientMapRepository.findAllByDrugCodes(listOf("d001"))).willReturn(emptyList())
        given(durInteractionRepository.findInteractions(listOf("Tylenol 500mg"))).willReturn(emptyList())

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
        given(userHealthProfileRepository.findById(1L)).willReturn(Optional.empty())
        given(drugMasterRepository.findAllById(listOf("D001"))).willReturn(listOf(drug))
        given(drugIngredientMapRepository.findAllByDrugCodes(listOf("D001"))).willReturn(emptyList())
        given(durInteractionRepository.findInteractions(listOf("Tylenol 500mg"))).willReturn(emptyList())

        val response = service.analyze(
            socialId = "kakao-123",
            request = PrescriptionAnalyzeRequest(medications = medications),
        )

        assertEquals(1, response.safetySummary.safeCount)
        assertEquals(MedicationSafetyStatus.SAFE, response.analyzedMedications.single().status)
    }
}
