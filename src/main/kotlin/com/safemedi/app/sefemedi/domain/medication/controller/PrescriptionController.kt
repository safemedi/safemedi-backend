package com.safemedi.app.sefemedi.domain.medication.controller

import com.safemedi.app.sefemedi.domain.medication.dto.PrescriptionCreateRequest
import com.safemedi.app.sefemedi.domain.medication.dto.PrescriptionCreateResponse
import com.safemedi.app.sefemedi.domain.medication.dto.PrescriptionDeleteResponse
import com.safemedi.app.sefemedi.domain.medication.dto.PrescriptionDetailResponse
import com.safemedi.app.sefemedi.domain.medication.dto.PrescriptionListResponse
import com.safemedi.app.sefemedi.domain.medication.dto.PrescriptionUpdateRequest
import com.safemedi.app.sefemedi.domain.medication.dto.PrescriptionUpdateResponse
import com.safemedi.app.sefemedi.domain.medication.service.PrescriptionCreateService
import com.safemedi.app.sefemedi.domain.medication.service.PrescriptionDeleteService
import com.safemedi.app.sefemedi.domain.medication.service.PrescriptionQueryService
import com.safemedi.app.sefemedi.domain.medication.service.PrescriptionUpdateService
import com.safemedi.app.sefemedi.global.error.BusinessException
import com.safemedi.app.sefemedi.global.error.ErrorCode
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/prescriptions")
class PrescriptionController(
    private val prescriptionCreateService: PrescriptionCreateService,
    private val prescriptionQueryService: PrescriptionQueryService,
    private val prescriptionDeleteService: PrescriptionDeleteService,
    private val prescriptionUpdateService: PrescriptionUpdateService,
) {
    @GetMapping
    fun findPrescriptions(
        @AuthenticationPrincipal socialId: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
    ): PrescriptionListResponse {
        return prescriptionQueryService.findPrescriptions(
            socialId = socialId ?: throw BusinessException(ErrorCode.INVALID_TOKEN),
            page = page,
            size = size,
        )
    }

    @GetMapping("/{prescriptionId}")
    fun findPrescriptionDetail(
        @AuthenticationPrincipal socialId: String?,
        @PathVariable prescriptionId: Long,
    ): PrescriptionDetailResponse {
        return prescriptionQueryService.findPrescriptionDetail(
            socialId = socialId ?: throw BusinessException(ErrorCode.INVALID_TOKEN),
            prescriptionId = prescriptionId,
        )
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @AuthenticationPrincipal socialId: String?,
        @RequestBody request: PrescriptionCreateRequest,
    ): PrescriptionCreateResponse {
        return prescriptionCreateService.create(
            socialId = socialId ?: throw BusinessException(ErrorCode.INVALID_TOKEN),
            request = request,
        )
    }

    @DeleteMapping("/{prescriptionId}")
    fun delete(
        @AuthenticationPrincipal socialId: String?,
        @PathVariable prescriptionId: Long,
    ): PrescriptionDeleteResponse {
        return prescriptionDeleteService.delete(
            socialId = socialId ?: throw BusinessException(ErrorCode.INVALID_TOKEN),
            prescriptionId = prescriptionId,
        )
    }

    @PatchMapping("/{prescriptionId}")
    fun update(
        @AuthenticationPrincipal socialId: String?,
        @PathVariable prescriptionId: Long,
        @RequestBody request: PrescriptionUpdateRequest,
    ): PrescriptionUpdateResponse {
        return prescriptionUpdateService.update(
            socialId = socialId ?: throw BusinessException(ErrorCode.INVALID_TOKEN),
            prescriptionId = prescriptionId,
            request = request,
        )
    }
}
