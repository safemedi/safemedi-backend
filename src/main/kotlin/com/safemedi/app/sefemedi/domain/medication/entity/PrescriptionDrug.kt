package com.safemedi.app.sefemedi.domain.medication.entity

import com.safemedi.app.sefemedi.domain.drug.entity.DrugMaster
import jakarta.persistence.*

@Entity
@Table(name = "prescription_drug")
class PrescriptionDrug(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prescription_id")
    val prescription: Prescription,

    @Column(name = "drug_name", length = 1000, nullable = false)
    var drugName: String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "drug_code")
    var drug: DrugMaster? = null,

    @Column(name = "atc_code", length = 20)
    var atcCode: String? = null
)
