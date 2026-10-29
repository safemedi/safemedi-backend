package com.safemedi.app.sefemedi.domain.user.repository

import com.safemedi.app.sefemedi.domain.user.entity.UserDiseaseMap
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface UserDiseaseMapRepository : JpaRepository<UserDiseaseMap, Long> {

    @EntityGraph(attributePaths = ["disease"], type = EntityGraph.EntityGraphType.FETCH)
    fun findAllByUser_IdOrderByCreatedAtAsc(
        userId: Long,
    ): List<UserDiseaseMap>

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        delete from UserDiseaseMap udm
        where udm.user.id = :userId
        """
    )
    fun deleteAllByUserId(
        @Param("userId") userId: Long,
    ): Int
}
