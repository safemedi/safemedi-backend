package com.safemedi.app.sefemedi.domain.drug.entity

import jakarta.persistence.*

@Entity
@Table(name = "atc_group_master")
class AtcGroupMaster(
    @Id
    @Column(name = "atc_code", length = 20)
    val atcCode: String,

    @Column(name = "atc_name_ko")
    var atcNameKo: String? = null,

    @Column(name = "atc_name_en")
    var atcNameEn: String? = null,

    @Column(name = "atc_level")
    var atcLevel: Int? = null
)
