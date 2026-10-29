package com.safemedi.app.sefemedi.domain.user.repository

import com.safemedi.app.sefemedi.domain.user.entity.UserHealthProfile
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface UserHealthProfileRepository : JpaRepository<UserHealthProfile, Long> {
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        delete from UserHealthProfile uhp
        where uhp.user.id = :userId
        """
    )
    fun deleteByUserId(
        @Param("userId") userId: Long,
    ): Int
}
