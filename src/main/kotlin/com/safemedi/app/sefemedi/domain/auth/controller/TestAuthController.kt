package com.safemedi.app.sefemedi.domain.auth.controller

import com.safemedi.app.sefemedi.domain.auth.dto.LoginResponse
import com.safemedi.app.sefemedi.domain.auth.dto.TestLoginRequest
import com.safemedi.app.sefemedi.domain.auth.service.TestAuthService
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@ConditionalOnProperty(
    prefix = "app.test-login",
    name = ["enabled"],
    havingValue = "true",
)
@RequestMapping("/api/v1/auth")
class TestAuthController(

    private val testAuthService: TestAuthService,
) {

    @PostMapping("/test-login")
    fun testLogin(
        @RequestBody request: TestLoginRequest,
    ): LoginResponse {
        return testAuthService.testLogin(
            socialId = request.socialId,
        )
    }
}
