package com.safemedi.app.sefemedi.domain.drug.repository

import com.safemedi.app.sefemedi.domain.drug.entity.DurAge
import com.safemedi.app.sefemedi.domain.drug.entity.DurElderly
import com.safemedi.app.sefemedi.domain.drug.entity.DurInteraction
import com.safemedi.app.sefemedi.domain.drug.entity.DurPregnancy
import com.safemedi.app.sefemedi.domain.drug.entity.DurCoverage
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface DurAgeRepository : JpaRepository<DurAge, Long> {
    fun findByDrugCodeInAndRuntimeVerifiedTrue(drugCodes: Collection<String>): List<DurAge>
}

interface DurElderlyRepository : JpaRepository<DurElderly, Long> {
    fun findByDrugCodeInAndRuntimeVerifiedTrue(drugCodes: Collection<String>): List<DurElderly>
}

interface DurPregnancyRepository : JpaRepository<DurPregnancy, Long> {
    fun findByDrugCodeInAndRuntimeVerifiedTrue(drugCodes: Collection<String>): List<DurPregnancy>
}

interface DurInteractionRepository : JpaRepository<DurInteraction, Long> {
    @Query(
        """
        select di
        from DurInteraction di
        where di.drugCodeA in :drugCodes
          and di.drugCodeB in :drugCodes
          and di.runtimeVerified = true
        """
    )
    fun findInteractions(
        @Param("drugCodes") drugCodes: Collection<String>,
    ): List<DurInteraction>
}

interface DurCoverageRepository : JpaRepository<DurCoverage, String> {
    fun findByDrugCodeInAndVerifiedTrue(drugCodes: Collection<String>): List<DurCoverage>
}
