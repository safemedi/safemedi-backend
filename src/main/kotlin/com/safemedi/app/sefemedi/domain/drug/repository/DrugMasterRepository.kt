package com.safemedi.app.sefemedi.domain.drug.repository

import com.safemedi.app.sefemedi.domain.drug.entity.DrugMaster
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.data.jpa.repository.JpaRepository

interface DrugMasterRepository : JpaRepository<DrugMaster, String> {
    fun findByDrugNameContainingAndAtcCodeIsNotNullOrderByDrugNameAsc(
        keyword: String,
        pageable: Pageable,
    ): Slice<DrugMaster>
}
