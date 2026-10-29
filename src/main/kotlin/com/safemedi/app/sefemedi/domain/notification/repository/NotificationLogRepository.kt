package com.safemedi.app.sefemedi.domain.notification.repository

import com.safemedi.app.sefemedi.domain.notification.entity.NotificationLog
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface NotificationLogRepository : JpaRepository<NotificationLog, Long> {

    fun findByDeduplicationKey(
        deduplicationKey: String,
    ): NotificationLog?

    fun findByUserId(
        userId: Long,
        pageable: Pageable,
    ): Slice<NotificationLog>

    fun countByUserIdAndIsReadFalse(
        userId: Long,
    ): Long

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        delete from NotificationLog n
        where n.user.id = :userId
        """
    )
    fun deleteAllByUserId(
        @Param("userId") userId: Long,
    ): Int

    @Modifying(clearAutomatically = true)
    @Query(
        """
        update NotificationLog n
        set n.isRead = true
        where n.user.id = :userId
          and n.isRead = false
        """
    )
    fun markAllAsReadByUserId(
        @Param("userId") userId: Long,
    ): Int
}
