package com.safemedi.app.sefemedi.domain.notification.fcm

interface NotificationPushSender {

    fun send(
        request: NotificationPushRequest,
    ): NotificationPushResult
}

data class NotificationPushRequest(
    val token: String,
    val title: String,
    val body: String,
    val data: Map<String, String> = emptyMap(),
)

data class NotificationPushResult(
    val status: NotificationPushStatus,
    val errorCode: String? = null,
)

enum class NotificationPushStatus {
    SUCCESS,
    INVALID_TOKEN,
    TEMPORARY_FAILURE,
    PERMANENT_FAILURE,
}
