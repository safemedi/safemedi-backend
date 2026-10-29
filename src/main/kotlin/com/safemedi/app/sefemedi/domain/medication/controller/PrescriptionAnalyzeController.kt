package com.safemedi.app.sefemedi.domain.medication.controller

import com.safemedi.app.sefemedi.domain.medication.dto.PrescriptionAnalyzeRequest
import com.safemedi.app.sefemedi.domain.medication.dto.PrescriptionAnalyzeResponse
import com.safemedi.app.sefemedi.domain.medication.service.PrescriptionAnalyzeService
import com.safemedi.app.sefemedi.global.error.BusinessException
import com.safemedi.app.sefemedi.global.error.ErrorCode
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/prescriptions")
class PrescriptionAnalyzeController(
    private val prescriptionAnalyzeService: PrescriptionAnalyzeService,
) {
    @PostMapping("/analyze")
    fun analyze(
        @AuthenticationPrincipal socialId: String?,
        @RequestBody request: PrescriptionAnalyzeRequest,
    ): PrescriptionAnalyzeResponse {
        return prescriptionAnalyzeService.analyze(
            socialId = socialId ?: throw BusinessException(ErrorCode.INVALID_TOKEN),
            request = request,
        )
    }
}
