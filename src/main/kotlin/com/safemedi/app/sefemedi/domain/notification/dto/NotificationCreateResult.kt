package com.safemedi.app.sefemedi.domain.notification.dto

import com.safemedi.app.sefemedi.domain.notification.entity.NotificationLog

data class NotificationCreateResult(
    val status: NotificationCreateStatus,
    val notification: NotificationLog? = null,
)

enum class NotificationCreateStatus {
    CREATED,
    DUPLICATED,
    SKIPPED_BY_SETTINGS,
    SKIPPED_NO_ACTIVE_DEVICE,
}
