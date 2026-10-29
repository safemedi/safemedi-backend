package com.safemedi.app.sefemedi.domain.user.repository

import com.safemedi.app.sefemedi.domain.user.entity.UserDevice
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface UserDeviceRepository : JpaRepository<UserDevice, Long> {

    fun findFirstByUser_IdOrderByCreatedAtDesc(
        userId: Long,
    ): UserDevice?

    // 계정 전환과 비활성화가 동일한 소유권 상태를 기준으로 순차 처리되도록 한다.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    fun findByDeviceToken(
        deviceToken: String,
    ): UserDevice?

    fun findFirstByUser_IdAndIsActiveTrueOrderByCreatedAtDesc(
        userId: Long,
    ): UserDevice?

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        delete from UserDevice ud
        where ud.user.id = :userId
        """
    )
    fun deleteAllByUserId(
        @Param("userId") userId: Long,
    ): Int
}
