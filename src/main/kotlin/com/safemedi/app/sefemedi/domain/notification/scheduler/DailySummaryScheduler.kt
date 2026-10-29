package com.safemedi.app.sefemedi.domain.notification.scheduler

import com.safemedi.app.sefemedi.domain.notification.service.DailySummaryService
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.ZoneId

@Component
class DailySummaryScheduler(
    private val dailySummaryService: DailySummaryService,
) {

    @Scheduled(cron = "0 0 8 * * *", zone = "Asia/Seoul")
    fun createDailySummaries() {
        dailySummaryService.create(
            LocalDateTime.now(SERVICE_ZONE_ID),
        )
    }

    private companion object {
        val SERVICE_ZONE_ID: ZoneId = ZoneId.of("Asia/Seoul")
    }
}
