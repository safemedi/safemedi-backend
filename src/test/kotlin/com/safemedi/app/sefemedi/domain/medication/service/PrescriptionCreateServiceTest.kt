package com.safemedi.app.sefemedi.domain.medication.service

import com.safemedi.app.sefemedi.domain.drug.entity.DrugMaster
import com.safemedi.app.sefemedi.domain.drug.repository.DrugMasterRepository
import com.safemedi.app.sefemedi.domain.medication.dto.PrescriptionCreateRequest
import com.safemedi.app.sefemedi.domain.medication.dto.PrescriptionMedicationRequest
import com.safemedi.app.sefemedi.domain.medication.entity.MedicationRecord
import com.safemedi.app.sefemedi.domain.medication.entity.Prescription
import com.safemedi.app.sefemedi.domain.medication.entity.PrescriptionDrug
import com.safemedi.app.sefemedi.domain.medication.entity.PrescriptionDrugTime
import com.safemedi.app.sefemedi.domain.medication.repository.MedicationRecordRepository
import com.safemedi.app.sefemedi.domain.medication.repository.PrescriptionDrugRepository
import com.safemedi.app.sefemedi.domain.medication.repository.PrescriptionDrugTimeRepository
import com.safemedi.app.sefemedi.domain.medication.repository.PrescriptionRepository
import com.safemedi.app.sefemedi.domain.user.entity.User
import com.safemedi.app.sefemedi.domain.user.repository.UserRepository
import com.safemedi.app.sefemedi.global.error.BusinessException
import com.safemedi.app.sefemedi.global.error.ErrorCode
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyList
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@Suppress("UNCHECKED_CAST")
class PrescriptionCreateServiceTest {
    private lateinit var userRepository: UserRepository
    private lateinit var drugMasterRepository: DrugMasterRepository
    private lateinit var prescriptionRepository: PrescriptionRepository
    private lateinit var prescriptionDrugRepository: PrescriptionDrugRepository
    private lateinit var prescriptionDrugTimeRepository: PrescriptionDrugTimeRepository
    private lateinit var medicationRecordRepository: MedicationRecordRepository
    private lateinit var service: PrescriptionCreateService

    private val user = User(
        id = 1L,
        socialId = "kakao-123",
    )

    @BeforeEach
    fun setUp() {
        userRepository = mock(UserRepository::class.java)
        drugMasterRepository = mock(DrugMasterRepository::class.java)
        prescriptionRepository = mock(PrescriptionRepository::class.java)
        prescriptionDrugRepository = mock(PrescriptionDrugRepository::class.java)
        prescriptionDrugTimeRepository = mock(PrescriptionDrugTimeRepository::class.java)
        medicationRecordRepository = mock(MedicationRecordRepository::class.java)

        service = PrescriptionCreateService(
            userRepository = userRepository,
            drugMasterRepository = drugMasterRepository,
            prescriptionRepository = prescriptionRepository,
            prescriptionDrugRepository = prescriptionDrugRepository,
            prescriptionDrugTimeRepository = prescriptionDrugTimeRepository,
            medicationRecordRepository = medicationRecordRepository,
        )
    }

    @Test
    fun `날짜 오류`() {
        val exception = assertFailsWith<BusinessException> {
            service.create(
                socialId = "kakao-123",
                request = validRequest().copy(
                    startDate = LocalDate.of(2026, 5, 20),
                    endDate = LocalDate.of(2026, 5, 13),
                ),
            )
        }

        assertEquals(ErrorCode.INVALID_PRESCRIPTION_DATE, exception.errorCode)
    }

    @Test
    fun `기간 제한`() {
        val exception = assertFailsWith<BusinessException> {
            service.create(
                socialId = "kakao-123",
                request = validRequest().copy(
                    startDate = LocalDate.of(2026, 1, 1),
                    endDate = LocalDate.of(2026, 7, 2),
                ),
            )
        }

        assertEquals(ErrorCode.INVALID_PRESCRIPTION_DATE, exception.errorCode)
    }

    @Test
    fun `복용시간 오류`() {
        val exception = assertFailsWith<BusinessException> {
            service.create(
                socialId = "kakao-123",
                request = validRequest().copy(
                    medications = listOf(
                        PrescriptionMedicationRequest(
                            drugCode = "D001",
                            atcCode = "N02BE01",
                            drugName = "Tylenol 500mg",
                            takeTimes = listOf("8:00"),
                        )
                    )
                ),
            )
        }

        assertEquals(ErrorCode.INVALID_TAKE_TIMES, exception.errorCode)
    }

    @Test
    fun `처방전 제목 길이 오류`() {
        val exception = assertFailsWith<BusinessException> {
            service.create(
                socialId = "kakao-123",
                request = validRequest().copy(
                    title = "a".repeat(256),
                ),
            )
        }

        assertEquals(ErrorCode.INVALID_REQUEST, exception.errorCode)
    }

    @Test
    fun `처방전 저장`() {
        var medicationRecordCount = 0
        var capturedPrescriptionDrug: PrescriptionDrug? = null
        val drug = DrugMaster(
            drugCode = "D001",
            drugName = "Tylenol 500mg",
            atcCode = "N02BE01",
        )

        val savedPrescription = Prescription(
            id = 25L,
            user = user,
            title = "Prescription",
            isDoctorApproved = false,
            startDate = LocalDate.of(2026, 5, 13),
            endDate = LocalDate.of(2026, 5, 14),
        )
        val savedPrescriptionDrug = PrescriptionDrug(
            id = 1L,
            prescription = savedPrescription,
            drugName = "Tylenol 500mg",
            atcCode = "N02BE01",
        )
        val savedPrescriptionDrugTimes = listOf(
            PrescriptionDrugTime(
                id = 1L,
                prescriptionDrug = savedPrescriptionDrug,
                takeTime = java.time.LocalTime.of(8, 0),
            ),
            PrescriptionDrugTime(
                id = 2L,
                prescriptionDrug = savedPrescriptionDrug,
                takeTime = java.time.LocalTime.of(18, 0),
            ),
        )

        given(userRepository.findBySocialId("kakao-123")).willReturn(user)
        given(drugMasterRepository.findAllById(listOf("D001"))).willReturn(listOf(drug))
        given(prescriptionRepository.save(any(Prescription::class.java))).willReturn(savedPrescription)
        given(prescriptionDrugRepository.saveAll(anyList<PrescriptionDrug>())).willAnswer {
            val prescriptionDrugs = it.arguments[0] as List<PrescriptionDrug>
            capturedPrescriptionDrug = prescriptionDrugs.single()
            listOf(savedPrescriptionDrug)
        }
        given(prescriptionDrugTimeRepository.saveAll(anyList<PrescriptionDrugTime>())).willReturn(savedPrescriptionDrugTimes)
        given(medicationRecordRepository.saveAll(anyList<MedicationRecord>())).willAnswer {
            val records = it.arguments[0] as List<MedicationRecord>
            medicationRecordCount = records.size
            records
        }

        val response = service.create(
            socialId = "kakao-123",
            request = validRequest().copy(
                startDate = LocalDate.of(2026, 5, 13),
                endDate = LocalDate.of(2026, 5, 14),
                isDoctorApproved = false,
                medications = listOf(
                    PrescriptionMedicationRequest(
                        drugCode = "D001",
                        atcCode = "N02BE01",
                        drugName = "Tylenol 500mg",
                        takeTimes = listOf("08:00", "18:00"),
                    )
                ),
            ),
        )

        assertEquals(25L, response.prescriptionId)
        assertEquals(4, medicationRecordCount)
        assertEquals("D001", capturedPrescriptionDrug?.drug?.drugCode)
    }

    private fun validRequest(): PrescriptionCreateRequest =
        PrescriptionCreateRequest(
            title = "Prescription",
            startDate = LocalDate.of(2026, 5, 13),
            endDate = LocalDate.of(2026, 5, 20),
            isDoctorApproved = true,
            medications = listOf(
                PrescriptionMedicationRequest(
                    drugCode = "D001",
                    atcCode = "N02BE01",
                    drugName = "Tylenol 500mg",
                    takeTimes = listOf("08:00"),
                )
            ),
        )
}
