package com.safemedi.app.sefemedi.global.config

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

class TimeConfigTest {

    @Test
    fun `JPA 감사 시각은 UTC Clock을 사용한다`() {
        val clock = Clock.fixed(Instant.parse("2026-07-15T06:00:00Z"), ZoneOffset.UTC)
        val provider = TimeConfig().utcDateTimeProvider(clock)

        val result = LocalDateTime.from(provider.now.orElseThrow())

        assertEquals(LocalDateTime.of(2026, 7, 15, 6, 0), result)
    }
}
