package com.safemedi.app.sefemedi.domain.drug.repository

import com.safemedi.app.sefemedi.domain.drug.entity.DiseaseMaster
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.data.jpa.repository.JpaRepository

interface DiseaseMasterRepository : JpaRepository<DiseaseMaster, String> {
    fun findByDiseaseNameContainingOrderByDiseaseNameAsc(
        keyword: String,
        pageable: Pageable,
    ): Slice<DiseaseMaster>
}
