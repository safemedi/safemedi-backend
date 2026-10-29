package com.safemedi.app.sefemedi.domain.drug.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "disease_master")
class DiseaseMaster(
    @Id
    @Column(name = "disease_code", length = 50)
    val diseaseCode: String,

    @Column(name = "disease_name")
    var diseaseName: String
)
