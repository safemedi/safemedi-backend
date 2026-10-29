package com.safemedi.app.sefemedi.domain.notification.fcm

import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingException
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.MessagingErrorCode
import com.google.firebase.messaging.Notification
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(prefix = "firebase", name = ["enabled"], havingValue = "true")
class FirebaseNotificationPushSender(
    private val firebaseMessaging: FirebaseMessaging,
) : NotificationPushSender {

    override fun send(
        request: NotificationPushRequest,
    ): NotificationPushResult {
        val message = Message.builder()
            .setToken(request.token)
            .setNotification(
                Notification.builder()
                    .setTitle(request.title)
                    .setBody(request.body)
                    .build()
            )
            .putAllData(request.data)
            .build()

        return try {
            firebaseMessaging.send(message)
            NotificationPushResult(NotificationPushStatus.SUCCESS)
        } catch (exception: FirebaseMessagingException) {
            val errorCode = exception.messagingErrorCode?.name ?: exception.errorCode?.name
            NotificationPushResult(
                status = classify(exception),
                errorCode = errorCode,
            )
        }
    }

    private fun classify(
        exception: FirebaseMessagingException,
    ): NotificationPushStatus {
        return when (exception.messagingErrorCode) {
            MessagingErrorCode.UNREGISTERED -> NotificationPushStatus.INVALID_TOKEN
            MessagingErrorCode.UNAVAILABLE,
            MessagingErrorCode.INTERNAL,
            MessagingErrorCode.QUOTA_EXCEEDED -> NotificationPushStatus.TEMPORARY_FAILURE
            else -> NotificationPushStatus.PERMANENT_FAILURE
        }
    }
}
