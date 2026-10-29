package com.safemedi.app.sefemedi

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.test.context.ActiveProfiles
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import tools.jackson.databind.json.JsonMapper

@SpringBootTest
@ActiveProfiles("test")
class SefemediApplicationTests {

	@Test
	fun contextLoads() {
	}

	@TestConfiguration
	class TestBeans {
		@Bean
		@Primary
		fun jsonMapper(): JsonMapper {
			return JsonMapper.builder().build()
		}
	}
}
