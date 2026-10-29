package com.safemedi.app.sefemedi.domain.family.controller

import com.safemedi.app.sefemedi.domain.family.dto.FamilyListResponse
import com.safemedi.app.sefemedi.domain.family.dto.FamilyRelationUpdateRequest
import com.safemedi.app.sefemedi.domain.family.dto.FamilyRelationUpdateResponse
import com.safemedi.app.sefemedi.domain.family.service.FamilyDisconnectService
import com.safemedi.app.sefemedi.domain.family.service.FamilyListQueryService
import com.safemedi.app.sefemedi.domain.family.service.FamilyRelationUpdateService
import com.safemedi.app.sefemedi.domain.user.dto.MedicalSummaryResponse
import com.safemedi.app.sefemedi.domain.user.service.MedicalSummaryService
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/families")
class FamilyController(
    private val medicalSummaryService: MedicalSummaryService,
    private val familyRelationUpdateService: FamilyRelationUpdateService,
    private val familyDisconnectService: FamilyDisconnectService,
    private val familyListQueryService: FamilyListQueryService,
) {

    @GetMapping
    fun getFamilies(authentication: Authentication): FamilyListResponse {
        return familyListQueryService.getFamilies(authentication.name)
    }

    @GetMapping("/{familyId}/medical-summary")
    fun getFamilyMedicalSummary(
        authentication: Authentication,
        @PathVariable familyId: Long,
    ): MedicalSummaryResponse {
        return medicalSummaryService.getFamilyMedicalSummary(
            socialId = authentication.name,
            familyId = familyId,
        )
    }

    @PatchMapping("/{familyId}")
    fun updateRelation(
        authentication: Authentication,
        @PathVariable familyId: Long,
        @RequestBody request: FamilyRelationUpdateRequest,
    ): FamilyRelationUpdateResponse {
        return familyRelationUpdateService.update(
            socialId = authentication.name,
            familyId = familyId,
            request = request,
        )
    }

    @DeleteMapping("/{familyId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun disconnect(
        authentication: Authentication,
        @PathVariable familyId: Long,
    ) {
        familyDisconnectService.disconnect(
            socialId = authentication.name,
            familyId = familyId,
        )
    }
}
