package com.safemedi.app.sefemedi.domain.family.repository

import jakarta.persistence.LockModeType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query

class FamilyRepositoryTest {

    @Test
    fun `findConnectionsBetweenForUpdate locks both directions in ID order`() {
        val method = FamilyRepository::class.java.getMethod(
            "findConnectionsBetweenForUpdate",
            Long::class.javaPrimitiveType,
            Long::class.javaPrimitiveType,
        )

        val lock = method.getAnnotation(Lock::class.java)
        val query = method.getAnnotation(Query::class.java)

        assertEquals(LockModeType.PESSIMISTIC_WRITE, lock.value)
        assertTrue(query.value.contains("order by f.id asc"))
        assertTrue(query.value.contains("f.user.id = :firstUserId"))
        assertTrue(query.value.contains("f.user.id = :secondUserId"))
    }

    @Test
    fun `findWithConnectedUserById fetches connectedUser via entity graph`() {
        val method = FamilyRepository::class.java.getMethod(
            "findWithConnectedUserById",
            Long::class.javaPrimitiveType,
        )

        val entityGraph = method.getAnnotation(EntityGraph::class.java)

        assertEquals(listOf("connectedUser"), entityGraph.attributePaths.toList())
        assertEquals(EntityGraph.EntityGraphType.FETCH, entityGraph.type)
    }

    @Test
    fun `findAllByUser_IdOrderByCreatedAtAsc fetches connectedUser via entity graph`() {
        val method = FamilyRepository::class.java.getMethod(
            "findAllByUser_IdOrderByCreatedAtAsc",
            Long::class.javaPrimitiveType,
        )

        val entityGraph = method.getAnnotation(EntityGraph::class.java)

        assertEquals(listOf("connectedUser"), entityGraph.attributePaths.toList())
        assertEquals(EntityGraph.EntityGraphType.FETCH, entityGraph.type)
    }
}
