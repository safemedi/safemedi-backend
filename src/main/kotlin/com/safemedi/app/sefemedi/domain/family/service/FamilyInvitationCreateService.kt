package com.safemedi.app.sefemedi.domain.family.service

import com.safemedi.app.sefemedi.domain.family.dto.FamilyInvitationCreateResponse
import com.safemedi.app.sefemedi.domain.family.entity.FamilyInvitation
import com.safemedi.app.sefemedi.domain.family.entity.FamilyInvitationStatus
import com.safemedi.app.sefemedi.domain.family.repository.FamilyInvitationRepository
import com.safemedi.app.sefemedi.domain.user.repository.UserRepository
import com.safemedi.app.sefemedi.global.error.BusinessException
import com.safemedi.app.sefemedi.global.error.ErrorCode
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.util.UriComponentsBuilder
import java.time.Clock
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

@Service
class FamilyInvitationCreateService(
    private val userRepository: UserRepository,
    private val familyInvitationRepository: FamilyInvitationRepository,
    private val tokenGenerator: FamilyInvitationTokenGenerator,
    private val tokenCipher: FamilyInvitationTokenCipher,
    private val clock: Clock,
    @Value("\${app.family-invitation.base-url}") baseUrl: String,
) {
    private val invitationBaseUrl = baseUrl.trimEnd('/').also {
        require(it.startsWith("https://")) { "가족 초대 링크 기본 URL은 HTTPS를 사용해야 합니다." }
    }

    @Transactional
    fun create(socialId: String): FamilyInvitationCreateResult {
        val inviter = userRepository.findBySocialIdForUpdate(socialId)
            ?: throw BusinessException(ErrorCode.USER_NOT_FOUND)
        val inviterId = inviter.id
            ?: throw BusinessException(ErrorCode.INTERNAL_SERVER_ERROR)
        val now = clock.instant()
        val existingInvitation = familyInvitationRepository
            .findFirstByInviter_IdAndStatusAndExpiresAtAfterOrderByCreatedAtDesc(
                inviterId = inviterId,
                status = FamilyInvitationStatus.PENDING,
                now = now,
            )

        if (existingInvitation != null) {
            log.info(
                "유효한 가족 초대 링크 재사용: invitationId={}, inviterId={}, expiresAt={}",
                existingInvitation.id,
                inviterId,
                existingInvitation.expiresAt,
            )
            return FamilyInvitationCreateResult(
                response = existingInvitation.toResponse(tokenCipher.decrypt(existingInvitation.encryptedToken)),
                created = false,
            )
        }

        val expiresAt = now.plus(INVITATION_VALID_HOURS, ChronoUnit.HOURS)
        val token = tokenGenerator.generate()
        val invitation = familyInvitationRepository.saveAndFlush(
            FamilyInvitation(
                inviter = inviter,
                tokenHash = token.hash,
                encryptedToken = tokenCipher.encrypt(token.rawValue),
                expiresAt = expiresAt,
            )
        )
        val invitationId = invitation.id
            ?: throw BusinessException(ErrorCode.INTERNAL_SERVER_ERROR)

        log.info(
            "가족 초대 링크 생성: invitationId={}, inviterId={}, expiresAt={}",
            invitationId,
            inviter.id,
            expiresAt,
        )

        return FamilyInvitationCreateResult(
            response = FamilyInvitationCreateResponse(
                invitationId = invitationId,
                inviteUrl = buildInviteUrl(token.rawValue),
                status = FamilyInvitationStatus.PENDING,
                createdAt = now,
                expiresAt = expiresAt,
            ),
            created = true,
        )
    }

    private fun FamilyInvitation.toResponse(rawToken: String): FamilyInvitationCreateResponse {
        val invitationId = id ?: throw BusinessException(ErrorCode.INTERNAL_SERVER_ERROR)
        val invitationCreatedAt = createdAt ?: throw BusinessException(ErrorCode.INTERNAL_SERVER_ERROR)
        return FamilyInvitationCreateResponse(
            invitationId = invitationId,
            inviteUrl = buildInviteUrl(rawToken),
            status = status,
            createdAt = invitationCreatedAt.toInstant(ZoneOffset.UTC),
            expiresAt = expiresAt,
        )
    }

    private fun buildInviteUrl(rawToken: String): String {
        return UriComponentsBuilder.fromUriString(invitationBaseUrl)
            .pathSegment(rawToken)
            .build()
            .toUriString()
    }

    private companion object {
        const val INVITATION_VALID_HOURS = 24L
        val log = LoggerFactory.getLogger(FamilyInvitationCreateService::class.java)
    }
}

data class FamilyInvitationCreateResult(
    val response: FamilyInvitationCreateResponse,
    val created: Boolean,
)
