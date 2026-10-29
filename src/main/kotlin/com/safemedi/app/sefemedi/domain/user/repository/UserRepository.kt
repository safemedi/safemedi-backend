package com.safemedi.app.sefemedi.domain.user.repository

import com.safemedi.app.sefemedi.domain.user.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import jakarta.persistence.LockModeType

interface UserRepository : JpaRepository<User, Long> {

    @Query(
        """
        select u
        from User u
        where u.socialId = :socialId
          and u.deletedAt is null
        """
    )
    fun findBySocialId(
        @Param("socialId") socialId: String,
    ): User?

    //잠금을 걸기때문에 새로운 함수 만들어서 사용

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
        """
        select u
        from User u
        where u.socialId = :socialId
          and u.deletedAt is null
        """
    )
    fun findBySocialIdForUpdate(
        @Param("socialId") socialId: String,
    ): User?


    @Query(
        """
        select u
        from User u
        where u.socialId = :socialId
        """
    )
    fun findBySocialIdIncludingDeleted(
        @Param("socialId") socialId: String,
    ): User?
}
