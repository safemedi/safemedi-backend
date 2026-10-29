package com.safemedi.app.sefemedi.domain.user.entity

import com.safemedi.app.sefemedi.global.entity.BaseTimeEntity
import jakarta.persistence.*
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDate

@Entity
@Table(name = "user_health_profile")
@EntityListeners(AuditingEntityListener::class)
class UserHealthProfile(
    @Id
    @Column(name = "user_id")
    val userId: Long? = null,

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    val user: User,

    @Column(name = "birth_date")
    var birthDate: LocalDate? = null,

    @Enumerated(EnumType.STRING)
    var gender: Gender? = null,

    var height: Int? = null,
    var weight: Int? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "blood_type")
    var bloodType: BloodType? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "rh_type")
    var rhType: RhType = RhType.PLUS
) : BaseTimeEntity()
