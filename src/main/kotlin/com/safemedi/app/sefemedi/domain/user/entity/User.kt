package com.safemedi.app.sefemedi.domain.user.entity

import com.safemedi.app.sefemedi.global.entity.BaseTimeEntity
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "user")
class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(length = 50)
    var nickname: String? = null,

    @Suppress("unused")
    @Enumerated(EnumType.STRING)
    @Column(name = "social_provider")
    val socialProvider: SocialProvider? = null,

    @Column(name = "social_id", unique = true)
    val socialId: String? = null,

    @Column(name = "invite_code", length = 20)
    var inviteCode: String? = null,

    @Column(name = "is_tutorial_completed")
    var isTutorialCompleted: Boolean = false,

    @Suppress("SqlResolve", "JpaDataSourceORMInspection")
    @Column(name = "deleted_at")
    var deletedAt: LocalDateTime? = null
) : BaseTimeEntity() {
    fun withdraw(withdrawnAt: LocalDateTime) {
        nickname = null
        inviteCode = null
        isTutorialCompleted = false
        deletedAt = withdrawnAt
    }

    fun reactivate() {
        deletedAt = null
        isTutorialCompleted = false
    }
}
