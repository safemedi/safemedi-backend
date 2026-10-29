package com.safemedi.app.sefemedi.domain.medication.service

import com.safemedi.app.sefemedi.domain.medication.dto.MedicationRecordUpdateRequest
import com.safemedi.app.sefemedi.domain.medication.entity.MedicationRecord
import com.safemedi.app.sefemedi.domain.medication.entity.MedicationStatus
import com.safemedi.app.sefemedi.domain.medication.entity.Prescription
import com.safemedi.app.sefemedi.domain.medication.entity.PrescriptionDrug
import com.safemedi.app.sefemedi.domain.medication.entity.PrescriptionDrugTime
import com.safemedi.app.sefemedi.domain.medication.repository.MedicationRecordRepository
import com.safemedi.app.sefemedi.domain.notification.dto.NotificationCreateCommand
import com.safemedi.app.sefemedi.domain.notification.entity.NotificationTargetType
import com.safemedi.app.sefemedi.domain.notification.entity.NotificationType
import com.safemedi.app.sefemedi.domain.notification.service.NotificationCreateService
import com.safemedi.app.sefemedi.domain.user.entity.User
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
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MedicationRecordUpdateServiceTest {
    private lateinit var userRepository: UserRepository
    private lateinit var medicationRecordRepository: MedicationRecordRepository
    private lateinit var notificationCreateService: NotificationCreateService
    private lateinit var service: MedicationRecordUpdateService

    private val user = User(
        id = 1L,
        socialId = "kakao-123",
    )
    private val otherUser = User(
        id = 2L,
        socialId = "kakao-456",
    )
    private val scheduledAt = LocalDateTime.of(2026, 4, 6, 8, 0)
    private val prescription = Prescription(
        id = 10L,
        user = user,
        title = "Prescription",
        startDate = LocalDate.of(2026, 4, 1),
        endDate = LocalDate.of(2026, 4, 7),
    )

    @BeforeEach
    fun setUp() {
        userRepository = mock(UserRepository::class.java)
        medicationRecordRepository = mock(MedicationRecordRepository::class.java)
        notificationCreateService = mock(NotificationCreateService::class.java)

        service = MedicationRecordUpdateService(
            userRepository = userRepository,
            medicationRecordRepository = medicationRecordRepository,
            notificationCreateService = notificationCreateService,
        )
    }

    @Test
    fun `같은 처방전 시간대의 복약 기록 그룹을 한번에 성공 처리하고 알림은 1건만 생성한다`() {
        val records = listOf(
            medicationRecord(id = 500L, recordUser = user, drugName = "Tylenol", status = MedicationStatus.PENDING),
            medicationRecord(id = 501L, recordUser = user, drugName = "Aspirin", status = MedicationStatus.PENDING),
        )

        given(userRepository.findBySocialId("kakao-123")).willReturn(user)
        given(medicationRecordRepository.findActiveAllByIdIn(setOf(500L, 501L))).willReturn(records)
        given(
            medicationRecordRepository.findAllByPrescriptionIdAndScheduledAtAndUserId(10L, scheduledAt, 1L)
        ).willReturn(records)

        val response = service.update(
            socialId = "kakao-123",
            request = MedicationRecordUpdateRequest(
                recordIds = listOf(500L, 501L),
                status = MedicationStatus.SUCCESS,
            ),
        )

        assertEquals(listOf(500L, 501L), response.recordIds)
        assertEquals(10L, response.prescriptionId)
        assertEquals(MedicationStatus.SUCCESS.name, response.status)
        assertEquals(listOf("Tylenol", "Aspirin"), response.drugNames)
        assertNotNull(response.takenAt)
        assertTrue(records.all { it.status == MedicationStatus.SUCCESS })
        assertTrue(records.all { it.takenAt != null })

        val command = mockingDetails(notificationCreateService)
            .invocations
            .single()
            .arguments[0] as NotificationCreateCommand
        assertEquals(1L, command.userId)
        assertEquals(NotificationType.MEDICATION_COMPLETED, command.type)
        assertEquals("복약 완료", command.title)
        assertEquals("Tylenol, Aspirin 복용을 완료했어요", command.content)
        assertEquals(NotificationTargetType.PRESCRIPTION, command.targetType)
        assertEquals(10L, command.targetId)
        assertEquals("MEDICATION_COMPLETED:PRESCRIPTION:10:$scheduledAt:1", command.deduplicationKey)
        assertNotNull(command.scheduledAt)
    }

    @Test
    fun `복약 기록 그룹을 건너뜀 처리하면 알림을 생성하지 않는다`() {
        val records = listOf(
            medicationRecord(id = 500L, recordUser = user, drugName = "Tylenol", status = MedicationStatus.PENDING),
        )

        given(userRepository.findBySocialId("kakao-123")).willReturn(user)
        given(medicationRecordRepository.findActiveAllByIdIn(setOf(500L))).willReturn(records)
        given(
            medicationRecordRepository.findAllByPrescriptionIdAndScheduledAtAndUserId(10L, scheduledAt, 1L)
        ).willReturn(records)

        val response = service.update(
            socialId = "kakao-123",
            request = MedicationRecordUpdateRequest(
                recordIds = listOf(500L),
                status = MedicationStatus.SKIP,
            ),
        )

        assertEquals(MedicationStatus.SKIP.name, response.status)
        assertNull(response.takenAt)
        assertEquals(MedicationStatus.SKIP, records.single().status)
        verifyNoInteractions(notificationCreateService)
    }

    @Test
    fun `요청한 recordIds 중 일부가 존재하지 않으면 오류`() {
        val records = listOf(
            medicationRecord(id = 500L, recordUser = user, drugName = "Tylenol", status = MedicationStatus.PENDING),
        )

        given(userRepository.findBySocialId("kakao-123")).willReturn(user)
        given(medicationRecordRepository.findActiveAllByIdIn(setOf(500L, 999L))).willReturn(records)

        val exception = assertFailsWith<BusinessException> {
            service.update(
                socialId = "kakao-123",
                request = MedicationRecordUpdateRequest(
                    recordIds = listOf(500L, 999L),
                    status = MedicationStatus.SUCCESS,
                ),
            )
        }

        assertEquals(ErrorCode.MEDICATION_RECORD_NOT_FOUND, exception.errorCode)
    }

    @Test
    fun `타인 소유 복약 기록이 섞여 있으면 오류`() {
        val records = listOf(
            medicationRecord(id = 500L, recordUser = user, drugName = "Tylenol", status = MedicationStatus.PENDING),
            medicationRecord(id = 502L, recordUser = otherUser, drugName = "Aspirin", status = MedicationStatus.PENDING),
        )

        given(userRepository.findBySocialId("kakao-123")).willReturn(user)
        given(medicationRecordRepository.findActiveAllByIdIn(setOf(500L, 502L))).willReturn(records)

        val exception = assertFailsWith<BusinessException> {
            service.update(
                socialId = "kakao-123",
                request = MedicationRecordUpdateRequest(
                    recordIds = listOf(500L, 502L),
                    status = MedicationStatus.SUCCESS,
                ),
            )
        }

        assertEquals(ErrorCode.MEDICATION_RECORD_NOT_FOUND, exception.errorCode)
    }

    @Test
    fun `서로 다른 처방전 시간대 그룹이 섞여 있으면 오류`() {
        val otherScheduledAt = scheduledAt.plusHours(4)
        val records = listOf(
            medicationRecord(id = 500L, recordUser = user, drugName = "Tylenol", status = MedicationStatus.PENDING),
            medicationRecord(
                id = 503L,
                recordUser = user,
                drugName = "Aspirin",
                status = MedicationStatus.PENDING,
                recordScheduledAt = otherScheduledAt,
            ),
        )

        given(userRepository.findBySocialId("kakao-123")).willReturn(user)
        given(medicationRecordRepository.findActiveAllByIdIn(setOf(500L, 503L))).willReturn(records)

        val exception = assertFailsWith<BusinessException> {
            service.update(
                socialId = "kakao-123",
                request = MedicationRecordUpdateRequest(
                    recordIds = listOf(500L, 503L),
                    status = MedicationStatus.SUCCESS,
                ),
            )
        }

        assertEquals(ErrorCode.INVALID_REQUEST, exception.errorCode)
    }

    @Test
    fun `그룹 내 일부가 이미 처리되었으면 오류이고 상태가 바뀌지 않는다`() {
        val records = listOf(
            medicationRecord(id = 500L, recordUser = user, drugName = "Tylenol", status = MedicationStatus.PENDING),
            medicationRecord(id = 501L, recordUser = user, drugName = "Aspirin", status = MedicationStatus.SUCCESS),
        )

        given(userRepository.findBySocialId("kakao-123")).willReturn(user)
        given(medicationRecordRepository.findActiveAllByIdIn(setOf(500L, 501L))).willReturn(records)
        given(
            medicationRecordRepository.findAllByPrescriptionIdAndScheduledAtAndUserId(10L, scheduledAt, 1L)
        ).willReturn(records)

        val exception = assertFailsWith<BusinessException> {
            service.update(
                socialId = "kakao-123",
                request = MedicationRecordUpdateRequest(
                    recordIds = listOf(500L, 501L),
                    status = MedicationStatus.SKIP,
                ),
            )
        }

        assertEquals(ErrorCode.MEDICATION_RECORD_ALREADY_PROCESSED, exception.errorCode)
        assertEquals(MedicationStatus.PENDING, records[0].status)
        assertEquals(MedicationStatus.SUCCESS, records[1].status)
        verifyNoInteractions(notificationCreateService)
    }

    @Test
    fun `이미 처리된 복약 기록 그룹을 대기 상태로 되돌린다`() {
        val records = listOf(
            medicationRecord(id = 500L, recordUser = user, drugName = "Tylenol", status = MedicationStatus.SUCCESS).apply {
                updateStatus(status = MedicationStatus.SUCCESS, takenAt = LocalDateTime.of(2026, 4, 6, 8, 5))
            },
        )

        given(userRepository.findBySocialId("kakao-123")).willReturn(user)
        given(medicationRecordRepository.findActiveAllByIdIn(setOf(500L))).willReturn(records)
        given(
            medicationRecordRepository.findAllByPrescriptionIdAndScheduledAtAndUserId(10L, scheduledAt, 1L)
        ).willReturn(records)

        val response = service.update(
            socialId = "kakao-123",
            request = MedicationRecordUpdateRequest(
                recordIds = listOf(500L),
                status = MedicationStatus.PENDING,
            ),
        )

        assertEquals(MedicationStatus.PENDING.name, response.status)
        assertNull(response.takenAt)
        assertEquals(MedicationStatus.PENDING, records.single().status)
        assertNull(records.single().takenAt)
        verifyNoInteractions(notificationCreateService)
    }

    @Test
    fun `그룹의 일부 recordIds만 요청하면 오류이고 상태가 바뀌지 않는다`() {
        val requested = medicationRecord(id = 500L, recordUser = user, drugName = "Tylenol", status = MedicationStatus.PENDING)
        val sibling = medicationRecord(id = 501L, recordUser = user, drugName = "Aspirin", status = MedicationStatus.PENDING)

        given(userRepository.findBySocialId("kakao-123")).willReturn(user)
        given(medicationRecordRepository.findActiveAllByIdIn(setOf(500L))).willReturn(listOf(requested))
        given(
            medicationRecordRepository.findAllByPrescriptionIdAndScheduledAtAndUserId(10L, scheduledAt, 1L)
        ).willReturn(listOf(requested, sibling))

        val exception = assertFailsWith<BusinessException> {
            service.update(
                socialId = "kakao-123",
                request = MedicationRecordUpdateRequest(
                    recordIds = listOf(500L),
                    status = MedicationStatus.SUCCESS,
                ),
            )
        }

        assertEquals(ErrorCode.INVALID_REQUEST, exception.errorCode)
        assertEquals(MedicationStatus.PENDING, requested.status)
        assertEquals(MedicationStatus.PENDING, sibling.status)
        verifyNoInteractions(notificationCreateService)
    }

    @Test
    fun `recordIds에 중복이 있어도 정상 처리된다`() {
        val record = medicationRecord(id = 500L, recordUser = user, drugName = "Tylenol", status = MedicationStatus.PENDING)

        given(userRepository.findBySocialId("kakao-123")).willReturn(user)
        given(medicationRecordRepository.findActiveAllByIdIn(setOf(500L))).willReturn(listOf(record))
        given(
            medicationRecordRepository.findAllByPrescriptionIdAndScheduledAtAndUserId(10L, scheduledAt, 1L)
        ).willReturn(listOf(record))

        val response = service.update(
            socialId = "kakao-123",
            request = MedicationRecordUpdateRequest(
                recordIds = listOf(500L, 500L),
                status = MedicationStatus.SUCCESS,
            ),
        )

        assertEquals(listOf(500L), response.recordIds)
        assertEquals(MedicationStatus.SUCCESS, record.status)
    }

    private fun medicationRecord(
        id: Long,
        recordUser: User,
        drugName: String,
        status: MedicationStatus,
        recordScheduledAt: LocalDateTime = scheduledAt,
    ): MedicationRecord {
        val prescriptionDrug = PrescriptionDrug(
            id = id + 1000,
            prescription = prescription,
            drugName = drugName,
        )
        val prescriptionDrugTime = PrescriptionDrugTime(
            id = id + 2000,
            prescriptionDrug = prescriptionDrug,
            takeTime = LocalTime.of(8, 0),
        )

        return MedicationRecord(
            id = id,
            user = recordUser,
            prescription = prescription,
            prescriptionDrugTime = prescriptionDrugTime,
            scheduledAt = recordScheduledAt,
            status = status,
        )
    }
}
