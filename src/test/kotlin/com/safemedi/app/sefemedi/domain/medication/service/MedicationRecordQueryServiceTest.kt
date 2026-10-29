package com.safemedi.app.sefemedi.domain.medication.service

import com.safemedi.app.sefemedi.domain.family.entity.Family
import com.safemedi.app.sefemedi.domain.family.repository.FamilyRepository
import com.safemedi.app.sefemedi.domain.medication.entity.MedicationRecord
import com.safemedi.app.sefemedi.domain.medication.entity.MedicationStatus
import com.safemedi.app.sefemedi.domain.medication.entity.Prescription
import com.safemedi.app.sefemedi.domain.medication.entity.PrescriptionDrug
import com.safemedi.app.sefemedi.domain.medication.entity.PrescriptionDrugTime
import com.safemedi.app.sefemedi.domain.medication.repository.MedicationRecordRepository
import com.safemedi.app.sefemedi.domain.user.entity.User
import com.safemedi.app.sefemedi.domain.user.repository.UserRepository
import com.safemedi.app.sefemedi.global.error.BusinessException
import com.safemedi.app.sefemedi.global.error.ErrorCode
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class MedicationRecordQueryServiceTest {
    private lateinit var userRepository: UserRepository
    private lateinit var familyRepository: FamilyRepository
    private lateinit var medicationRecordRepository: MedicationRecordRepository
    private lateinit var medicationRecordQueryService: MedicationRecordQueryService

    private val user = User(
        id = 1L,
        socialId = "kakao-123",
    )

    @BeforeEach
    fun setUp() {
        userRepository = mock(UserRepository::class.java)
        familyRepository = mock(FamilyRepository::class.java)
        medicationRecordRepository = mock(MedicationRecordRepository::class.java)
        medicationRecordQueryService = MedicationRecordQueryService(
            userRepository = userRepository,
            familyRepository = familyRepository,
            medicationRecordRepository = medicationRecordRepository,
        )
    }

    @Test
    fun `findRecords returns daily medication records`() {
        val date = LocalDate.of(2026, 5, 12)
        val records = listOf(
            medicationRecord(
                id = 500L,
                prescriptionId = 25L,
                user = user,
                title = "Blood pressure",
                drugName = "Norvasc",
                scheduledAt = date.atTime(8, 0),
                takenAt = date.atTime(8, 5),
                status = MedicationStatus.SUCCESS,
            ),
            medicationRecord(
                id = 501L,
                prescriptionId = 25L,
                user = user,
                title = "Blood pressure",
                drugName = "Aspirin",
                scheduledAt = date.atTime(8, 0),
                takenAt = date.atTime(8, 6),
                status = MedicationStatus.SUCCESS,
            ),
            medicationRecord(
                id = 502L,
                prescriptionId = 26L,
                user = user,
                title = "Diabetes",
                drugName = "Diabex",
                scheduledAt = date.atTime(20, 0),
                takenAt = null,
                status = MedicationStatus.PENDING,
            ),
        )

        given(userRepository.findBySocialId("kakao-123")).willReturn(user)
        given(
            medicationRecordRepository.findRecordsForPeriod(
                userId = 1L,
                startAt = date.atStartOfDay(),
                endAt = date.plusDays(1).atStartOfDay(),
            )
        ).willReturn(records)

        val response = medicationRecordQueryService.findRecords(
            socialId = "kakao-123",
            type = "DAILY",
            date = "2026-05-12",
            familyId = null,
        )

        assertEquals(LocalDate.of(2026, 5, 12), response.date)
        assertEquals(3, response.summary.totalCount)
        assertEquals(2, response.summary.takenCount)
        assertEquals("2/3", response.summary.fraction)
        assertEquals(2, response.records?.size)
        assertEquals(listOf(500L, 501L), response.records?.first()?.recordIds)
        assertEquals(listOf("Norvasc", "Aspirin"), response.records?.first()?.medicationNames)
        assertEquals("08:05", response.records?.first()?.takenTime)
        assertEquals(null, response.dailyRecords)
    }

    @Test
    fun `findRecords returns monthly medication records by date`() {
        val record = medicationRecord(
            id = 500L,
            prescriptionId = 25L,
            user = user,
            title = "Blood pressure",
            drugName = "Norvasc",
            scheduledAt = LocalDateTime.of(2026, 5, 12, 8, 0),
            takenAt = LocalDateTime.of(2026, 5, 12, 8, 5),
            status = MedicationStatus.SUCCESS,
        )

        given(userRepository.findBySocialId("kakao-123")).willReturn(user)
        given(
            medicationRecordRepository.findRecordsForPeriod(
                userId = 1L,
                startAt = LocalDateTime.of(2026, 5, 1, 0, 0),
                endAt = LocalDateTime.of(2026, 6, 1, 0, 0),
            )
        ).willReturn(listOf(record))

        val response = medicationRecordQueryService.findRecords(
            socialId = "kakao-123",
            type = "MONTH",
            date = "2026-05-12",
            familyId = null,
        )

        assertEquals(LocalDate.of(2026, 5, 1), response.periodStartDate)
        assertEquals(LocalDate.of(2026, 5, 31), response.periodEndDate)
        assertEquals(1, response.summary.totalCount)
        assertEquals("1/1", response.dailyRecords?.single()?.fraction)
        assertEquals(listOf(500L), response.dailyRecords?.single()?.items?.single()?.recordIds)
        assertEquals(null, response.records)
    }

    @Test
    fun `findRecords returns family medication records`() {
        val connectedUser = User(
            id = 2L,
            socialId = "family-user",
        )
        val family = Family(
            id = 10L,
            user = user,
            connectedUser = connectedUser,
            relation = "mother",
            isAllowMyInfo = false,
        )
        val date = LocalDate.of(2026, 5, 12)

        given(userRepository.findBySocialId("kakao-123")).willReturn(user)
        given(familyRepository.findByIdAndUser_Id(10L, 1L)).willReturn(family)
        given(familyRepository.findByUser_IdAndConnectedUser_Id(2L, 1L)).willReturn(
            Family(user = family.connectedUser, connectedUser = user, relation = "가족", isAllowMyInfo = true),
        )
        given(
            medicationRecordRepository.findRecordsForPeriod(
                userId = 2L,
                startAt = date.atStartOfDay(),
                endAt = date.plusDays(1).atStartOfDay(),
            )
        ).willReturn(emptyList())

        val response = medicationRecordQueryService.findRecords(
            socialId = "kakao-123",
            type = "DAILY",
            date = "2026-05-12",
            familyId = 10L,
        )

        assertEquals(10L, response.familyId)
        assertEquals("mother", response.relation)
    }

    @Test
    fun `findRecords throws when type is invalid`() {
        given(userRepository.findBySocialId("kakao-123")).willReturn(user)

        val exception = assertFailsWith<BusinessException> {
            medicationRecordQueryService.findRecords(
                socialId = "kakao-123",
                type = "YEAR",
                date = "2026-05-12",
                familyId = null,
            )
        }

        assertEquals(ErrorCode.INVALID_ENUM_VALUE, exception.errorCode)
    }

    @Test
    fun `findRecords throws when type is missing`() {
        given(userRepository.findBySocialId("kakao-123")).willReturn(user)

        val exception = assertFailsWith<BusinessException> {
            medicationRecordQueryService.findRecords(
                socialId = "kakao-123",
                type = null,
                date = "2026-05-12",
                familyId = null,
            )
        }

        assertEquals(ErrorCode.MEDICATION_RECORD_TYPE_REQUIRED, exception.errorCode)
    }

    @Test
    fun `findRecords throws when family is not connected`() {
        given(userRepository.findBySocialId("kakao-123")).willReturn(user)
        given(familyRepository.findByIdAndUser_Id(99L, 1L)).willReturn(null)

        val exception = assertFailsWith<BusinessException> {
            medicationRecordQueryService.findRecords(
                socialId = "kakao-123",
                type = "DAILY",
                date = "2026-05-12",
                familyId = 99L,
            )
        }

        assertEquals(ErrorCode.FAMILY_ACCESS_DENIED, exception.errorCode)
    }

    @Test
    fun `요청자가 공개해도 대상이 공유를 거부하면 조회를 차단한다`() {
        val target = User(id = 2L, socialId = "family-user")
        val family = Family(
            id = 10L, user = user, connectedUser = target, relation = "가족", isAllowMyInfo = true,
        )
        given(userRepository.findBySocialId("kakao-123")).willReturn(user)
        given(familyRepository.findByIdAndUser_Id(10L, 1L)).willReturn(family)
        given(familyRepository.findByUser_IdAndConnectedUser_Id(2L, 1L)).willReturn(Family(user = target, connectedUser = user, relation = "가족", isAllowMyInfo = false))

        val exception = kotlin.test.assertFailsWith<BusinessException> {
            medicationRecordQueryService.findRecords("kakao-123", "DAILY", "2026-05-12", 10L)
        }

        assertEquals(ErrorCode.FAMILY_ACCESS_DENIED, exception.errorCode)
        org.mockito.Mockito.verifyNoInteractions(medicationRecordRepository)
    }

    @Test
    fun `역방향 가족 연결이 없으면 조회를 차단한다`() {
        val target = User(id = 2L, socialId = "family-user")
        val family = Family(
            id = 10L, user = user, connectedUser = target, relation = "가족", isAllowMyInfo = true,
        )
        given(userRepository.findBySocialId("kakao-123")).willReturn(user)
        given(familyRepository.findByIdAndUser_Id(10L, 1L)).willReturn(family)
        given(familyRepository.findByUser_IdAndConnectedUser_Id(2L, 1L)).willReturn(null)

        val exception = kotlin.test.assertFailsWith<BusinessException> {
            medicationRecordQueryService.findRecords("kakao-123", "DAILY", "2026-05-12", 10L)
        }

        assertEquals(ErrorCode.FAMILY_ACCESS_DENIED, exception.errorCode)
        org.mockito.Mockito.verifyNoInteractions(medicationRecordRepository)
    }

    private fun medicationRecord(
        id: Long,
        prescriptionId: Long = id,
        user: User,
        title: String,
        drugName: String,
        scheduledAt: LocalDateTime,
        takenAt: LocalDateTime?,
        status: MedicationStatus,
    ): MedicationRecord {
        val prescription = Prescription(
            id = prescriptionId,
            user = user,
            title = title,
            startDate = scheduledAt.toLocalDate(),
            endDate = scheduledAt.toLocalDate(),
        )
        val prescriptionDrug = PrescriptionDrug(
            id = id,
            prescription = prescription,
            drugName = drugName,
        )
        val prescriptionDrugTime = PrescriptionDrugTime(
            id = id,
            prescriptionDrug = prescriptionDrug,
            takeTime = LocalTime.from(scheduledAt),
        )

        return MedicationRecord(
            id = id,
            user = user,
            prescription = prescription,
            prescriptionDrugTime = prescriptionDrugTime,
            scheduledAt = scheduledAt,
            takenAt = takenAt,
            status = status,
        )
    }
}
