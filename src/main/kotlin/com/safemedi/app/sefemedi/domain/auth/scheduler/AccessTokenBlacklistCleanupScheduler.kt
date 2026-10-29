package com.safemedi.app.sefemedi.domain.auth.scheduler

import com.safemedi.app.sefemedi.domain.auth.service.AccessTokenBlacklistService
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class AccessTokenBlacklistCleanupScheduler(
    private val accessTokenBlacklistService: AccessTokenBlacklistService,
) {

    @Scheduled(fixedDelay = 3_600_000)
    fun purgeExpiredEntries() {
        accessTokenBlacklistService.purgeExpiredEntries()
    }
}
