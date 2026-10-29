package com.safemedi.app.sefemedi.domain.medication.entity

import com.safemedi.app.sefemedi.domain.user.entity.User
import com.safemedi.app.sefemedi.global.entity.BaseTimeEntity
import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "prescription")
class Prescription(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    val user: User,

    @Column(name = "title", length = 255)
    var title: String,

    @Column(name = "has_allergy_conflict")
    var hasAllergyConflict: Boolean = false,

    @Column(name = "is_doctor_approved")
    var isDoctorApproved: Boolean = false,

    @Column(name = "start_date")
    var startDate: LocalDate,

    @Column(name = "end_date")
    var endDate: LocalDate,

    @Column(name = "deleted_at")
    var deletedAt: LocalDateTime? = null
) : BaseTimeEntity() {
    fun updateTitle(title: String) {
        this.title = title
    }

    fun delete(deletedAt: LocalDateTime) {
        this.deletedAt = deletedAt
    }
}
