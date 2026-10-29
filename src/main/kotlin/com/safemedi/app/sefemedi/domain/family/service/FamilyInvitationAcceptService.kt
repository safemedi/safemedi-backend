package com.safemedi.app.sefemedi.domain.family.service

import com.safemedi.app.sefemedi.domain.family.dto.FamilyInvitationAcceptResponse
import com.safemedi.app.sefemedi.domain.family.entity.Family
import com.safemedi.app.sefemedi.domain.family.entity.FamilyInvitationStatus
import com.safemedi.app.sefemedi.domain.family.repository.FamilyInvitationRepository
import com.safemedi.app.sefemedi.domain.family.repository.FamilyRepository
import com.safemedi.app.sefemedi.domain.notification.dto.NotificationCreateCommand
import com.safemedi.app.sefemedi.domain.notification.entity.NotificationTargetType
import com.safemedi.app.sefemedi.domain.notification.entity.NotificationType
import com.safemedi.app.sefemedi.domain.notification.service.NotificationCreateService
import com.safemedi.app.sefemedi.domain.user.entity.User
import com.safemedi.app.sefemedi.domain.user.repository.UserRepository
import com.safemedi.app.sefemedi.global.error.BusinessException
import com.safemedi.app.sefemedi.global.error.ErrorCode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneOffset

@Service
class FamilyInvitationAcceptService(
    private val userRepository: UserRepository,
    private val familyInvitationRepository: FamilyInvitationRepository,
    private val familyRepository: FamilyRepository,
    private val tokenHasher: FamilyInvitationTokenHasher,
    private val notificationCreateService: NotificationCreateService,
    private val clock: Clock,
) {

    @Transactional
    fun accept(
        socialId: String,
        token: String,
    ): FamilyInvitationAcceptResponse {
        val acceptingUser = userRepository.findBySocialId(socialId)
            ?: throw BusinessException(ErrorCode.INVALID_ACCESS_TOKEN)
        val invitation = familyInvitationRepository.findByTokenHashForUpdate(tokenHasher.hash(token))
            ?: throw BusinessException(ErrorCode.FAMILY_INVITATION_NOT_FOUND)
        val acceptedInstant = clock.instant()

        if (!invitation.expiresAt.isAfter(acceptedInstant)) {
            throw BusinessException(ErrorCode.FAMILY_INVITATION_EXPIRED)
        }
        if (invitation.status != FamilyInvitationStatus.PENDING) {
            throw BusinessException(ErrorCode.FAMILY_INVITATION_ALREADY_USED)
        }

        val inviter = invitation.inviter
        if (inviter.id == acceptingUser.id) {
            throw BusinessException(ErrorCode.FAMILY_INVITATION_SELF_ACCESS)
        }
        val inviterName = inviter.nickname
            ?: throw BusinessException(ErrorCode.FAMILY_INVITATION_INVITER_NAME_NOT_FOUND)
        val acceptingUserId = acceptingUser.id
            ?: throw BusinessException(ErrorCode.INTERNAL_SERVER_ERROR)
        val inviterId = inviter.id
            ?: throw BusinessException(ErrorCode.INTERNAL_SERVER_ERROR)

        if (isAlreadyConnected(acceptingUserId, inviterId)) {
            throw BusinessException(ErrorCode.FAMILY_INVITATION_ALREADY_CONNECTED)
        }

        invitation.accept(
            acceptedBy = acceptingUser,
            acceptedAt = LocalDateTime.ofInstant(acceptedInstant, ZoneOffset.UTC),
        )
        val acceptingUserFamily = familyRepository.save(
            Family(
                user = acceptingUser,
                connectedUser = inviter,
                relation = DEFAULT_RELATION,
            )
        )
        val inviterFamily = familyRepository.save(
            Family(
                user = inviter,
                connectedUser = acceptingUser,
                relation = DEFAULT_RELATION,
            )
        )

        val familyId = acceptingUserFamily.id
            ?: throw BusinessException(ErrorCode.INTERNAL_SERVER_ERROR)

        createFamilyConnectedNotification(
            inviter = inviter,
            acceptingUser = acceptingUser,
            inviterFamily = inviterFamily,
        )

        return FamilyInvitationAcceptResponse(
            familyId = familyId,
            name = inviterName,
            relation = DEFAULT_RELATION,
            connectedAt = acceptedInstant,
        )
    }

    private fun createFamilyConnectedNotification(
        inviter: User,
        acceptingUser: User,
        inviterFamily: Family,
    ) {
        val inviterId = inviter.id ?: throw BusinessException(ErrorCode.INTERNAL_SERVER_ERROR)
        val inviterFamilyId = inviterFamily.id ?: throw BusinessException(ErrorCode.INTERNAL_SERVER_ERROR)
        val content = acceptingUser.nickname
            ?.let { "${it}님과 가족으로 연결되었어요" }
            ?: "가족으로 연결되었어요"

        notificationCreateService.create(
            NotificationCreateCommand(
                userId = inviterId,
                type = NotificationType.FAMILY_CONNECTED,
                title = "가족 연결",
                content = content,
                targetType = NotificationTargetType.FAMILY,
                targetId = inviterFamilyId,
                deduplicationKey = "FAMILY_CONNECTED:FAMILY:$inviterFamilyId:$inviterId",
            )
        )
    }

    private fun isAlreadyConnected(
        acceptingUserId: Long,
        inviterId: Long,
    ): Boolean {
        return familyRepository.countConnectionsBetween(acceptingUserId, inviterId) > 0
    }

    private companion object {
        const val DEFAULT_RELATION = "가족"
    }
}
