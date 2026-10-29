package com.safemedi.app.sefemedi.domain.medication.entity

import jakarta.persistence.*
import java.time.LocalDateTime
import java.time.LocalTime

@Entity
@Table(name = "prescription_drug_time")
class PrescriptionDrugTime(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prescription_drug_id")
    val prescriptionDrug: PrescriptionDrug,

    @Column(name = "take_time")
    var takeTime: LocalTime,

    @Column(name = "deleted_at")
    var deletedAt: LocalDateTime? = null
) {
    fun delete(deletedAt: LocalDateTime) {
        this.deletedAt = deletedAt
    }
}
