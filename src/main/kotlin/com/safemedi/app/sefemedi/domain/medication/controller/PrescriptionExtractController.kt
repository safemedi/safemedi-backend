package com.safemedi.app.sefemedi.domain.medication.controller

import com.safemedi.app.sefemedi.domain.medication.dto.PrescriptionExtractResponse
import com.safemedi.app.sefemedi.domain.medication.service.PrescriptionExtractService
import com.safemedi.app.sefemedi.global.error.BusinessException
import com.safemedi.app.sefemedi.global.error.ErrorCode
import org.springframework.http.MediaType
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/v1/prescriptions")
class PrescriptionExtractController(private val service: PrescriptionExtractService) {
    @PostMapping("/extract", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun extract(
        @AuthenticationPrincipal socialId: String?,
        @RequestPart("image") image: List<MultipartFile>,
    ): PrescriptionExtractResponse {
        if (socialId == null) throw BusinessException(ErrorCode.INVALID_TOKEN)
        if (image.size != 1) throw BusinessException(ErrorCode.INVALID_PRESCRIPTION_IMAGE)
        return service.extract(socialId, image.single())
    }
}
