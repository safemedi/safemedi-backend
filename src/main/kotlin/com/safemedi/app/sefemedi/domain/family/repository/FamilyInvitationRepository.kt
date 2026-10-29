package com.safemedi.app.sefemedi.domain.family.repository

import com.safemedi.app.sefemedi.domain.family.entity.FamilyInvitation
import com.safemedi.app.sefemedi.domain.family.entity.FamilyInvitationStatus
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant

interface FamilyInvitationRepository : JpaRepository<FamilyInvitation, Long> {
    @EntityGraph(attributePaths = ["inviter"])
    fun findByTokenHash(tokenHash: String): FamilyInvitation?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = ["inviter"])
    @Query("select fi from FamilyInvitation fi where fi.tokenHash = :tokenHash")
    fun findByTokenHashForUpdate(
        @Param("tokenHash") tokenHash: String,
    ): FamilyInvitation?

    fun findFirstByInviter_IdAndStatusAndExpiresAtAfterOrderByCreatedAtDesc(
        inviterId: Long,
        status: FamilyInvitationStatus,
        now: Instant,
    ): FamilyInvitation?
}
