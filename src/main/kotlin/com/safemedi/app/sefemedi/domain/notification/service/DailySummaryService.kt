package com.safemedi.app.sefemedi.domain.notification.service

import com.safemedi.app.sefemedi.domain.medication.entity.MedicationRecord
import com.safemedi.app.sefemedi.domain.medication.entity.MedicationStatus
import com.safemedi.app.sefemedi.domain.medication.repository.MedicationRecordRepository
import com.safemedi.app.sefemedi.domain.notification.dto.NotificationCreateCommand
import com.safemedi.app.sefemedi.domain.notification.entity.NotificationTargetType
import com.safemedi.app.sefemedi.domain.notification.entity.NotificationType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class DailySummaryService(
    private val medicationRecordRepository: MedicationRecordRepository,
    private val notificationCreateService: NotificationCreateService,
) {

    @Transactional
    fun create(now: LocalDateTime) {
        val records = medicationRecordRepository.findPendingRecordsScheduledBetween(
            status = MedicationStatus.PENDING,
            startAt = now.toLocalDate().atStartOfDay().minusNanos(1),
            endAt = now.toLocalDate().plusDays(1).atStartOfDay().minusNanos(1),
        )

        records.groupBy { it.requireUserId() }
            .forEach { (userId, userRecords) ->
                val firstRecord = userRecords.minBy { it.scheduledAt }
                notificationCreateService.create(
                    NotificationCreateCommand(
                        userId = userId,
                        type = NotificationType.TODAY_MEDICATION_SCHEDULE,
                        title = "오늘의 복약 스케줄",
                        content = "오늘 복용할 약이 ${userRecords.size}개 남았어요",
                        targetType = NotificationTargetType.MEDICATION_RECORD,
                        targetId = firstRecord.requireRecordId(),
                        deduplicationKey = "TODAY_MEDICATION_SCHEDULE:${now.toLocalDate()}:$userId",
                        scheduledAt = now,
                    )
                )
            }
    }

    private fun MedicationRecord.requireRecordId(): Long {
        return requireNotNull(id)
    }

    private fun MedicationRecord.requireUserId(): Long {
        return requireNotNull(user.id)
    }
}
