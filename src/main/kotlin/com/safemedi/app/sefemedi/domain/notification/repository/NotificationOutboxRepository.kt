package com.safemedi.app.sefemedi.domain.notification.repository

import com.safemedi.app.sefemedi.domain.notification.entity.NotificationOutbox
import com.safemedi.app.sefemedi.domain.notification.entity.NotificationOutboxStatus
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface NotificationOutboxRepository : JpaRepository<NotificationOutbox, Long> {

    @Query(
        """
        select o
        from NotificationOutbox o
        join fetch o.notificationLog n
        join fetch o.user u
        where (o.status = :pendingStatus and o.scheduledAt <= :now)
           or (o.status = :failedStatus and o.nextRetryAt <= :now)
        order by o.scheduledAt asc, o.id asc
        """
    )
    fun findDueOutboxes(
        @Param("pendingStatus") pendingStatus: NotificationOutboxStatus,
        @Param("failedStatus") failedStatus: NotificationOutboxStatus,
        @Param("now") now: LocalDateTime,
        pageable: Pageable,
    ): List<NotificationOutbox>

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        delete from NotificationOutbox o
        where o.user.id = :userId
        """
    )
    fun deleteAllByUserId(
        @Param("userId") userId: Long,
    ): Int
}
