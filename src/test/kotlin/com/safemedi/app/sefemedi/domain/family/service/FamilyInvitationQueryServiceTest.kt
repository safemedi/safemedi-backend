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
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import org.mockito.Mockito.verifyNoInteractions
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class FamilyInvitationQueryServiceTest {
    private lateinit var userRepository: UserRepository
    private lateinit var familyInvitationRepository: FamilyInvitationRepository
    private lateinit var tokenHasher: FamilyInvitationTokenHasher
    private lateinit var service: FamilyInvitationQueryService

    private val currentUser = User(id = 1L, nickname = "현재 사용자", socialId = "kakao-123")
    private val inviter = User(id = 2L, nickname = "홍길동", socialId = "kakao-456")
    private val now = Instant.parse("2026-07-15T06:00:00Z")

    @BeforeEach
    fun setUp() {
        userRepository = mock(UserRepository::class.java)
        familyInvitationRepository = mock(FamilyInvitationRepository::class.java)
        tokenHasher = mock(FamilyInvitationTokenHasher::class.java)
        service = FamilyInvitationQueryService(
            userRepository = userRepository,
            familyInvitationRepository = familyInvitationRepository,
            tokenHasher = tokenHasher,
            clock = Clock.fixed(Instant.parse("2026-07-15T06:00:00Z"), ZoneOffset.UTC),
        )

        given(userRepository.findBySocialId("kakao-123")).willReturn(currentUser)
        given(tokenHasher.hash("raw-token")).willReturn("token-hash")
    }

    @Test
    fun `유효한 초대 링크이면 초대자 이름과 UTC 만료 시각을 반환한다`() {
        given(familyInvitationRepository.findByTokenHash("token-hash")).willReturn(
            invitation(expiresAt = now.plusSeconds(12 * 60 * 60))
        )

        val response = service.getInvitationInfo("kakao-123", "raw-token")

        assertEquals("홍길동", response.inviterName)
        assertEquals(Instant.parse("2026-07-15T18:00:00Z"), response.expiresAt)
    }

    @Test
    fun `현재 사용자가 없으면 AUTH_003 예외를 던진다`() {
        given(userRepository.findBySocialId("missing-user")).willReturn(null)

        assertError(ErrorCode.INVALID_ACCESS_TOKEN) {
            service.getInvitationInfo("missing-user", "raw-token")
        }

        verifyNoInteractions(familyInvitationRepository)
    }

    @Test
    fun `토큰에 해당하는 초대가 없으면 INV_001 예외를 던진다`() {
        given(familyInvitationRepository.findByTokenHash("token-hash")).willReturn(null)

        assertError(ErrorCode.FAMILY_INVITATION_NOT_FOUND) {
            service.getInvitationInfo("kakao-123", "raw-token")
        }
    }

    @Test
    fun `초대가 만료되었으면 INV_002 예외를 던진다`() {
        given(familyInvitationRepository.findByTokenHash("token-hash")).willReturn(
            invitation(expiresAt = now)
        )

        assertError(ErrorCode.FAMILY_INVITATION_EXPIRED) {
            service.getInvitationInfo("kakao-123", "raw-token")
        }
    }

    @Test
    fun `이미 사용된 초대이면 INV_003 예외를 던진다`() {
        given(familyInvitationRepository.findByTokenHash("token-hash")).willReturn(
            invitation(status = FamilyInvitationStatus.ACCEPTED)
        )

        assertError(ErrorCode.FAMILY_INVITATION_ALREADY_USED) {
            service.getInvitationInfo("kakao-123", "raw-token")
        }
    }

    @Test
    fun `자신이 생성한 초대이면 INV_004 예외를 던진다`() {
        given(familyInvitationRepository.findByTokenHash("token-hash")).willReturn(
            invitation(inviter = currentUser)
        )

        assertError(ErrorCode.FAMILY_INVITATION_SELF_ACCESS) {
            service.getInvitationInfo("kakao-123", "raw-token")
        }
    }

    @Test
    fun `초대자 이름이 없으면 INV_005 예외를 던진다`() {
        given(familyInvitationRepository.findByTokenHash("token-hash")).willReturn(
            invitation(inviter = User(id = 2L, nickname = null, socialId = "kakao-456"))
        )

        assertError(ErrorCode.FAMILY_INVITATION_INVITER_NAME_NOT_FOUND) {
            service.getInvitationInfo("kakao-123", "raw-token")
        }
    }

    private fun invitation(
        inviter: User = this.inviter,
        status: FamilyInvitationStatus = FamilyInvitationStatus.PENDING,
        expiresAt: Instant = now.plusSeconds(3600),
    ): FamilyInvitation {
        return FamilyInvitation(
            id = 10L,
            inviter = inviter,
            tokenHash = "token-hash",
            encryptedToken = "encrypted-token",
            status = status,
            expiresAt = expiresAt,
        )
    }

    private fun assertError(
        errorCode: ErrorCode,
        block: () -> Unit,
    ) {
        val exception = assertThrows(BusinessException::class.java, block)
        assertEquals(errorCode, exception.errorCode)
    }
}
