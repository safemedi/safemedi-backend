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
class MedicationReminderService(
    private val medicationRecordRepository: MedicationRecordRepository,
    private val notificationCreateService: NotificationCreateService,
) {

    @Transactional
    fun createDueReminders(
        now: LocalDateTime,
    ) {
        val records = medicationRecordRepository.findPendingRecordsScheduledBetween(
            status = MedicationStatus.PENDING,
            startAt = now.minusMinutes(REMINDER_LOOKBACK_MINUTES),
            endAt = now,
        )

        records.groupBy { it.groupKey() }
            .forEach { (groupKey, groupRecords) ->
                val drugNames = groupRecords.map { it.drugName() }.distinct()

                notificationCreateService.create(
                    NotificationCreateCommand(
                        userId = groupKey.userId,
                        type = NotificationType.MEDICATION_REMINDER,
                        title = "약 복용 시간입니다",
                        content = "${drugNames.joinToString(", ")}을 복용할 시간이에요",
                        targetType = NotificationTargetType.PRESCRIPTION,
                        targetId = groupKey.prescriptionId,
                        deduplicationKey = "MEDICATION_REMINDER:PRESCRIPTION:${groupKey.prescriptionId}:${groupKey.scheduledAt}:${groupKey.userId}",
                        scheduledAt = groupKey.scheduledAt,
                    )
                )
            }
    }

    private fun MedicationRecord.groupKey(): ReminderGroupKey {
        return ReminderGroupKey(
            prescriptionId = requireNotNull(prescription.id),
            scheduledAt = scheduledAt,
            userId = requireNotNull(user.id),
        )
    }

    private fun MedicationRecord.drugName(): String {
        return prescriptionDrugTime.prescriptionDrug.drugName
    }

    private data class ReminderGroupKey(
        val prescriptionId: Long,
        val scheduledAt: LocalDateTime,
        val userId: Long,
    )

    private companion object {
        const val REMINDER_LOOKBACK_MINUTES = 5L
    }
}
