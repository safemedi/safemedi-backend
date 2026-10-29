package com.safemedi.app.sefemedi.domain.family.service

import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.HexFormat

@Component
class FamilyInvitationTokenHasher {

    fun hash(rawToken: String): String {
        return MessageDigest.getInstance("SHA-256")
            .digest(rawToken.toByteArray(StandardCharsets.UTF_8))
            .let(HexFormat.of()::formatHex)
    }
}
