package com.safemedi.app.sefemedi.domain.family.service

import com.safemedi.app.sefemedi.domain.family.dto.FamilyInvitationInfoResponse
import com.safemedi.app.sefemedi.domain.family.entity.FamilyInvitationStatus
import com.safemedi.app.sefemedi.domain.family.repository.FamilyInvitationRepository
import com.safemedi.app.sefemedi.domain.user.repository.UserRepository
import com.safemedi.app.sefemedi.global.error.BusinessException
import com.safemedi.app.sefemedi.global.error.ErrorCode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock

@Service
class FamilyInvitationQueryService(
    private val userRepository: UserRepository,
    private val familyInvitationRepository: FamilyInvitationRepository,
    private val tokenHasher: FamilyInvitationTokenHasher,
    private val clock: Clock,
) {

    @Transactional(readOnly = true)
    fun getInvitationInfo(
        socialId: String,
        token: String,
    ): FamilyInvitationInfoResponse {
        val currentUser = userRepository.findBySocialId(socialId)
            ?: throw BusinessException(ErrorCode.INVALID_ACCESS_TOKEN)
        val invitation = familyInvitationRepository.findByTokenHash(tokenHasher.hash(token))
            ?: throw BusinessException(ErrorCode.FAMILY_INVITATION_NOT_FOUND)
        val now = clock.instant()

        if (!invitation.expiresAt.isAfter(now)) {
            throw BusinessException(ErrorCode.FAMILY_INVITATION_EXPIRED)
        }
        if (invitation.status != FamilyInvitationStatus.PENDING) {
            throw BusinessException(ErrorCode.FAMILY_INVITATION_ALREADY_USED)
        }
        if (invitation.inviter.id == currentUser.id) {
            throw BusinessException(ErrorCode.FAMILY_INVITATION_SELF_ACCESS)
        }

        val inviterName = invitation.inviter.nickname
            ?: throw BusinessException(ErrorCode.FAMILY_INVITATION_INVITER_NAME_NOT_FOUND)

        return FamilyInvitationInfoResponse(
            inviterName = inviterName,
            expiresAt = invitation.expiresAt,
        )
    }
}
