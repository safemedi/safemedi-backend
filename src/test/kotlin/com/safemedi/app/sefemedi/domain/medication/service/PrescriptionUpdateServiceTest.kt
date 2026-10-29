package com.safemedi.app.sefemedi.domain.medication.service

import com.safemedi.app.sefemedi.domain.medication.dto.PrescriptionMedicationUpdateRequest
import com.safemedi.app.sefemedi.domain.medication.dto.PrescriptionUpdateRequest
import com.safemedi.app.sefemedi.domain.medication.entity.MedicationRecord
import com.safemedi.app.sefemedi.domain.medication.entity.MedicationStatus
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
import org.mockito.ArgumentMatchers.anyList
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

@Suppress("UNCHECKED_CAST")
class PrescriptionUpdateServiceTest {
    private lateinit var userRepository: UserRepository
    private lateinit var prescriptionRepository: PrescriptionRepository
    private lateinit var prescriptionDrugRepository: PrescriptionDrugRepository
    private lateinit var prescriptionDrugTimeRepository: PrescriptionDrugTimeRepository
    private lateinit var medicationRecordRepository: MedicationRecordRepository
    private lateinit var service: PrescriptionUpdateService

    private val user = User(
        id = 1L,
        socialId = "kakao-123",
    )

    @BeforeEach
    fun setUp() {
        userRepository = mock(UserRepository::class.java)
        prescriptionRepository = mock(PrescriptionRepository::class.java)
        prescriptionDrugRepository = mock(PrescriptionDrugRepository::class.java)
        prescriptionDrugTimeRepository = mock(PrescriptionDrugTimeRepository::class.java)
        medicationRecordRepository = mock(MedicationRecordRepository::class.java)

        service = PrescriptionUpdateService(
            userRepository = userRepository,
            prescriptionRepository = prescriptionRepository,
            prescriptionDrugRepository = prescriptionDrugRepository,
            prescriptionDrugTimeRepository = prescriptionDrugTimeRepository,
            medicationRecordRepository = medicationRecordRepository,
        )
    }

    @Test
    fun `처방전 제목과 복용 시간을 수정하고 미래 복약 기록을 재생성한다`() {
        val tomorrow = LocalDate.now(SERVICE_ZONE_ID).plusDays(1)
        val prescription = Prescription(
            id = 10L,
            user = user,
            title = "Old title",
            startDate = tomorrow,
            endDate = tomorrow.plusDays(1),
        )
        val prescriptionDrug = PrescriptionDrug(
            id = 1L,
            prescription = prescription,
            drugName = "Tylenol",
        )
        val oldTime = PrescriptionDrugTime(
            id = 1L,
            prescriptionDrug = prescriptionDrug,
            takeTime = LocalTime.of(8, 0),
        )
        var savedRecordCount = 0

        given(userRepository.findBySocialId("kakao-123")).willReturn(user)
        given(prescriptionRepository.findByIdAndDeletedAtIsNull(10L)).willReturn(prescription)
        given(prescriptionDrugRepository.findByPrescriptionIdAndIds(10L, listOf(1L)))
            .willReturn(listOf(prescriptionDrug))
        given(prescriptionDrugTimeRepository.findByPrescriptionDrugIds(listOf(1L)))
            .willReturn(listOf(oldTime))
        given(prescriptionDrugTimeRepository.saveAll(anyList<PrescriptionDrugTime>())).willAnswer {
            it.arguments[0] as List<PrescriptionDrugTime>
        }
        given(medicationRecordRepository.saveAll(anyList<MedicationRecord>())).willAnswer {
            val records = it.arguments[0] as List<MedicationRecord>
            savedRecordCount = records.size
            records
        }

        val response = service.update(
            socialId = "kakao-123",
            prescriptionId = 10L,
            request = PrescriptionUpdateRequest(
                title = "New title",
                medications = listOf(
                    PrescriptionMedicationUpdateRequest(
                        prescriptionDrugId = 1L,
                        takeTimes = listOf("09:00", "20:00"),
                    )
                ),
            ),
        )

        assertEquals(10L, response.prescriptionId)
        assertEquals("New title", response.title)
        assertEquals("New title", prescription.title)
        assertNotNull(oldTime.deletedAt)
        assertEquals(4, savedRecordCount)
        verify(medicationRecordRepository).deleteFuturePendingByPrescriptionIdAndPrescriptionDrugIds(
            prescriptionId = 10L,
            prescriptionDrugIds = listOf(1L),
            status = MedicationStatus.PENDING,
            now = oldTime.deletedAt ?: throw AssertionError("deletedAt should be set"),
        )
    }

    @Test
    fun `처방 시작일이 과거이면 오늘부터 미래 복약 기록을 재생성한다`() {
        val today = LocalDate.now(SERVICE_ZONE_ID)
        val prescription = Prescription(
            id = 10L,
            user = user,
            title = "Old title",
            startDate = today.minusDays(30),
            endDate = today.plusDays(1),
        )
        val prescriptionDrug = PrescriptionDrug(
            id = 1L,
            prescription = prescription,
            drugName = "Tylenol",
        )
        val oldTime = PrescriptionDrugTime(
            id = 1L,
            prescriptionDrug = prescriptionDrug,
            takeTime = LocalTime.of(8, 0),
        )
        var savedRecords = emptyList<MedicationRecord>()

        given(userRepository.findBySocialId("kakao-123")).willReturn(user)
        given(prescriptionRepository.findByIdAndDeletedAtIsNull(10L)).willReturn(prescription)
        given(prescriptionDrugRepository.findByPrescriptionIdAndIds(10L, listOf(1L)))
            .willReturn(listOf(prescriptionDrug))
        given(prescriptionDrugTimeRepository.findByPrescriptionDrugIds(listOf(1L)))
            .willReturn(listOf(oldTime))
        given(prescriptionDrugTimeRepository.saveAll(anyList<PrescriptionDrugTime>())).willAnswer {
            it.arguments[0] as List<PrescriptionDrugTime>
        }
        given(medicationRecordRepository.saveAll(anyList<MedicationRecord>())).willAnswer {
            val records = it.arguments[0] as List<MedicationRecord>
            savedRecords = records
            records
        }

        service.update(
            socialId = "kakao-123",
            prescriptionId = 10L,
            request = PrescriptionUpdateRequest(
                medications = listOf(
                    PrescriptionMedicationUpdateRequest(
                        prescriptionDrugId = 1L,
                        takeTimes = listOf("23:59"),
                    )
                ),
            ),
        )

        assertEquals(2, savedRecords.size)
        assertEquals(listOf(today, today.plusDays(1)), savedRecords.map { it.scheduledAt.toLocalDate() })
    }

    @Test
    fun `종료된 처방전의 복용 시간은 수정할 수 없다`() {
        val prescription = Prescription(
            id = 10L,
            user = user,
            title = "Old title",
            startDate = LocalDate.now(SERVICE_ZONE_ID).minusDays(3),
            endDate = LocalDate.now(SERVICE_ZONE_ID).minusDays(1),
        )

        given(userRepository.findBySocialId("kakao-123")).willReturn(user)
        given(prescriptionRepository.findByIdAndDeletedAtIsNull(10L)).willReturn(prescription)

        val exception = assertFailsWith<BusinessException> {
            service.update(
                socialId = "kakao-123",
                prescriptionId = 10L,
                request = PrescriptionUpdateRequest(
                    medications = listOf(
                        PrescriptionMedicationUpdateRequest(
                            prescriptionDrugId = 1L,
                            takeTimes = listOf("09:00"),
                        )
                    ),
                ),
            )
        }

        assertEquals(ErrorCode.ENDED_PRESCRIPTION_UPDATE_NOT_ALLOWED, exception.errorCode)
    }

    @Test
    fun `처방전 제목 길이 오류`() {
        val prescription = Prescription(
            id = 10L,
            user = user,
            title = "Old title",
            startDate = LocalDate.now(SERVICE_ZONE_ID),
            endDate = LocalDate.now(SERVICE_ZONE_ID).plusDays(1),
        )

        given(userRepository.findBySocialId("kakao-123")).willReturn(user)
        given(prescriptionRepository.findByIdAndDeletedAtIsNull(10L)).willReturn(prescription)

        val exception = assertFailsWith<BusinessException> {
            service.update(
                socialId = "kakao-123",
                prescriptionId = 10L,
                request = PrescriptionUpdateRequest(
                    title = "a".repeat(256),
                ),
            )
        }

        assertEquals(ErrorCode.INVALID_REQUEST, exception.errorCode)
    }

    @Test
    fun `처방전에 없는 약물이면 오류`() {
        val tomorrow = LocalDate.now(SERVICE_ZONE_ID).plusDays(1)
        val prescription = Prescription(
            id = 10L,
            user = user,
            title = "Old title",
            startDate = tomorrow,
            endDate = tomorrow.plusDays(1),
        )

        given(userRepository.findBySocialId("kakao-123")).willReturn(user)
        given(prescriptionRepository.findByIdAndDeletedAtIsNull(10L)).willReturn(prescription)
        given(prescriptionDrugRepository.findByPrescriptionIdAndIds(10L, listOf(999L)))
            .willReturn(emptyList())

        val exception = assertFailsWith<BusinessException> {
            service.update(
                socialId = "kakao-123",
                prescriptionId = 10L,
                request = PrescriptionUpdateRequest(
                    medications = listOf(
                        PrescriptionMedicationUpdateRequest(
                            prescriptionDrugId = 999L,
                            takeTimes = listOf("09:00"),
                        )
                    ),
                ),
            )
        }

        assertEquals(ErrorCode.PRESCRIPTION_DRUG_NOT_FOUND, exception.errorCode)
    }

    private companion object {
        val SERVICE_ZONE_ID: ZoneId = ZoneId.of("Asia/Seoul")
    }
}
