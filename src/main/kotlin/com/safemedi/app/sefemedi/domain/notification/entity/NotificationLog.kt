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

@Entity
@Table(name = "notification_log")
class NotificationLog(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    val user: User,

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    var type: NotificationType,

    @Column(length = 255)
    var title: String,

    @Column(columnDefinition = "text")
    var message: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", length = 50)
    var targetType: NotificationTargetType? = null,

    @Column(name = "target_id")
    var targetId: Long? = null,

    @Column(name = "deduplication_key", length = 255)
    var deduplicationKey: String? = null,

    @Column(name = "is_read")
    var isRead: Boolean = false
) : BaseTimeEntity() {

    fun markAsRead() {
        isRead = true
    }
}
