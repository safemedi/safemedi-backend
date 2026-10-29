package com.safemedi.app.sefemedi.domain.user.repository

import com.safemedi.app.sefemedi.domain.user.entity.UserAllergy
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface UserAllergyRepository : JpaRepository<UserAllergy, Long> {

    fun findAllByUser_IdOrderByCreatedAtAsc(
        userId: Long,
    ): List<UserAllergy>

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        delete from UserAllergy ua
        where ua.user.id = :userId
        """
    )
    fun deleteAllByUserId(
        @Param("userId") userId: Long,
    ): Int
}
