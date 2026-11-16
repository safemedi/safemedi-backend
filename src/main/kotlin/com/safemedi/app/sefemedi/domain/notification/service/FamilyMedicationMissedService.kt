package com.safemedi.app.sefemedi.domain.notification.service

import com.safemedi.app.sefemedi.domain.family.repository.FamilyRepository
import com.safemedi.app.sefemedi.domain.medication.entity.MedicationStatus
import com.safemedi.app.sefemedi.domain.medication.repository.MedicationRecordRepository
import com.safemedi.app.sefemedi.domain.notification.dto.NotificationCreateCommand
import com.safemedi.app.sefemedi.domain.notification.entity.NotificationTargetType
import com.safemedi.app.sefemedi.domain.notification.entity.NotificationType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class FamilyMedicationMissedService(
    private val medicationRecordRepository: MedicationRecordRepository,
    private val familyRepository: FamilyRepository,
    private val notificationCreateService: NotificationCreateService,
) {
    @Transactional
    fun create(now: LocalDateTime) {
        val date = now.toLocalDate()
        val cutoff = date.atTime(22, 0)
        val userIds = medicationRecordRepository.findUserIdsWithLowAdherence(
            startAt = date.atStartOfDay(),
            endAt = cutoff,
            successStatus = MedicationStatus.SUCCESS,
        )
        if (userIds.isEmpty()) return

        familyRepository.findMissedMedicationAlertRecipients(userIds).forEach { family ->
            val recipientId = requireNotNull(family.user.id)
            val memberId = requireNotNull(family.connectedUser.id)
            val nickname = family.connectedUser.nickname?.takeIf { it.isNotBlank() } ?: "가족"
            notificationCreateService.create(
                NotificationCreateCommand(
                    userId = recipientId,
                    type = NotificationType.FAMILY_MEDICATION_MISSED,
                    title = "가족 복약 안내",
                    content = "${nickname}님이 약을 다 먹지 않았어요. 건강을 위해 함께 노력해요!",
                    targetType = NotificationTargetType.FAMILY,
                    targetId = requireNotNull(family.id),
                    deduplicationKey = "FAMILY_MEDICATION_MISSED:$date:$memberId:$recipientId",
                    scheduledAt = cutoff,
                ),
            )
        }
    }
}
