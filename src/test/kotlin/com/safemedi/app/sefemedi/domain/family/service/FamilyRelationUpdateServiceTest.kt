package com.safemedi.app.sefemedi.domain.family.service

import com.safemedi.app.sefemedi.domain.family.dto.FamilyRelationUpdateRequest
import com.safemedi.app.sefemedi.domain.family.entity.Family
import com.safemedi.app.sefemedi.domain.family.repository.FamilyRepository
import com.safemedi.app.sefemedi.domain.user.entity.User
import com.safemedi.app.sefemedi.domain.user.repository.UserRepository
import com.safemedi.app.sefemedi.global.entity.BaseTimeEntity
import com.safemedi.app.sefemedi.global.error.BusinessException
import com.safemedi.app.sefemedi.global.error.ErrorCode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import java.time.Instant
import java.time.LocalDateTime

class FamilyRelationUpdateServiceTest {

    private lateinit var userRepository: UserRepository
    private lateinit var familyRepository: FamilyRepository
    private lateinit var service: FamilyRelationUpdateService

    private val currentUser = User(id = 1L, nickname = "홍길동", socialId = "kakao-123")
    private val connectedUser = User(id = 2L, nickname = "김영희", socialId = "kakao-456")

    @BeforeEach
    fun setUp() {
        userRepository = mock(UserRepository::class.java)
        familyRepository = mock(FamilyRepository::class.java)
        service = FamilyRelationUpdateService(userRepository, familyRepository)

        given(userRepository.findBySocialId("kakao-123")).willReturn(currentUser)
    }

    @Test
    fun `가족 호칭의 앞뒤 공백을 제거해 현재 사용자의 관계만 수정한다`() {
        val family = family()
        given(familyRepository.findWithConnectedUserById(12L)).willReturn(family)
        doAnswer {
            setUpdatedAt(family, LocalDateTime.of(2026, 7, 15, 6, 10))
            null
        }.`when`(familyRepository).flush()

        val response = service.update(
            socialId = "kakao-123",
            familyId = 12L,
            request = FamilyRelationUpdateRequest("  어머니  "),
        )

        assertEquals("어머니", family.relation)
        assertEquals(12L, response.familyId)
        assertEquals("김영희", response.name)
        assertEquals("어머니", response.relation)
        assertEquals(Instant.parse("2026-07-15T06:10:00Z"), response.updatedAt)
        verify(familyRepository).flush()
    }

    @Test
    fun `호칭이 비어 있으면 VAL_008 예외를 던진다`() {
        assertError(ErrorCode.INVALID_FAMILY_RELATION) {
            service.update("kakao-123", 12L, FamilyRelationUpdateRequest("   "))
        }

        verifyNoInteractions(familyRepository)
    }

    @Test
    fun `호칭이 누락되면 VAL_008 예외를 던진다`() {
        assertError(ErrorCode.INVALID_FAMILY_RELATION) {
            service.update("kakao-123", 12L, FamilyRelationUpdateRequest(null))
        }

        verifyNoInteractions(familyRepository)
    }

    @Test
    fun `호칭이 20자를 초과하면 VAL_008 예외를 던진다`() {
        assertError(ErrorCode.INVALID_FAMILY_RELATION) {
            service.update("kakao-123", 12L, FamilyRelationUpdateRequest("가".repeat(21)))
        }

        verifyNoInteractions(familyRepository)
    }

    @Test
    fun `호칭이 20자이면 수정할 수 있다`() {
        val family = family()
        val relation = "가".repeat(20)
        given(familyRepository.findWithConnectedUserById(12L)).willReturn(family)
        doAnswer {
            setUpdatedAt(family, LocalDateTime.of(2026, 7, 15, 6, 10))
            null
        }.`when`(familyRepository).flush()

        val response = service.update(
            "kakao-123",
            12L,
            FamilyRelationUpdateRequest(relation),
        )

        assertEquals(relation, response.relation)
    }

    @Test
    fun `가족 관계가 없으면 FAM_002 예외를 던진다`() {
        given(familyRepository.findWithConnectedUserById(12L)).willReturn(null)

        assertError(ErrorCode.FAMILY_NOT_FOUND) {
            service.update("kakao-123", 12L, FamilyRelationUpdateRequest("어머니"))
        }
    }

    @Test
    fun `다른 사용자의 가족 관계이면 FAM_003 예외를 던진다`() {
        val otherUser = User(id = 3L, nickname = "다른 사용자", socialId = "kakao-789")
        given(familyRepository.findWithConnectedUserById(12L)).willReturn(
            family(user = otherUser)
        )

        assertError(ErrorCode.FAMILY_ACCESS_DENIED) {
            service.update("kakao-123", 12L, FamilyRelationUpdateRequest("어머니"))
        }

        verify(familyRepository, never()).flush()
    }

    @Test
    fun `인증 사용자를 찾을 수 없으면 AUTH_003 예외를 던진다`() {
        given(userRepository.findBySocialId("missing-user")).willReturn(null)

        assertError(ErrorCode.INVALID_ACCESS_TOKEN) {
            service.update("missing-user", 12L, FamilyRelationUpdateRequest("어머니"))
        }

        verifyNoInteractions(familyRepository)
    }

    private fun family(
        user: User = currentUser,
    ): Family {
        return Family(
            id = 12L,
            user = user,
            connectedUser = connectedUser,
            relation = "가족",
        )
    }

    private fun setUpdatedAt(
        family: Family,
        updatedAt: LocalDateTime,
    ) {
        val field = BaseTimeEntity::class.java.getDeclaredField("updatedAt")
        field.isAccessible = true
        field.set(family, updatedAt)
    }

    private fun assertError(
        errorCode: ErrorCode,
        block: () -> Unit,
    ) {
        val exception = assertThrows(BusinessException::class.java, block)
        assertEquals(errorCode, exception.errorCode)
    }
}
