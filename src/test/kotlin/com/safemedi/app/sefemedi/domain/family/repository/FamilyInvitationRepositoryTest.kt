package com.safemedi.app.sefemedi.domain.family.repository

import jakarta.persistence.LockModeType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.Lock

class FamilyInvitationRepositoryTest {

    @Test
    fun `findByTokenHash는 inviter를 함께 조회한다`() {
        val method = FamilyInvitationRepository::class.java.getMethod(
            "findByTokenHash",
            String::class.java,
        )

        val entityGraph = method.getAnnotation(EntityGraph::class.java)

        assertEquals(listOf("inviter"), entityGraph.attributePaths.toList())
        assertEquals(EntityGraph.EntityGraphType.FETCH, entityGraph.type)
    }

    @Test
    fun `findByTokenHashForUpdate는 inviter 조회와 쓰기 잠금을 적용한다`() {
        val method = FamilyInvitationRepository::class.java.getMethod(
            "findByTokenHashForUpdate",
            String::class.java,
        )

        val entityGraph = method.getAnnotation(EntityGraph::class.java)
        val lock = method.getAnnotation(Lock::class.java)

        assertEquals(listOf("inviter"), entityGraph.attributePaths.toList())
        assertEquals(EntityGraph.EntityGraphType.FETCH, entityGraph.type)
        assertEquals(LockModeType.PESSIMISTIC_WRITE, lock.value)
    }
}
