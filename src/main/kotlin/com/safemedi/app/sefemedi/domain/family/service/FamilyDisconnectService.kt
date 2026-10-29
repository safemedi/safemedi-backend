package com.safemedi.app.sefemedi.domain.family.service

import com.safemedi.app.sefemedi.domain.family.repository.FamilyRepository
import com.safemedi.app.sefemedi.domain.notification.dto.NotificationCreateCommand
import com.safemedi.app.sefemedi.domain.notification.entity.NotificationType
import com.safemedi.app.sefemedi.domain.notification.service.NotificationCreateService
import com.safemedi.app.sefemedi.domain.user.entity.User
import com.safemedi.app.sefemedi.domain.user.repository.UserRepository
import com.safemedi.app.sefemedi.global.error.BusinessException
import com.safemedi.app.sefemedi.global.error.ErrorCode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class FamilyDisconnectService(
    private val userRepository: UserRepository,
    private val familyRepository: FamilyRepository,
    private val notificationCreateService: NotificationCreateService,
) {

    @Transactional
    fun disconnect(
        socialId: String,
        familyId: Long,
    ) {
        val currentUser = userRepository.findBySocialId(socialId)
            ?: throw BusinessException(ErrorCode.INVALID_ACCESS_TOKEN)
        val currentUserId = currentUser.id
            ?: throw BusinessException(ErrorCode.INTERNAL_SERVER_ERROR)
        val family = familyRepository.findWithConnectedUserById(familyId)
            ?: throw BusinessException(ErrorCode.FAMILY_NOT_FOUND)

        if (family.user.id != currentUserId) {
            throw BusinessException(ErrorCode.FAMILY_ACCESS_DENIED)
        }

        val connectedUserId = family.connectedUser.id
            ?: throw BusinessException(ErrorCode.INTERNAL_SERVER_ERROR)
        val firstUserId = minOf(currentUserId, connectedUserId)
        val secondUserId = maxOf(currentUserId, connectedUserId)
        val connections = familyRepository.findConnectionsBetweenForUpdate(
            firstUserId = firstUserId,
            secondUserId = secondUserId,
        )

        if (connections.none { it.id == familyId }) {
            throw BusinessException(ErrorCode.FAMILY_NOT_FOUND)
        }

        createFamilyDisconnectedNotification(
            currentUser = currentUser,
            connectedUser = family.connectedUser,
            familyId = familyId,
        )

        familyRepository.deleteAll(connections)
    }

    private fun createFamilyDisconnectedNotification(
        currentUser: User,
        connectedUser: User,
        familyId: Long,
    ) {
        val connectedUserId = connectedUser.id ?: throw BusinessException(ErrorCode.INTERNAL_SERVER_ERROR)
        val content = currentUser.nickname
            ?.let { "${it}님과의 가족 연결이 해제되었어요" }
            ?: "가족 연결이 해제되었어요"

        notificationCreateService.create(
            NotificationCreateCommand(
                userId = connectedUserId,
                type = NotificationType.FAMILY_DISCONNECTED,
                title = "가족 연결 해제",
                content = content,
                deduplicationKey = "FAMILY_DISCONNECTED:FAMILY:$familyId:$connectedUserId",
            )
        )
    }
}
