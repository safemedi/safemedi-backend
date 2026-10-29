package com.safemedi.app.sefemedi.domain.notification.service

import com.safemedi.app.sefemedi.domain.medication.entity.MedicationRecord
import com.safemedi.app.sefemedi.domain.medication.entity.MedicationStatus
import com.safemedi.app.sefemedi.domain.medication.entity.Prescription
import com.safemedi.app.sefemedi.domain.medication.entity.PrescriptionDrug
import com.safemedi.app.sefemedi.domain.medication.entity.PrescriptionDrugTime
import com.safemedi.app.sefemedi.domain.medication.repository.MedicationRecordRepository
import com.safemedi.app.sefemedi.domain.notification.dto.NotificationCreateCommand
import com.safemedi.app.sefemedi.domain.notification.entity.NotificationTargetType
import com.safemedi.app.sefemedi.domain.notification.entity.NotificationType
import com.safemedi.app.sefemedi.domain.user.entity.User
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockingDetails
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class DailySummaryServiceTest {

    private lateinit var medicationRecordRepository: MedicationRecordRepository
    private lateinit var notificationCreateService: NotificationCreateService
    private lateinit var dailySummaryService: DailySummaryService

    @BeforeEach
    fun setUp() {
        medicationRecordRepository = mock(MedicationRecordRepository::class.java)
        notificationCreateService = mock(NotificationCreateService::class.java)
        dailySummaryService = DailySummaryService(
            medicationRecordRepository = medicationRecordRepository,
            notificationCreateService = notificationCreateService,
        )
    }

    @Test
    fun `create makes one daily summary per user`() {
        val now = LocalDateTime.of(2026, 6, 27, 8, 0)
        val user = User(
            id = 1L,
            socialId = "kakao-123",
        )
        val records = listOf(
            medicationRecord(
                id = 500L,
                user = user,
                scheduledAt = LocalDateTime.of(2026, 6, 27, 9, 0),
            ),
            medicationRecord(
                id = 501L,
                user = user,
                scheduledAt = LocalDateTime.of(2026, 6, 27, 20, 0),
            ),
        )

        given(
            medicationRecordRepository.findPendingRecordsScheduledBetween(
                status = MedicationStatus.PENDING,
                startAt = LocalDate.of(2026, 6, 27).atStartOfDay().minusNanos(1),
                endAt = LocalDate.of(2026, 6, 28).atStartOfDay().minusNanos(1),
            )
        ).willReturn(records)

        dailySummaryService.create(now)

        val command = mockingDetails(notificationCreateService)
            .invocations
            .single()
            .arguments[0] as NotificationCreateCommand
        assertEquals(1L, command.userId)
        assertEquals(NotificationType.TODAY_MEDICATION_SCHEDULE, command.type)
        assertEquals("오늘의 복약 스케줄", command.title)
        assertEquals("오늘 복용할 약이 2개 남았어요", command.content)
        assertEquals(NotificationTargetType.MEDICATION_RECORD, command.targetType)
        assertEquals(500L, command.targetId)
        assertEquals("TODAY_MEDICATION_SCHEDULE:2026-06-27:1", command.deduplicationKey)
        assertEquals(now, command.scheduledAt)
    }

    @Test
    fun `create does nothing when there are no pending records`() {
        val now = LocalDateTime.of(2026, 6, 27, 8, 0)

        given(
            medicationRecordRepository.findPendingRecordsScheduledBetween(
                status = MedicationStatus.PENDING,
                startAt = LocalDate.of(2026, 6, 27).atStartOfDay().minusNanos(1),
                endAt = LocalDate.of(2026, 6, 28).atStartOfDay().minusNanos(1),
            )
        ).willReturn(emptyList())

        dailySummaryService.create(now)

        verifyNoInteractions(notificationCreateService)
    }

    @Test
    fun `create queries from now to end of day`() {
        val now = LocalDateTime.of(2026, 6, 27, 8, 0)

        given(
            medicationRecordRepository.findPendingRecordsScheduledBetween(
                status = MedicationStatus.PENDING,
                startAt = LocalDate.of(2026, 6, 27).atStartOfDay().minusNanos(1),
                endAt = LocalDate.of(2026, 6, 28).atStartOfDay().minusNanos(1),
            )
        ).willReturn(emptyList())

        dailySummaryService.create(now)

        verify(medicationRecordRepository).findPendingRecordsScheduledBetween(
            status = MedicationStatus.PENDING,
            startAt = LocalDate.of(2026, 6, 27).atStartOfDay().minusNanos(1),
            endAt = LocalDate.of(2026, 6, 28).atStartOfDay().minusNanos(1),
        )
    }

    private fun medicationRecord(
        id: Long,
        user: User,
        scheduledAt: LocalDateTime,
    ): MedicationRecord {
        val prescription = Prescription(
            id = 10L,
            user = user,
            title = "Prescription",
            startDate = LocalDate.of(2026, 6, 20),
            endDate = LocalDate.of(2026, 6, 30),
        )
        val prescriptionDrug = PrescriptionDrug(
            id = 20L,
            prescription = prescription,
            drugName = "Tylenol",
        )
        val prescriptionDrugTime = PrescriptionDrugTime(
            id = 30L,
            prescriptionDrug = prescriptionDrug,
            takeTime = LocalTime.of(9, 0),
        )

        return MedicationRecord(
            id = id,
            user = user,
            prescription = prescription,
            prescriptionDrugTime = prescriptionDrugTime,
            scheduledAt = scheduledAt,
            status = MedicationStatus.PENDING,
        )
    }
}
