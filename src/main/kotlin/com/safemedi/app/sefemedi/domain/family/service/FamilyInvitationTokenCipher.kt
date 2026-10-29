package com.safemedi.app.sefemedi.domain.family.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

@Component
class FamilyInvitationTokenCipher(
    @Value("\${app.family-invitation.token-encryption-key}") encodedKey: String,
) {
    private val secureRandom = SecureRandom()
    private val key = Base64.getDecoder().decode(encodedKey).also {
        require(it.size == KEY_BYTE_LENGTH) { "가족 초대 토큰 암호화 키는 32바이트여야 합니다." }
    }.let { SecretKeySpec(it, "AES") }

    fun encrypt(rawToken: String): String {
        val nonce = ByteArray(NONCE_BYTE_LENGTH).also(secureRandom::nextBytes)
        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION).apply {
            init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(AUTH_TAG_BIT_LENGTH, nonce))
        }
        val encrypted = cipher.doFinal(rawToken.toByteArray(Charsets.UTF_8))
        return Base64.getEncoder().encodeToString(nonce + encrypted)
    }

    fun decrypt(encryptedToken: String): String {
        val payload = Base64.getDecoder().decode(encryptedToken)
        require(payload.size > NONCE_BYTE_LENGTH) { "유효하지 않은 가족 초대 토큰 암호문입니다." }
        val nonce = payload.copyOfRange(0, NONCE_BYTE_LENGTH)
        val encrypted = payload.copyOfRange(NONCE_BYTE_LENGTH, payload.size)
        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION).apply {
            init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(AUTH_TAG_BIT_LENGTH, nonce))
        }
        return cipher.doFinal(encrypted).toString(Charsets.UTF_8)
    }

    private companion object {
        const val KEY_BYTE_LENGTH = 32
        const val NONCE_BYTE_LENGTH = 12
        const val AUTH_TAG_BIT_LENGTH = 128
        const val CIPHER_TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
