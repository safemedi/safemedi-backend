package com.safemedi.app.sefemedi.domain.notification.service

import com.safemedi.app.sefemedi.domain.notification.dto.NotificationReadResponse
import com.safemedi.app.sefemedi.domain.notification.dto.NotificationReadAllResponse
import com.safemedi.app.sefemedi.domain.notification.repository.NotificationLogRepository
import com.safemedi.app.sefemedi.domain.user.repository.UserRepository
import com.safemedi.app.sefemedi.global.error.BusinessException
import com.safemedi.app.sefemedi.global.error.ErrorCode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class NotificationReadService(
    private val userRepository: UserRepository,
    private val notificationLogRepository: NotificationLogRepository,
) {

    @Transactional
    fun markAsRead(
        socialId: String,
        notificationId: Long,
    ): NotificationReadResponse {
        val user = userRepository.findBySocialId(socialId)
            ?: throw BusinessException(ErrorCode.INVALID_TOKEN)
        val userId = user.id ?: throw BusinessException(ErrorCode.INTERNAL_SERVER_ERROR)
        val notification = notificationLogRepository.findById(notificationId)
            .orElseThrow { BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND) }
        val notificationUserId = notification.user.id
            ?: throw BusinessException(ErrorCode.INTERNAL_SERVER_ERROR)

        if (notificationUserId != userId) {
            throw BusinessException(ErrorCode.NOTIFICATION_ACCESS_DENIED)
        }

        notification.markAsRead()

        return NotificationReadResponse(
            notificationId = notification.id ?: throw BusinessException(ErrorCode.INTERNAL_SERVER_ERROR),
            isRead = notification.isRead,
        )
    }

    @Transactional
    fun markAllAsRead(
        socialId: String,
    ): NotificationReadAllResponse {
        val user = userRepository.findBySocialId(socialId)
            ?: throw BusinessException(ErrorCode.INVALID_TOKEN)
        val userId = user.id ?: throw BusinessException(ErrorCode.INTERNAL_SERVER_ERROR)

        return NotificationReadAllResponse(
            updatedCount = notificationLogRepository.markAllAsReadByUserId(userId),
        )
    }
}
