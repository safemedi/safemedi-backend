package com.safemedi.app.sefemedi.global.config

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityConfigTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `well-known assetlinks 요청은 인증 없이 200과 JSON을 반환한다`() {
        mockMvc.perform(get("/.well-known/assetlinks.json"))
            .andExpect(status().isOk)
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$[0].target.package_name").value("com.safemedi"))
    }

    @Test
    fun `Prometheus 메트릭은 인증 없이 조회할 수 있다`() {
        mockMvc.perform(get("/actuator/health"))
            .andExpect(status().isOk)

        mockMvc.perform(get("/actuator/prometheus"))
            .andExpect(status().isOk)
            .andExpect(content().string(org.hamcrest.Matchers.containsString("jvm_")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("http_server_requests_seconds_bucket")))
    }
}
