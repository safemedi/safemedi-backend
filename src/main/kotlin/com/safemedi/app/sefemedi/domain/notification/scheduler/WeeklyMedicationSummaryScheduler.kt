package com.safemedi.app.sefemedi.domain.notification.scheduler

import com.safemedi.app.sefemedi.domain.notification.service.WeeklyMedicationSummaryService
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.ZoneId

@Component
class WeeklyMedicationSummaryScheduler(
    private val weeklyMedicationSummaryService: WeeklyMedicationSummaryService,
) {
    @Scheduled(cron = "0 0 22 * * SUN", zone = "Asia/Seoul")
    fun createWeeklyMedicationSummaries() {
        weeklyMedicationSummaryService.create(LocalDateTime.now(ZoneId.of("Asia/Seoul")))
    }
}
