package com.safemedi.app.sefemedi.domain.user.entity

import com.safemedi.app.sefemedi.global.entity.BaseTimeEntity
import jakarta.persistence.*

@Entity
@Table(name = "user_device")
class UserDevice(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    var user: User,

    @Column(name = "device_token", length = 512, unique = true)
    var deviceToken: String,

    @Column(name = "device_type", length = 20)
    var deviceType: String,

    @Column(name = "is_my_reminder_on")
    var isMyReminderOn: Boolean = true,

    @Column(name = "is_family_reminder_on")
    var isFamilyReminderOn: Boolean = true,

    @Column(name = "is_missed_alert_on")
    var isMissedAlertOn: Boolean = true,

    @Column(name = "is_active")
    var isActive: Boolean = true
) : BaseTimeEntity() {
    fun register(
        user: User,
        deviceType: String,
    ) {
        if (this.user.id != user.id) {
            this.isMyReminderOn = true
            this.isFamilyReminderOn = true
            this.isMissedAlertOn = true
        }

        this.user = user
        this.deviceType = deviceType
        this.isActive = true
    }

    fun deactivate() {
        this.isActive = false
    }

    fun updateNotificationSettings(
        isMyReminderOn: Boolean?,
        isFamilyReminderOn: Boolean?,
        isMissedAlertOn: Boolean?,
    ) {
        isMyReminderOn?.let { this.isMyReminderOn = it }
        isFamilyReminderOn?.let { this.isFamilyReminderOn = it }
        isMissedAlertOn?.let { this.isMissedAlertOn = it }
    }
}
