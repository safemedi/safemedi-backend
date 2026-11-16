package com.safemedi.app.sefemedi.domain.notification.scheduler

import com.safemedi.app.sefemedi.domain.notification.service.FamilyMedicationMissedService
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.ZoneId

@Component
class FamilyMedicationMissedScheduler(
    private val familyMedicationMissedService: FamilyMedicationMissedService,
) {
    @Scheduled(cron = "0 0 22 * * *", zone = "Asia/Seoul")
    fun createFamilyMedicationMissedNotifications() {
        familyMedicationMissedService.create(LocalDateTime.now(ZoneId.of("Asia/Seoul")))
    }
}
