package com.safemedi.app.sefemedi.domain.drug.repository

import com.safemedi.app.sefemedi.domain.drug.entity.DurAge
import com.safemedi.app.sefemedi.domain.drug.entity.DurElderly
import com.safemedi.app.sefemedi.domain.drug.entity.DurInteraction
import com.safemedi.app.sefemedi.domain.drug.entity.DurPregnancy
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface DurAgeRepository : JpaRepository<DurAge, Long> {
    fun findByDrugNameIn(drugNames: Collection<String>): List<DurAge>
}

interface DurElderlyRepository : JpaRepository<DurElderly, Long> {
    fun findByDrugNameIn(drugNames: Collection<String>): List<DurElderly>
}

interface DurPregnancyRepository : JpaRepository<DurPregnancy, Long> {
    fun findByDrugNameIn(drugNames: Collection<String>): List<DurPregnancy>
}

interface DurInteractionRepository : JpaRepository<DurInteraction, Long> {
    @Query(
        """
        select di
        from DurInteraction di
        where di.drugNameA in :drugNames
          and di.drugNameB in :drugNames
        """
    )
    fun findInteractions(
        @Param("drugNames") drugNames: Collection<String>,
    ): List<DurInteraction>
}
