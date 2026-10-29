package com.safemedi.app.sefemedi.domain.notification.dto

import com.safemedi.app.sefemedi.domain.notification.entity.NotificationTargetType
import com.safemedi.app.sefemedi.domain.notification.entity.NotificationType
import java.time.Instant

data class NotificationListResponse(
    val content: List<NotificationSummaryResponse>,
    val page: Int,
    val size: Int,
    val isLast: Boolean,
)

data class NotificationSummaryResponse(
    val notificationId: Long,
    val type: NotificationType,
    val title: String,
    val message: String,
    val isRead: Boolean,
    val targetType: NotificationTargetType?,
    val targetId: Long?,
    val createdAt: Instant,
)
