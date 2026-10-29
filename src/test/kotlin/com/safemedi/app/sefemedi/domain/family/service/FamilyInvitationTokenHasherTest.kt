package com.safemedi.app.sefemedi.domain.family.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class FamilyInvitationTokenHasherTest {

    @Test
    fun `초대 토큰을 SHA-256 해시로 변환한다`() {
        val result = FamilyInvitationTokenHasher().hash("raw-token")

        assertEquals(
            "34d328009b123fbbb0dc93f18b3e6de1ecf7b1a5783c33dff7ffe1926f09e943",
            result,
        )
    }
}
