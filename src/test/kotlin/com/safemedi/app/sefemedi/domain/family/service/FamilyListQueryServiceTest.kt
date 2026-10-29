package com.safemedi.app.sefemedi.domain.family.service

import com.safemedi.app.sefemedi.domain.family.entity.Family
import com.safemedi.app.sefemedi.domain.family.repository.FamilyRepository
import com.safemedi.app.sefemedi.domain.user.entity.User
import com.safemedi.app.sefemedi.domain.user.repository.UserRepository
import com.safemedi.app.sefemedi.global.error.BusinessException
import com.safemedi.app.sefemedi.global.error.ErrorCode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import org.mockito.Mockito.verifyNoInteractions

class FamilyListQueryServiceTest {

    private lateinit var userRepository: UserRepository
    private lateinit var familyRepository: FamilyRepository
    private lateinit var service: FamilyListQueryService

    private val currentUser = User(id = 1L, nickname = "홍길동", socialId = "kakao-123")

    @BeforeEach
    fun setUp() {
        userRepository = mock(UserRepository::class.java)
        familyRepository = mock(FamilyRepository::class.java)
        service = FamilyListQueryService(userRepository, familyRepository)

        given(userRepository.findBySocialId("kakao-123")).willReturn(currentUser)
    }

    @Test
    fun `본인을 첫 번째로 두고 연동 가족을 조회 순서대로 반환한다`() {
        val mother = User(id = 2L, nickname = "김영희", socialId = "kakao-456")
        val father = User(id = 3L, nickname = "홍철수", socialId = "kakao-789")
        given(familyRepository.findAllByUser_IdOrderByCreatedAtAsc(1L)).willReturn(
            listOf(
                family(id = 12L, connectedUser = mother, relation = "어머니"),
                family(id = 13L, connectedUser = father, relation = "아버지"),
            )
        )

        val response = service.getFamilies("kakao-123")

        assertEquals(3, response.families.size)
        assertNull(response.families[0].familyId)
        assertEquals("홍길동", response.families[0].name)
        assertEquals("본인", response.families[0].relation)
        assertEquals(12L, response.families[1].familyId)
        assertEquals("김영희", response.families[1].name)
        assertEquals("어머니", response.families[1].relation)
        assertEquals(13L, response.families[2].familyId)
        assertEquals("홍철수", response.families[2].name)
        assertEquals("아버지", response.families[2].relation)
    }

    @Test
    fun `연동 가족이 없으면 본인만 반환한다`() {
        given(familyRepository.findAllByUser_IdOrderByCreatedAtAsc(1L)).willReturn(emptyList())

        val response = service.getFamilies("kakao-123")

        assertEquals(1, response.families.size)
        assertNull(response.families.single().familyId)
        assertEquals("홍길동", response.families.single().name)
        assertEquals("본인", response.families.single().relation)
    }

    @Test
    fun `인증 사용자를 찾을 수 없으면 AUTH_003 예외를 던진다`() {
        given(userRepository.findBySocialId("missing-user")).willReturn(null)

        assertError(ErrorCode.INVALID_ACCESS_TOKEN) {
            service.getFamilies("missing-user")
        }

        verifyNoInteractions(familyRepository)
    }

    @Test
    fun `본인 닉네임이 없으면 이름 없이 본인 항목을 반환한다`() {
        given(userRepository.findBySocialId("kakao-123")).willReturn(
            User(id = 1L, nickname = null, socialId = "kakao-123")
        )
        given(familyRepository.findAllByUser_IdOrderByCreatedAtAsc(1L)).willReturn(emptyList())

        val response = service.getFamilies("kakao-123")

        assertEquals(1, response.families.size)
        assertNull(response.families.single().familyId)
        assertNull(response.families.single().name)
        assertEquals("본인", response.families.single().relation)
    }

    @Test
    fun `연동 가족 닉네임이 없으면 이름 없이 가족 항목을 반환한다`() {
        val connectedUser = User(id = 2L, nickname = null, socialId = "kakao-456")
        given(familyRepository.findAllByUser_IdOrderByCreatedAtAsc(1L)).willReturn(
            listOf(family(id = 12L, connectedUser = connectedUser, relation = "가족"))
        )

        val response = service.getFamilies("kakao-123")

        assertEquals(2, response.families.size)
        assertNull(response.families[0].familyId)
        assertEquals("홍길동", response.families[0].name)
        assertEquals("본인", response.families[0].relation)
        assertEquals(12L, response.families[1].familyId)
        assertNull(response.families[1].name)
        assertEquals("가족", response.families[1].relation)
    }

    private fun family(
        id: Long,
        connectedUser: User,
        relation: String,
    ): Family {
        return Family(
            id = id,
            user = currentUser,
            connectedUser = connectedUser,
            relation = relation,
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
