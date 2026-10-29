package com.safemedi.app.sefemedi.domain.user.controller

import com.safemedi.app.sefemedi.domain.user.dto.MedicalSummaryResponse
import com.safemedi.app.sefemedi.domain.user.service.MedicalSummaryService
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/users")
class UserMedicalSummaryController(
    private val medicalSummaryService: MedicalSummaryService,
) {

    @GetMapping("/me/medical-summary")
    fun getMyMedicalSummary(
        authentication: Authentication,
    ): MedicalSummaryResponse {
        return medicalSummaryService.getMyMedicalSummary(
            socialId = authentication.name,
        )
    }
}
