package com.safemedi.app.sefemedi.domain.user.entity

import com.safemedi.app.sefemedi.domain.drug.entity.DrugMaster
import com.safemedi.app.sefemedi.global.entity.BaseTimeEntity
import jakarta.persistence.*

@Entity
@Table(name = "user_allergy")
class UserAllergy(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    val user: User,

    @Enumerated(EnumType.STRING)
    @Column(name = "allergy_type")
    var allergyType: AllergyType,

    @Column(name = "allergy_value", length = 50)
    var allergyValue: String,

    @Column(name = "allergy_name")
    var allergyName: String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registered_via_drug")
    var registeredViaDrug: DrugMaster? = null
) : BaseTimeEntity()
