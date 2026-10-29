package com.safemedi.app.sefemedi.domain.notification.service

import com.safemedi.app.sefemedi.domain.notification.dto.NotificationCreateCommand
import com.safemedi.app.sefemedi.domain.notification.dto.NotificationCreateResult
import com.safemedi.app.sefemedi.domain.notification.dto.NotificationCreateStatus
import com.safemedi.app.sefemedi.domain.notification.entity.NotificationLog
import com.safemedi.app.sefemedi.domain.notification.entity.NotificationOutbox
import com.safemedi.app.sefemedi.domain.notification.entity.NotificationType
import com.safemedi.app.sefemedi.domain.notification.repository.NotificationLogRepository
import com.safemedi.app.sefemedi.domain.notification.repository.NotificationOutboxRepository
import com.safemedi.app.sefemedi.domain.user.entity.UserDevice
import com.safemedi.app.sefemedi.domain.user.repository.UserDeviceRepository
import com.safemedi.app.sefemedi.domain.user.repository.UserRepository
import com.safemedi.app.sefemedi.global.error.BusinessException
import com.safemedi.app.sefemedi.global.error.ErrorCode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.ZoneId

@Service
class NotificationCreateService(
    private val userRepository: UserRepository,
    private val userDeviceRepository: UserDeviceRepository,
    private val notificationLogRepository: NotificationLogRepository,
    private val notificationOutboxRepository: NotificationOutboxRepository,
) {
    private val serviceZoneId = ZoneId.of("Asia/Seoul")

    @Transactional
    fun create(
        command: NotificationCreateCommand,
    ): NotificationCreateResult {
        validate(command)

        val existingNotification = notificationLogRepository.findByDeduplicationKey(command.deduplicationKey)
        if (existingNotification != null) {
            return NotificationCreateResult(
                status = NotificationCreateStatus.DUPLICATED,
                notification = existingNotification,
            )
        }

        val user = userRepository.findById(command.userId)
            .orElseThrow { BusinessException(ErrorCode.USER_NOT_FOUND) }
        val latestDevice = userDeviceRepository.findFirstByUser_IdAndIsActiveTrueOrderByCreatedAtDesc(command.userId)

        if (latestDevice == null) {
            return NotificationCreateResult(
                status = NotificationCreateStatus.SKIPPED_NO_ACTIVE_DEVICE,
            )
        }

        if (!shouldCreate(command.type, latestDevice)) {
            return NotificationCreateResult(
                status = NotificationCreateStatus.SKIPPED_BY_SETTINGS,
            )
        }

        val notificationLog = notificationLogRepository.save(
            NotificationLog(
                user = user,
                type = command.type,
                title = command.title.trim(),
                message = command.content.trim(),
                targetType = command.targetType,
                targetId = command.targetId,
                deduplicationKey = command.deduplicationKey.trim(),
            )
        )

        notificationOutboxRepository.save(
            NotificationOutbox(
                notificationLog = notificationLog,
                user = user,
                eventKey = command.deduplicationKey.trim(),
                scheduledAt = command.scheduledAt ?: LocalDateTime.now(serviceZoneId),
            )
        )

        return NotificationCreateResult(
            status = NotificationCreateStatus.CREATED,
            notification = notificationLog,
        )
    }

    private fun validate(
        command: NotificationCreateCommand,
    ) {
        if (command.title.isBlank() || command.content.isBlank() || command.deduplicationKey.isBlank()) {
            throw BusinessException(ErrorCode.INVALID_REQUEST)
        }
    }

    private fun shouldCreate(
        type: NotificationType,
        latestDevice: UserDevice,
    ): Boolean {
        return when (type) {
            NotificationType.MEDICATION_REMINDER -> latestDevice.isMyReminderOn
            NotificationType.FAMILY_CONNECTED,
            NotificationType.FAMILY_DISCONNECTED -> latestDevice.isFamilyReminderOn
            NotificationType.MEDICATION_COMPLETED,
            NotificationType.DRUG_INTERACTION_WARNING,
            NotificationType.TODAY_MEDICATION_SCHEDULE,
            NotificationType.REPORT_READY -> true
        }
    }
}
