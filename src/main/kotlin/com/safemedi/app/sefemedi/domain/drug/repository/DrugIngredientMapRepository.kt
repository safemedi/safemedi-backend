package com.safemedi.app.sefemedi.domain.drug.repository

import com.safemedi.app.sefemedi.domain.drug.entity.DrugIngredientMap
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface DrugIngredientMapRepository : JpaRepository<DrugIngredientMap, Long> {
    @Query(
        """
        select dim
        from DrugIngredientMap dim
        join fetch dim.drug d
        join fetch dim.ingredient i
        where d.drugCode in :drugCodes
        """
    )
    fun findAllByDrugCodes(
        @Param("drugCodes") drugCodes: Collection<String>,
    ): List<DrugIngredientMap>
}
