package com.safemedi.app.sefemedi.domain.family.entity

import com.safemedi.app.sefemedi.domain.user.entity.User
import com.safemedi.app.sefemedi.global.entity.BaseTimeEntity
import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

@Entity
@Table(name = "family")
@EntityListeners(AuditingEntityListener::class)
class Family(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    val user: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "connected_user_id")
    val connectedUser: User,

    @Column(length = 50)
    var relation: String,

    @Column(name = "is_allow_my_info")
    var isAllowMyInfo: Boolean = true,

    @Column(name = "is_receive_alert")
    var isReceiveAlert: Boolean = true
) : BaseTimeEntity()