package com.safemedi.app.sefemedi.domain.family.service

import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.Base64

class FamilyInvitationTokenCipherTest {
    private val cipher = FamilyInvitationTokenCipher(
        Base64.getEncoder().encodeToString(ByteArray(32) { it.toByte() })
    )

    @Test
    fun `encrypt hides token and decrypt restores it`() {
        val rawToken = "family-invitation-token"

        val encrypted = cipher.encrypt(rawToken)

        assertNotEquals(rawToken, encrypted)
        assertEquals(rawToken, cipher.decrypt(encrypted))
    }

    @Test
    fun `decrypt rejects tampered ciphertext`() {
        val payload = Base64.getDecoder().decode(cipher.encrypt("family-invitation-token"))
        payload[payload.lastIndex] = (payload.last().toInt() xor 1).toByte()

        assertThrows(Exception::class.java) {
            cipher.decrypt(Base64.getEncoder().encodeToString(payload))
        }
    }
}
