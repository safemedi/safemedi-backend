package com.safemedi.app.sefemedi.domain.family.service

import org.springframework.stereotype.Component
import java.security.SecureRandom
import java.util.Base64

data class FamilyInvitationToken(
    val rawValue: String,
    val hash: String,
)

@Component
class FamilyInvitationTokenGenerator(
    private val tokenHasher: FamilyInvitationTokenHasher,
) {
    private val secureRandom = SecureRandom()

    fun generate(): FamilyInvitationToken {
        val bytes = ByteArray(TOKEN_BYTE_LENGTH).also(secureRandom::nextBytes)
        val rawValue = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
        return FamilyInvitationToken(rawValue = rawValue, hash = tokenHasher.hash(rawValue))
    }

    private companion object {
        const val TOKEN_BYTE_LENGTH = 32
    }
}
