package com.safemedi.app.sefemedi.domain.user.entity

import com.safemedi.app.sefemedi.domain.drug.entity.DiseaseMaster
import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

@Entity
@Table(name = "user_disease_map")
@EntityListeners(AuditingEntityListener::class)
class UserDiseaseMap(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    val user: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "disease_code")
    val disease: DiseaseMaster
) {
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    var createdAt: LocalDateTime? = null
        protected set
}
