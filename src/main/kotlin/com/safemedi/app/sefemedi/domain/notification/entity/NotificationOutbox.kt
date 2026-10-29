package com.safemedi.app.sefemedi.domain.notification.entity

import com.safemedi.app.sefemedi.domain.user.entity.User
import com.safemedi.app.sefemedi.global.entity.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "notification_outbox")
class NotificationOutbox(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notification_log_id", nullable = false)
    val notificationLog: NotificationLog,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @Column(name = "event_key", length = 255, nullable = false, unique = true)
    val eventKey: String,

    @Column(name = "scheduled_at", nullable = false)
    val scheduledAt: LocalDateTime,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    var status: NotificationOutboxStatus = NotificationOutboxStatus.PENDING,

    @Column(name = "retry_count", nullable = false)
    var retryCount: Int = 0,

    @Column(name = "next_retry_at")
    var nextRetryAt: LocalDateTime? = null,
) : BaseTimeEntity() {

    fun markProcessing() {
        status = NotificationOutboxStatus.PROCESSING
    }

    fun markSent() {
        status = NotificationOutboxStatus.SENT
        nextRetryAt = null
    }

    fun markRetryScheduled(
        nextRetryAt: LocalDateTime,
    ) {
        status = NotificationOutboxStatus.FAILED
        retryCount += 1
        this.nextRetryAt = nextRetryAt
    }

    fun markDead() {
        status = NotificationOutboxStatus.DEAD
        nextRetryAt = null
    }
}
