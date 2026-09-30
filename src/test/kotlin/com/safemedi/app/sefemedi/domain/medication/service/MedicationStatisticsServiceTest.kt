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

class MedicationStatisticsServiceTest {

    private lateinit var userRepository: UserRepository
    private lateinit var familyRepository: FamilyRepository
    private lateinit var medicationRecordRepository: MedicationRecordRepository
    private lateinit var medicationStatisticsService: MedicationStatisticsService

    private val user = User(
        id = 1L,
        socialId = "kakao-123",
    )

    @BeforeEach
    fun setUp() {
        userRepository = mock(UserRepository::class.java)
        familyRepository = mock(FamilyRepository::class.java)
        medicationRecordRepository = mock(MedicationRecordRepository::class.java)
        medicationStatisticsService = MedicationStatisticsService(
            userRepository = userRepository,
            familyRepository = familyRepository,
            medicationRecordRepository = medicationRecordRepository,
        )
    }

    @Test
    fun `복약 통계는 전체 분수와 날짜별 분수를 반환한다`() {
        val records = listOf(
            medicationRecord(
                id = 1L,
                scheduledAt = LocalDateTime.of(2026, 4, 1, 8, 0),
                status = MedicationStatus.SUCCESS,
            ),
            medicationRecord(
                id = 2L,
                scheduledAt = LocalDateTime.of(2026, 4, 1, 20, 0),
                status = MedicationStatus.PENDING,
            ),
            medicationRecord(
                id = 3L,
                scheduledAt = LocalDateTime.of(2026, 4, 2, 8, 0),
                status = MedicationStatus.SUCCESS,
            ),
        )

        given(userRepository.findBySocialId("kakao-123")).willReturn(user)
        given(
            medicationRecordRepository.findStatisticsRecords(
                userId = 1L,
                startAt = LocalDateTime.of(2026, 4, 1, 0, 0),
                endAt = LocalDateTime.of(2026, 4, 8, 0, 0),
            )
        ).willReturn(records)

        val response = medicationStatisticsService.findStatistics(
            socialId = "kakao-123",
            startDate = "2026-04-01",
            endDate = "2026-04-07",
            familyId = null,
        )

        assertEquals(LocalDate.of(2026, 4, 1), response.startDate)
        assertEquals(LocalDate.of(2026, 4, 7), response.endDate)
        assertEquals(null, response.familyId)
        assertEquals(null, response.relation)
        assertEquals(3, response.totalCount)
        assertEquals(2, response.takenCount)
        assertEquals("2/3", response.fraction)
        assertEquals(2, response.dailyCompliance.size)
        assertEquals("1/2", response.dailyCompliance[0].fraction)
        assertEquals("1/1", response.dailyCompliance[1].fraction)
    }

    @Test
    fun `가족 통계는 연결된 가족의 사용자 기록을 조회한다`() {
        val connectedUser = User(
            id = 2L,
            socialId = "family-user",
        )
        val family = Family(
            id = 10L,
            user = user,
            connectedUser = connectedUser,
            relation = "어머니",
            isAllowMyInfo = false,
        )

        given(userRepository.findBySocialId("kakao-123")).willReturn(user)
        given(familyRepository.findByIdAndUser_Id(10L, 1L)).willReturn(family)
        given(familyRepository.findByUser_IdAndConnectedUser_Id(2L, 1L)).willReturn(
            Family(user = family.connectedUser, connectedUser = user, relation = "가족", isAllowMyInfo = true),
        )
        given(
            medicationRecordRepository.findStatisticsRecords(
                userId = 2L,
                startAt = LocalDateTime.of(2026, 4, 1, 0, 0),
                endAt = LocalDateTime.of(2026, 4, 8, 0, 0),
            )
        ).willReturn(emptyList())

        val response = medicationStatisticsService.findStatistics(
            socialId = "kakao-123",
            startDate = "2026-04-01",
            endDate = "2026-04-07",
            familyId = 10L,
        )

        assertEquals(10L, response.familyId)
        assertEquals("어머니", response.relation)
        assertEquals("0/0", response.fraction)
    }

    @Test
    fun `시작일이 종료일보다 늦으면 예외를 던진다`() {
        given(userRepository.findBySocialId("kakao-123")).willReturn(user)

        val exception = assertFailsWith<BusinessException> {
            medicationStatisticsService.findStatistics(
                socialId = "kakao-123",
                startDate = "2026-04-08",
                endDate = "2026-04-07",
                familyId = null,
            )
        }

        assertEquals(ErrorCode.STATISTICS_INVALID_DATE_RANGE, exception.errorCode)
    }

    @Test
    fun `연결되지 않은 가족 ID면 예외를 던진다`() {
        given(userRepository.findBySocialId("kakao-123")).willReturn(user)
        given(familyRepository.findByIdAndUser_Id(99L, 1L)).willReturn(null)

        val exception = assertFailsWith<BusinessException> {
            medicationStatisticsService.findStatistics(
                socialId = "kakao-123",
                startDate = "2026-04-01",
                endDate = "2026-04-07",
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
            medicationStatisticsService.findStatistics("kakao-123", "2026-04-01", "2026-04-07", 10L)
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
            medicationStatisticsService.findStatistics("kakao-123", "2026-04-01", "2026-04-07", 10L)
        }

        assertEquals(ErrorCode.FAMILY_ACCESS_DENIED, exception.errorCode)
        org.mockito.Mockito.verifyNoInteractions(medicationRecordRepository)
    }

    private fun medicationRecord(
        id: Long,
        scheduledAt: LocalDateTime,
        status: MedicationStatus,
    ): MedicationRecord {
        val prescription = Prescription(
            id = 1L,
            user = user,
            title = "테스트 처방전",
            startDate = scheduledAt.toLocalDate(),
            endDate = scheduledAt.toLocalDate(),
        )
        val prescriptionDrug = PrescriptionDrug(
            id = 1L,
            prescription = prescription,
            drugName = "테스트 약",
        )
        val prescriptionDrugTime = PrescriptionDrugTime(
            id = 1L,
            prescriptionDrug = prescriptionDrug,
            takeTime = LocalTime.of(8, 0),
        )

        return MedicationRecord(
            id = id,
            user = user,
            prescription = prescription,
            prescriptionDrugTime = prescriptionDrugTime,
            scheduledAt = scheduledAt,
            status = status,
        )
    }
}
