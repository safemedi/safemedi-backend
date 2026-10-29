package com.safemedi.app.sefemedi

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.scheduling.annotation.EnableScheduling

@EnableJpaAuditing(dateTimeProviderRef = "utcDateTimeProvider")
@EnableScheduling
@SpringBootApplication
class SefemediApplication

fun main(args: Array<String>) {
	runApplication<SefemediApplication>(*args)
}
