package com.safemedi.app.sefemedi.domain.auth.repository

import com.safemedi.app.sefemedi.domain.auth.entity.RefreshToken
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface RefreshTokenRepository : JpaRepository<RefreshToken, Long> {

    fun findByUser_Id(
        userId: Long
    ): RefreshToken?

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        delete from RefreshToken rt
        where rt.user.id = :userId
        """
    )
    fun deleteByUserId(
        @Param("userId") userId: Long,
    ): Int
}
