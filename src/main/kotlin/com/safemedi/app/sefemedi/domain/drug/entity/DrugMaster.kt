package com.safemedi.app.sefemedi.domain.drug.entity

import jakarta.persistence.*

@Entity
@Table(name = "drug_master")
class DrugMaster(
    @Id
    @Column(name = "drug_code", length = 50)
    val drugCode: String,

    @Column(name = "drug_name", length = 1000)
    var drugName: String? = null,

    @Column(name = "atc_code", length = 20)
    var atcCode: String? = null
)
