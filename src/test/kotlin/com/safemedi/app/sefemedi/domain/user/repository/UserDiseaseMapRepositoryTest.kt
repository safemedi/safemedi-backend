package com.safemedi.app.sefemedi.domain.user.repository

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.data.jpa.repository.EntityGraph

class UserDiseaseMapRepositoryTest {

    @Test
    fun `findAllByUser_IdOrderByCreatedAtAsc fetches disease via entity graph`() {
        val method = UserDiseaseMapRepository::class.java.getMethod(
            "findAllByUser_IdOrderByCreatedAtAsc",
            Long::class.javaPrimitiveType,
        )

        val entityGraph = method.getAnnotation(EntityGraph::class.java)

        assertEquals(listOf("disease"), entityGraph.attributePaths.toList())
        assertEquals(EntityGraph.EntityGraphType.FETCH, entityGraph.type)
    }
}
