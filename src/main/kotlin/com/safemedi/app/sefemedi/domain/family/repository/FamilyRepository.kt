package com.safemedi.app.sefemedi.domain.family.repository

import com.safemedi.app.sefemedi.domain.family.entity.Family
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface FamilyRepository : JpaRepository<Family, Long> {

    @Query(
        """
        select count(f)
        from Family f
        where (f.user.id = :firstUserId and f.connectedUser.id = :secondUserId)
           or (f.user.id = :secondUserId and f.connectedUser.id = :firstUserId)
        """
    )
    fun countConnectionsBetween(
        @Param("firstUserId") firstUserId: Long,
        @Param("secondUserId") secondUserId: Long,
    ): Long

    @EntityGraph(attributePaths = ["connectedUser"], type = EntityGraph.EntityGraphType.FETCH)
    fun findAllByUser_IdOrderByCreatedAtAsc(
        userId: Long,
    ): List<Family>

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        delete from Family f
        where f.user.id = :userId
           or f.connectedUser.id = :userId
        """
    )
    fun deleteAllByUserIdOrConnectedUserId(
        @Param("userId") userId: Long,
    ): Int

    @EntityGraph(attributePaths = ["connectedUser"], type = EntityGraph.EntityGraphType.FETCH)
    fun findWithConnectedUserById(id: Long): Family?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
        """
        select f
        from Family f
        where (f.user.id = :firstUserId and f.connectedUser.id = :secondUserId)
           or (f.user.id = :secondUserId and f.connectedUser.id = :firstUserId)
        order by f.id asc
        """
    )
    fun findConnectionsBetweenForUpdate(
        @Param("firstUserId") firstUserId: Long,
        @Param("secondUserId") secondUserId: Long,
    ): List<Family>

    @EntityGraph(attributePaths = ["connectedUser"], type = EntityGraph.EntityGraphType.FETCH)
    fun findByIdAndUser_Id(
        id: Long,
        userId: Long,
    ): Family?

    fun findByUser_IdAndConnectedUser_Id(
        userId: Long,
        connectedUserId: Long,
    ): Family?
}
