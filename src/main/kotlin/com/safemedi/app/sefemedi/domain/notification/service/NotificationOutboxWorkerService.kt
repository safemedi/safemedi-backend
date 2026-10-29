package com.safemedi.app.sefemedi.domain.notification.service

import com.safemedi.app.sefemedi.domain.notification.entity.NotificationOutbox
import com.safemedi.app.sefemedi.domain.notification.entity.NotificationOutboxStatus
import com.safemedi.app.sefemedi.domain.notification.fcm.NotificationPushRequest
import com.safemedi.app.sefemedi.domain.notification.fcm.NotificationPushResult
import com.safemedi.app.sefemedi.domain.notification.fcm.NotificationPushSender
import com.safemedi.app.sefemedi.domain.notification.fcm.NotificationPushStatus
import com.safemedi.app.sefemedi.domain.notification.repository.NotificationOutboxRepository
import com.safemedi.app.sefemedi.domain.user.repository.UserDeviceRepository
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.data.domain.PageRequest
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import java.time.LocalDateTime

@Service
@ConditionalOnProperty(prefix = "firebase", name = ["enabled"], havingValue = "true")
class NotificationOutboxWorkerService(
    private val notificationOutboxRepository: NotificationOutboxRepository,
    private val userDeviceRepository: UserDeviceRepository,
    private val notificationPushSender: NotificationPushSender,
    private val transactionTemplate: TransactionTemplate,
) {

    fun processDueOutboxes(
        now: LocalDateTime,
        batchSize: Int,
    ): Int {
        val targets = claimDueOutboxes(
            now = now,
            batchSize = batchSize,
        )

        targets.forEach {
            sendAndComplete(
                target = it,
                now = now,
            )
        }

        return targets.size
    }

    private fun claimDueOutboxes(
        now: LocalDateTime,
        batchSize: Int,
    ): List<OutboxSendTarget> {
        return transactionTemplate.execute {
            notificationOutboxRepository.findDueOutboxes(
                pendingStatus = NotificationOutboxStatus.PENDING,
                failedStatus = NotificationOutboxStatus.FAILED,
                now = now,
                pageable = PageRequest.of(0, batchSize),
            ).mapNotNull(::claimOutbox)
        } ?: emptyList()
    }

    private fun claimOutbox(
        outbox: NotificationOutbox,
    ): OutboxSendTarget? {
        outbox.markProcessing()

        val userId = outbox.user.id
        if (userId == null) {
            log.warn(
                "FCM 발송 실패: 사용자 ID가 없어 알림 작업을 DEAD 처리합니다. outboxId={}, eventKey={}",
                outbox.id,
                outbox.eventKey,
            )
            outbox.markDead()
            return null
        }

        val userDevice = userDeviceRepository.findFirstByUser_IdAndIsActiveTrueOrderByCreatedAtDesc(userId)
        if (userDevice == null) {
            log.warn(
                "FCM 발송 실패: 활성 기기 토큰이 없어 알림 작업을 DEAD 처리합니다. outboxId={}, userId={}, eventKey={}",
                outbox.id,
                userId,
                outbox.eventKey,
            )
            outbox.markDead()
            return null
        }

        val outboxId = outbox.id
        val deviceId = userDevice.id
        if (outboxId == null || deviceId == null) {
            log.warn(
                "FCM 발송 실패: 알림 작업 또는 기기 ID가 없어 알림 작업을 DEAD 처리합니다. outboxId={}, deviceId={}, eventKey={}",
                outboxId,
                deviceId,
                outbox.eventKey,
            )
            outbox.markDead()
            return null
        }

        return OutboxSendTarget(
            outboxId = outboxId,
            userId = userId,
            deviceId = deviceId,
            token = userDevice.deviceToken,
            title = outbox.notificationLog.title,
            body = outbox.notificationLog.message,
            data = createData(outbox),
        )
    }

    private fun sendAndComplete(
        target: OutboxSendTarget,
        now: LocalDateTime,
    ) {
        val isCurrentTarget = transactionTemplate.execute {
            val device = userDeviceRepository.findById(target.deviceId).orElse(null)
            val valid = device != null && device.isActive &&
                device.user.id == target.userId && device.deviceToken == target.token
            if (!valid) {
                findOutboxOrNull(target.outboxId)?.markDead()
            }
            valid
        } == true
        if (!isCurrentTarget) return

        val result = notificationPushSender.send(
            NotificationPushRequest(
                token = target.token,
                title = target.title,
                body = target.body,
                data = target.data,
            )
        )

        when (result.status) {
            NotificationPushStatus.SUCCESS -> completeSuccess(target)
            NotificationPushStatus.INVALID_TOKEN -> completeInvalidToken(target, result)
            NotificationPushStatus.TEMPORARY_FAILURE -> completeTemporaryFailure(target, result, now)
            NotificationPushStatus.PERMANENT_FAILURE -> completePermanentFailure(target, result)
        }
    }

    private fun completeSuccess(
        target: OutboxSendTarget,
    ) {
        transactionTemplate.executeWithoutResult {
            val outbox = findOutboxOrNull(target.outboxId) ?: return@executeWithoutResult
            log.info(
                "FCM 발송 성공. outboxId={}, userId={}, notificationId={}",
                target.outboxId,
                target.userId,
                outbox.notificationLog.id,
            )
            outbox.markSent()
        }
    }

    private fun completeInvalidToken(
        target: OutboxSendTarget,
        result: NotificationPushResult,
    ) {
        transactionTemplate.executeWithoutResult {
            val outbox = findOutboxOrNull(target.outboxId) ?: return@executeWithoutResult
            log.warn(
                "FCM 발송 실패: 유효하지 않은 기기 토큰입니다. outboxId={}, userId={}, deviceId={}, errorCode={}",
                target.outboxId,
                target.userId,
                target.deviceId,
                result.errorCode,
            )
            val device = userDeviceRepository.findByDeviceToken(target.token)
            if (device?.id == target.deviceId && device.user.id == target.userId) {
                device.deactivate()
            }
            outbox.markDead()
        }
    }

    private fun completeTemporaryFailure(
        target: OutboxSendTarget,
        result: NotificationPushResult,
        now: LocalDateTime,
    ) {
        transactionTemplate.executeWithoutResult {
            val outbox = findOutboxOrNull(target.outboxId) ?: return@executeWithoutResult
            log.warn(
                "FCM 발송 일시 실패: 재시도 대상으로 처리합니다. outboxId={}, userId={}, retryCount={}, errorCode={}",
                target.outboxId,
                target.userId,
                outbox.retryCount,
                result.errorCode,
            )
            scheduleRetryOrDead(outbox, now)
        }
    }

    private fun completePermanentFailure(
        target: OutboxSendTarget,
        result: NotificationPushResult,
    ) {
        transactionTemplate.executeWithoutResult {
            val outbox = findOutboxOrNull(target.outboxId) ?: return@executeWithoutResult
            log.warn(
                "FCM 발송 영구 실패: 알림 작업을 DEAD 처리합니다. outboxId={}, userId={}, errorCode={}",
                target.outboxId,
                target.userId,
                result.errorCode,
            )
            outbox.markDead()
        }
    }

    private fun findOutboxOrNull(
        outboxId: Long,
    ): NotificationOutbox? {
        return notificationOutboxRepository.findById(outboxId).orElse(null)
    }

    private fun createData(
        outbox: NotificationOutbox,
    ): Map<String, String> {
        val notificationLog = outbox.notificationLog
        val data = mutableMapOf(
            "notificationId" to requireNotNull(notificationLog.id).toString(),
            "type" to notificationLog.type.name,
        )
        notificationLog.targetType?.let { data["targetType"] = it.name }
        notificationLog.targetId?.let { data["targetId"] = it.toString() }

        return data
    }

    private fun scheduleRetryOrDead(
        outbox: NotificationOutbox,
        now: LocalDateTime,
    ) {
        if (outbox.retryCount >= MAX_RETRY_COUNT) {
            outbox.markDead()
            return
        }

        outbox.markRetryScheduled(
            nextRetryAt = now.plusMinutes(retryDelayMinutes(outbox.retryCount)),
        )
    }

    private fun retryDelayMinutes(
        retryCount: Int,
    ): Long {
        return RETRY_DELAYS_MINUTES.getOrElse(retryCount) { RETRY_DELAYS_MINUTES.last() }
    }

    private companion object {
        private val log = LoggerFactory.getLogger(NotificationOutboxWorkerService::class.java)
        const val MAX_RETRY_COUNT = 3
        val RETRY_DELAYS_MINUTES = listOf(1L, 5L, 30L)
    }

    private data class OutboxSendTarget(
        val outboxId: Long,
        val userId: Long,
        val deviceId: Long,
        val token: String,
        val title: String,
        val body: String,
        val data: Map<String, String>,
    )
}
