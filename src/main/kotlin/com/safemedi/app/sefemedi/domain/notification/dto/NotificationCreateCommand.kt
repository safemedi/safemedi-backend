package com.safemedi.app.sefemedi.domain.notification.dto

import com.safemedi.app.sefemedi.domain.notification.entity.NotificationTargetType
import com.safemedi.app.sefemedi.domain.notification.entity.NotificationType
import java.time.LocalDateTime

data class NotificationCreateCommand(
    val userId: Long,
    val type: NotificationType,
    val title: String,
    val content: String,
    val targetType: NotificationTargetType? = null,
    val targetId: Long? = null,
    val deduplicationKey: String,
    val scheduledAt: LocalDateTime? = null,
)
