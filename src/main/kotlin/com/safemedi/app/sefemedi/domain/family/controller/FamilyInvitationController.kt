package com.safemedi.app.sefemedi.domain.family.controller

import com.safemedi.app.sefemedi.domain.family.dto.FamilyInvitationAcceptResponse
import com.safemedi.app.sefemedi.domain.family.dto.FamilyInvitationCreateResponse
import com.safemedi.app.sefemedi.domain.family.dto.FamilyInvitationInfoResponse
import com.safemedi.app.sefemedi.domain.family.service.FamilyInvitationAcceptService
import com.safemedi.app.sefemedi.domain.family.service.FamilyInvitationCreateService
import com.safemedi.app.sefemedi.domain.family.service.FamilyInvitationQueryService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/family-invitations")
class FamilyInvitationController(
    private val familyInvitationCreateService: FamilyInvitationCreateService,
    private val familyInvitationQueryService: FamilyInvitationQueryService,
    private val familyInvitationAcceptService: FamilyInvitationAcceptService,
) {

    @PostMapping
    fun createInvitation(authentication: Authentication): ResponseEntity<FamilyInvitationCreateResponse> {
        val result = familyInvitationCreateService.create(authentication.name)
        return ResponseEntity
            .status(if (result.created) HttpStatus.CREATED else HttpStatus.OK)
            .body(result.response)
    }

    @GetMapping("/{token}")
    fun getInvitationInfo(
        authentication: Authentication,
        @PathVariable token: String,
    ): FamilyInvitationInfoResponse {
        return familyInvitationQueryService.getInvitationInfo(
            socialId = authentication.name,
            token = token,
        )
    }

    @PostMapping("/{token}/accept")
    fun acceptInvitation(
        authentication: Authentication,
        @PathVariable token: String,
    ): FamilyInvitationAcceptResponse {
        return familyInvitationAcceptService.accept(
            socialId = authentication.name,
            token = token,
        )
    }
}
