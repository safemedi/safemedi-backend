package com.safemedi.app.sefemedi.domain.family.service

import com.safemedi.app.sefemedi.domain.family.entity.Family
import com.safemedi.app.sefemedi.domain.family.repository.FamilyRepository
import com.safemedi.app.sefemedi.domain.notification.dto.NotificationCreateCommand
import com.safemedi.app.sefemedi.domain.notification.entity.NotificationType
import com.safemedi.app.sefemedi.domain.notification.service.NotificationCreateService
import com.safemedi.app.sefemedi.domain.user.entity.User
import com.safemedi.app.sefemedi.domain.user.repository.UserRepository
import com.safemedi.app.sefemedi.global.error.BusinessException
import com.safemedi.app.sefemedi.global.error.ErrorCode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.anyList
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockingDetails
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions

class FamilyDisconnectServiceTest {

    private lateinit var userRepository: UserRepository
    private lateinit var familyRepository: FamilyRepository
    private lateinit var notificationCreateService: NotificationCreateService
    private lateinit var service: FamilyDisconnectService

    private val currentUser = User(id = 2L, nickname = "홍길동", socialId = "kakao-123")
    private val connectedUser = User(id = 1L, nickname = "김영희", socialId = "kakao-456")

    @BeforeEach
    fun setUp() {
        userRepository = mock(UserRepository::class.java)
        familyRepository = mock(FamilyRepository::class.java)
        notificationCreateService = mock(NotificationCreateService::class.java)
        service = FamilyDisconnectService(userRepository, familyRepository, notificationCreateService)

        given(userRepository.findBySocialId("kakao-123")).willReturn(currentUser)
    }

    @Test
    fun `가족 연동을 해제하면 잠근 양방향 관계를 모두 삭제하고 상대방에게만 알림을 보낸다`() {
        val currentUserFamily = family(12L, currentUser, connectedUser)
        val connectedUserFamily = family(13L, connectedUser, currentUser)
        val connections = listOf(currentUserFamily, connectedUserFamily)
        given(familyRepository.findWithConnectedUserById(12L)).willReturn(currentUserFamily)
        given(familyRepository.findConnectionsBetweenForUpdate(1L, 2L)).willReturn(connections)

        service.disconnect("kakao-123", 12L)

        verify(familyRepository).findConnectionsBetweenForUpdate(1L, 2L)
        verify(familyRepository).deleteAll(connections)

        val command = mockingDetails(notificationCreateService)
            .invocations
            .single()
            .arguments[0] as NotificationCreateCommand
        assertEquals(1L, command.userId)
        assertEquals(NotificationType.FAMILY_DISCONNECTED, command.type)
        assertEquals("홍길동님과의 가족 연결이 해제되었어요", command.content)
        assertEquals("FAMILY_DISCONNECTED:FAMILY:12:1", command.deduplicationKey)
    }

    @Test
    fun `해제를 실행한 본인의 닉네임이 없으면 이름 없이 알림 문구를 만든다`() {
        val nicknameLessCurrentUser = User(id = 2L, nickname = null, socialId = "kakao-123")
        given(userRepository.findBySocialId("kakao-123")).willReturn(nicknameLessCurrentUser)
        val currentUserFamily = family(12L, nicknameLessCurrentUser, connectedUser)
        val connectedUserFamily = family(13L, connectedUser, nicknameLessCurrentUser)
        val connections = listOf(currentUserFamily, connectedUserFamily)
        given(familyRepository.findWithConnectedUserById(12L)).willReturn(currentUserFamily)
        given(familyRepository.findConnectionsBetweenForUpdate(1L, 2L)).willReturn(connections)

        service.disconnect("kakao-123", 12L)

        val command = mockingDetails(notificationCreateService)
            .invocations
            .single()
            .arguments[0] as NotificationCreateCommand
        assertEquals("가족 연결이 해제되었어요", command.content)
    }

    @Test
    fun `인증 사용자를 찾을 수 없으면 AUTH_003 예외를 던진다`() {
        given(userRepository.findBySocialId("missing-user")).willReturn(null)

        assertError(ErrorCode.INVALID_ACCESS_TOKEN) {
            service.disconnect("missing-user", 12L)
        }

        verifyNoInteractions(familyRepository)
    }

    @Test
    fun `가족 관계가 없으면 FAM_002 예외를 던진다`() {
        given(familyRepository.findWithConnectedUserById(12L)).willReturn(null)

        assertError(ErrorCode.FAMILY_NOT_FOUND) {
            service.disconnect("kakao-123", 12L)
        }

        verify(familyRepository, never()).findConnectionsBetweenForUpdate(1L, 2L)
    }

    @Test
    fun `다른 사용자가 소유한 가족 관계이면 FAM_003 예외를 던진다`() {
        val otherUser = User(id = 3L, nickname = "다른 사용자", socialId = "kakao-789")
        val otherFamily = family(12L, otherUser, connectedUser)
        given(familyRepository.findWithConnectedUserById(12L)).willReturn(otherFamily)

        assertError(ErrorCode.FAMILY_ACCESS_DENIED) {
            service.disconnect("kakao-123", 12L)
        }

        verify(familyRepository, never()).findConnectionsBetweenForUpdate(1L, 2L)
        verify(familyRepository, never()).deleteAll(anyList())
    }

    @Test
    fun `잠금 대기 중 관계가 해제되었으면 FAM_002 예외를 던진다`() {
        val currentUserFamily = family(12L, currentUser, connectedUser)
        given(familyRepository.findWithConnectedUserById(12L)).willReturn(currentUserFamily)
        given(familyRepository.findConnectionsBetweenForUpdate(1L, 2L)).willReturn(emptyList())

        assertError(ErrorCode.FAMILY_NOT_FOUND) {
            service.disconnect("kakao-123", 12L)
        }

        verify(familyRepository, never()).deleteAll(anyList())
    }

    private fun family(
        id: Long,
        user: User,
        connectedUser: User,
    ): Family {
        return Family(
            id = id,
            user = user,
            connectedUser = connectedUser,
            relation = "가족",
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
