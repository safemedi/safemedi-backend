package com.safemedi.app.sefemedi.domain.notification.scheduler

import com.safemedi.app.sefemedi.domain.notification.service.MedicationReminderService
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.ZoneId

@Component
class MedicationReminderScheduler(
    private val medicationReminderService: MedicationReminderService,
) {

    @Scheduled(fixedDelay = 60_000)
    fun createDueReminders() {
        medicationReminderService.createDueReminders(
            LocalDateTime.now(SERVICE_ZONE_ID),
        )
    }

    private companion object {
        val SERVICE_ZONE_ID: ZoneId = ZoneId.of("Asia/Seoul")
    }
}
