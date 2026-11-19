package com.safemedi.app.sefemedi.domain.drug.repository

import com.safemedi.app.sefemedi.domain.drug.entity.DrugMaster
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface DrugMasterRepository : JpaRepository<DrugMaster, String> {
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    fun findByDrugNameStartingWith(prefix: String, pageable: Pageable): Slice<DrugMaster>

    @Query(
        """
        select d from DrugMaster d
        where d.drugName like concat('%', :keyword, '%')
          and d.atcCode is not null
          and trim(d.atcCode) <> ''
          and (
              d.drugName not like '%(수출용)%'
              or not exists (
                  select regular.drugCode from DrugMaster regular
                  where regular.drugName = trim(substring(d.drugName, 1, locate('(수출용)', d.drugName) - 1))
                    and regular.atcCode = d.atcCode
              )
          )
          and d.drugCode = (
              select min(duplicate.drugCode) from DrugMaster duplicate
              where duplicate.drugName = d.drugName
                and duplicate.atcCode = d.atcCode
          )
        order by d.drugName asc, d.drugCode asc
        """
    )
    fun searchDistinctByDrugNameAndAtcCode(
        @Param("keyword") keyword: String,
        pageable: Pageable,
    ): Slice<DrugMaster>
}
