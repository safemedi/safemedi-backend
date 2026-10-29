package com.safemedi.app.sefemedi.domain.drug.entity

import jakarta.persistence.*

@Entity
@Table(name = "ingredient_atc_map")
class IngredientAtcMap(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ingredient_code")
    val ingredient: IngredientMaster,

    @Column(name = "atc_code", length = 20)
    val atcCode: String
)
