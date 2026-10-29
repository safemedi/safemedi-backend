package com.safemedi.app.sefemedi.domain.drug.repository

import com.safemedi.app.sefemedi.domain.drug.entity.AtcGroupMaster
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface AtcGroupMasterRepository : JpaRepository<AtcGroupMaster, String> {
    @Query(
        """
        select a
        from AtcGroupMaster a
        where a.atcNameKo like concat('%', :keyword, '%')
           or a.atcNameEn like concat('%', :keyword, '%')
        order by coalesce(a.atcNameKo, a.atcNameEn) asc
        """
    )
    fun searchByKeyword(
        @Param("keyword") keyword: String,
        pageable: Pageable,
    ): Slice<AtcGroupMaster>
}
