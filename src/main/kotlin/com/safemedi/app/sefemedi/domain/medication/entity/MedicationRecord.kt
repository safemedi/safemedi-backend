package com.safemedi.app.sefemedi.domain.medication.entity

import com.safemedi.app.sefemedi.domain.user.entity.User
import com.safemedi.app.sefemedi.global.entity.BaseTimeEntity
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "medication_record")
class MedicationRecord(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    val user: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prescription_id")
    val prescription: Prescription,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prescription_drug_time_id", nullable = false)
    val prescriptionDrugTime: PrescriptionDrugTime,

    @Column(name = "scheduled_at")
    var scheduledAt: LocalDateTime,

    @Column(name = "taken_at")
    var takenAt: LocalDateTime? = null,

    @Enumerated(EnumType.STRING)
    var status: MedicationStatus = MedicationStatus.PENDING
) : BaseTimeEntity() {
    fun updateStatus(
        status: MedicationStatus,
        takenAt: LocalDateTime?,
    ) {
        this.status = status
        this.takenAt = takenAt
    }
}
