package com.safemedi.app.sefemedi.domain.notification.scheduler

import com.safemedi.app.sefemedi.domain.notification.service.NotificationOutboxWorkerService
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.ZoneId

@Component
@ConditionalOnProperty(prefix = "firebase", name = ["enabled"], havingValue = "true")
class NotificationOutboxScheduler(
    private val notificationOutboxWorkerService: NotificationOutboxWorkerService,
    @param:Value("\${firebase.outbox.batch-size}")
    private val batchSize: Int,
) {

    @Scheduled(fixedDelayString = "\${firebase.outbox.fixed-delay-ms}")
    fun processDueOutboxes() {
        notificationOutboxWorkerService.processDueOutboxes(
            now = LocalDateTime.now(SERVICE_ZONE_ID),
            batchSize = batchSize,
        )
    }

    private companion object {
        val SERVICE_ZONE_ID: ZoneId = ZoneId.of("Asia/Seoul")
    }
}
