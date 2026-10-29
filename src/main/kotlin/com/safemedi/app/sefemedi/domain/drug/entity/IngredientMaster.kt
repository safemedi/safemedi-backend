package com.safemedi.app.sefemedi.domain.drug.entity

import jakarta.persistence.*

@Entity
@Table(name = "ingredient_master")
class IngredientMaster(
    @Id
    @Column(name = "ingredient_code", length = 50)
    val ingredientCode: String,

    @Column(name = "ingredient_name", length = 1000)
    var ingredientName: String? = null
)
