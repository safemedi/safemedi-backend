package com.safemedi.app.sefemedi.domain.drug.entity

import jakarta.persistence.*

@Entity
@Table(name = "drug_ingredient_map")
class DrugIngredientMap(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "drug_code")
    val drug: DrugMaster,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ingredient_code")
    val ingredient: IngredientMaster
)
