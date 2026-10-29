package com.safemedi.app.sefemedi.domain.notification.entity

enum class NotificationOutboxStatus {
    PENDING,
    PROCESSING,
    SENT,
    FAILED,
    DEAD,
}
