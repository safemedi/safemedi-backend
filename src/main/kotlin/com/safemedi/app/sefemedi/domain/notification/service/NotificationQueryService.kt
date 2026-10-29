package com.safemedi.app.sefemedi.domain.notification.service

import com.safemedi.app.sefemedi.domain.notification.dto.NotificationListResponse
import com.safemedi.app.sefemedi.domain.notification.dto.NotificationSummaryResponse
import com.safemedi.app.sefemedi.domain.notification.dto.NotificationUnreadCountResponse
import com.safemedi.app.sefemedi.domain.notification.repository.NotificationLogRepository
import com.safemedi.app.sefemedi.domain.user.repository.UserRepository
import com.safemedi.app.sefemedi.global.error.BusinessException
import com.safemedi.app.sefemedi.global.error.ErrorCode
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.ZoneOffset

@Service
class NotificationQueryService(
    private val userRepository: UserRepository,
    private val notificationLogRepository: NotificationLogRepository,
) {

    @Transactional(readOnly = true)
    fun findNotifications(
        socialId: String,
        page: Int,
        size: Int,
    ): NotificationListResponse {
        validatePageRequest(page, size)

        val user = userRepository.findBySocialId(socialId)
            ?: throw BusinessException(ErrorCode.INVALID_TOKEN)
        val userId = user.id ?: throw BusinessException(ErrorCode.INTERNAL_SERVER_ERROR)
        val notifications = notificationLogRepository.findByUserId(
            userId = userId,
            pageable = PageRequest.of(page, size, NOTIFICATION_SORT),
        )

        return NotificationListResponse(
            content = notifications.content.map {
                NotificationSummaryResponse(
                    notificationId = it.id ?: throw BusinessException(ErrorCode.INTERNAL_SERVER_ERROR),
                    type = it.type,
                    title = it.title,
                    message = it.message,
                    isRead = it.isRead,
                    targetType = it.targetType,
                    targetId = it.targetId,
                    createdAt = it.createdAt
                        ?.toInstant(ZoneOffset.UTC)
                        ?: throw BusinessException(ErrorCode.INTERNAL_SERVER_ERROR),
                )
            },
            page = page,
            size = size,
            isLast = notifications.isLast,
        )
    }

    @Transactional(readOnly = true)
    fun countUnreadNotifications(
        socialId: String,
    ): NotificationUnreadCountResponse {
        val user = userRepository.findBySocialId(socialId)
            ?: throw BusinessException(ErrorCode.INVALID_TOKEN)
        val userId = user.id ?: throw BusinessException(ErrorCode.INTERNAL_SERVER_ERROR)

        return NotificationUnreadCountResponse(
            unreadCount = notificationLogRepository.countByUserIdAndIsReadFalse(userId),
        )
    }

    private fun validatePageRequest(
        page: Int,
        size: Int,
    ) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw BusinessException(ErrorCode.INVALID_PAGE_REQUEST)
        }
    }

    companion object {
        private const val MAX_PAGE_SIZE = 100
        private val NOTIFICATION_SORT = Sort.by(Sort.Direction.DESC, "createdAt", "id")
    }
}
