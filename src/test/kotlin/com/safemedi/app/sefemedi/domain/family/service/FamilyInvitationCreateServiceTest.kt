package com.safemedi.app.sefemedi.domain.family.service

import com.safemedi.app.sefemedi.domain.family.entity.FamilyInvitation
import com.safemedi.app.sefemedi.domain.family.entity.FamilyInvitationStatus
import com.safemedi.app.sefemedi.domain.family.repository.FamilyInvitationRepository
import com.safemedi.app.sefemedi.domain.user.entity.User
import com.safemedi.app.sefemedi.domain.user.repository.UserRepository
import com.safemedi.app.sefemedi.global.error.BusinessException
import com.safemedi.app.sefemedi.global.error.ErrorCode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.BDDMockito.given
import org.mockito.Mockito.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

class FamilyInvitationCreateServiceTest {
    private lateinit var userRepository: UserRepository
    private lateinit var familyInvitationRepository: FamilyInvitationRepository
    private lateinit var tokenGenerator: FamilyInvitationTokenGenerator
    private lateinit var tokenCipher: FamilyInvitationTokenCipher
    private lateinit var service: FamilyInvitationCreateService

    private val fixedInstant = Instant.parse("2026-07-15T04:00:00Z")

    @BeforeEach
    fun setUp() {
        userRepository = mock(UserRepository::class.java)
        familyInvitationRepository = mock(FamilyInvitationRepository::class.java)
        tokenGenerator = mock(FamilyInvitationTokenGenerator::class.java)
        tokenCipher = mock(FamilyInvitationTokenCipher::class.java)
        service = FamilyInvitationCreateService(
            userRepository = userRepository,
            familyInvitationRepository = familyInvitationRepository,
            tokenGenerator = tokenGenerator,
            tokenCipher = tokenCipher,
            clock = Clock.fixed(fixedInstant, ZoneOffset.UTC),
            baseUrl = "https://invite.example.com/invite/",
        )
    }

    @Test
    fun `create stores encrypted token and returns new PENDING invitation`() {
        val user = User(id = 3L, socialId = "kakao-123")
        val rawToken = "raw-url-safe-token"
        val tokenHash = "a".repeat(64)
        val encryptedToken = "encrypted-token"
        val invitationCaptor = ArgumentCaptor.forClass(FamilyInvitation::class.java)

        given(userRepository.findBySocialIdForUpdate("kakao-123")).willReturn(user)
        given(
            familyInvitationRepository.findFirstByInviter_IdAndStatusAndExpiresAtAfterOrderByCreatedAtDesc(
                3L,
                FamilyInvitationStatus.PENDING,
                fixedInstant,
            )
        ).willReturn(null)
        given(tokenGenerator.generate()).willReturn(
            FamilyInvitationToken(rawValue = rawToken, hash = tokenHash)
        )
        given(tokenCipher.encrypt(rawToken)).willReturn(encryptedToken)
        given(familyInvitationRepository.saveAndFlush(invitationCaptor.capture())).willAnswer {
            FamilyInvitation(
                id = 100L,
                inviter = invitationCaptor.value.inviter,
                tokenHash = invitationCaptor.value.tokenHash,
                encryptedToken = invitationCaptor.value.encryptedToken,
                expiresAt = invitationCaptor.value.expiresAt,
            )
        }

        val result = service.create("kakao-123")
        val storedInvitation = invitationCaptor.value

        assertEquals(true, result.created)
        assertEquals(100L, result.response.invitationId)
        assertEquals("https://invite.example.com/invite/$rawToken", result.response.inviteUrl)
        assertEquals(FamilyInvitationStatus.PENDING, result.response.status)
        assertEquals(Instant.parse("2026-07-15T04:00:00Z"), result.response.createdAt)
        assertEquals(Instant.parse("2026-07-16T04:00:00Z"), result.response.expiresAt)
        assertEquals(tokenHash, storedInvitation.tokenHash)
        assertEquals(encryptedToken, storedInvitation.encryptedToken)
        assertEquals(user, storedInvitation.inviter)
    }

    @Test
    fun `create returns existing active PENDING invitation without generating a new token`() {
        val user = User(id = 3L, socialId = "kakao-123")
        val existing = mock(FamilyInvitation::class.java)
        val createdAt = LocalDateTime.of(2026, 7, 14, 6, 0)
        val expiresAt = Instant.parse("2026-07-15T06:00:00Z")

        given(userRepository.findBySocialIdForUpdate("kakao-123")).willReturn(user)
        given(
            familyInvitationRepository.findFirstByInviter_IdAndStatusAndExpiresAtAfterOrderByCreatedAtDesc(
                3L,
                FamilyInvitationStatus.PENDING,
                fixedInstant,
            )
        ).willReturn(existing)
        given(existing.id).willReturn(99L)
        given(existing.encryptedToken).willReturn("encrypted-token")
        given(existing.status).willReturn(FamilyInvitationStatus.PENDING)
        given(existing.createdAt).willReturn(createdAt)
        given(existing.expiresAt).willReturn(expiresAt)
        given(tokenCipher.decrypt("encrypted-token")).willReturn("existing-token")

        val result = service.create("kakao-123")

        assertEquals(false, result.created)
        assertEquals(99L, result.response.invitationId)
        assertEquals("https://invite.example.com/invite/existing-token", result.response.inviteUrl)
        assertEquals(FamilyInvitationStatus.PENDING, result.response.status)
        assertEquals(createdAt.toInstant(ZoneOffset.UTC), result.response.createdAt)
        assertEquals(expiresAt, result.response.expiresAt)
        verify(tokenGenerator, never()).generate()
        verify(familyInvitationRepository, never()).saveAndFlush(any(FamilyInvitation::class.java))
    }

    @Test
    fun `create throws USER_NOT_FOUND without querying invitations when user is missing`() {
        given(userRepository.findBySocialIdForUpdate("missing-user")).willReturn(null)

        val exception = assertThrows(BusinessException::class.java) {
            service.create("missing-user")
        }

        assertEquals(ErrorCode.USER_NOT_FOUND, exception.errorCode)
        verify(tokenGenerator, never()).generate()
        verifyNoInteractions(familyInvitationRepository)
    }
}
