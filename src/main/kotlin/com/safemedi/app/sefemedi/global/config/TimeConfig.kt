package com.safemedi.app.sefemedi.global.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.auditing.DateTimeProvider
import java.time.Clock
import java.time.LocalDateTime
import java.util.Optional

@Configuration
class TimeConfig {

    @Bean
    fun utcClock(): Clock = Clock.systemUTC()

    @Bean
    fun utcDateTimeProvider(clock: Clock): DateTimeProvider {
        return DateTimeProvider { Optional.of(LocalDateTime.now(clock)) }
    }
}
