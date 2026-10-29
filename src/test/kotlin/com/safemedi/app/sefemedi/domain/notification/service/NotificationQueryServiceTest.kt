package com.safemedi.app.sefemedi.domain.notification.service

import com.safemedi.app.sefemedi.domain.notification.entity.NotificationLog
import com.safemedi.app.sefemedi.domain.notification.entity.NotificationTargetType
import com.safemedi.app.sefemedi.domain.notification.entity.NotificationType
import com.safemedi.app.sefemedi.domain.notification.repository.NotificationLogRepository
import com.safemedi.app.sefemedi.domain.user.entity.User
import com.safemedi.app.sefemedi.domain.user.repository.UserRepository
import com.safemedi.app.sefemedi.global.error.BusinessException
import com.safemedi.app.sefemedi.global.error.ErrorCode
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.SliceImpl
import org.springframework.data.domain.Sort
import org.springframework.test.util.ReflectionTestUtils
import java.time.Instant
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class NotificationQueryServiceTest {

    private lateinit var userRepository: UserRepository
    private lateinit var notificationLogRepository: NotificationLogRepository
    private lateinit var notificationQueryService: NotificationQueryService

    private val user = User(
        id = 1L,
        socialId = "kakao-123",
    )

    @BeforeEach
    fun setUp() {
        userRepository = mock(UserRepository::class.java)
        notificationLogRepository = mock(NotificationLogRepository::class.java)
        notificationQueryService = NotificationQueryService(
            userRepository = userRepository,
            notificationLogRepository = notificationLogRepository,
        )
    }

    @Test
    fun `findNotifications returns notification list`() {
        val notification = NotificationLog(
            id = 105L,
            user = user,
            type = NotificationType.MEDICATION_REMINDER,
            title = "Medication reminder",
            message = "Time to take Tylenol",
            targetType = NotificationTargetType.MEDICATION_RECORD,
            targetId = 500L,
            isRead = false,
        )
        ReflectionTestUtils.setField(
            notification,
            "createdAt",
            LocalDateTime.of(2026, 4, 7, 9, 0),
        )

        given(userRepository.findBySocialId("kakao-123")).willReturn(user)
        given(
            notificationLogRepository.findByUserId(
                userId = 1L,
                pageable = PageRequest.of(0, 10, NOTIFICATION_SORT),
            )
        ).willReturn(SliceImpl(listOf(notification), PageRequest.of(0, 10), false))

        val response = notificationQueryService.findNotifications(
            socialId = "kakao-123",
            page = 0,
            size = 10,
        )

        assertEquals(0, response.page)
        assertEquals(10, response.size)
        assertEquals(true, response.isLast)
        assertEquals(1, response.content.size)

        val item = response.content.single()
        assertEquals(105L, item.notificationId)
        assertEquals(NotificationType.MEDICATION_REMINDER, item.type)
        assertEquals("Medication reminder", item.title)
        assertEquals("Time to take Tylenol", item.message)
        assertEquals(false, item.isRead)
        assertEquals(NotificationTargetType.MEDICATION_RECORD, item.targetType)
        assertEquals(500L, item.targetId)
        assertEquals(Instant.parse("2026-04-07T09:00:00Z"), item.createdAt)
    }

    @Test
    fun `findNotifications returns empty list`() {
        given(userRepository.findBySocialId("kakao-123")).willReturn(user)
        given(
            notificationLogRepository.findByUserId(
                userId = 1L,
                pageable = PageRequest.of(0, 20, NOTIFICATION_SORT),
            )
        ).willReturn(SliceImpl(emptyList(), PageRequest.of(0, 20), false))

        val response = notificationQueryService.findNotifications(
            socialId = "kakao-123",
            page = 0,
            size = 20,
        )

        assertEquals(emptyList(), response.content)
        assertEquals(true, response.isLast)
    }

    @Test
    fun `findNotifications throws when page is invalid`() {
        val exception = assertFailsWith<BusinessException> {
            notificationQueryService.findNotifications(
                socialId = "kakao-123",
                page = -1,
                size = 20,
            )
        }

        assertEquals(ErrorCode.INVALID_PAGE_REQUEST, exception.errorCode)
    }

    @Test
    fun `findNotifications throws when size is invalid`() {
        val exception = assertFailsWith<BusinessException> {
            notificationQueryService.findNotifications(
                socialId = "kakao-123",
                page = 0,
                size = 101,
            )
        }

        assertEquals(ErrorCode.INVALID_PAGE_REQUEST, exception.errorCode)
    }

    @Test
    fun `countUnreadNotifications returns unread count`() {
        given(userRepository.findBySocialId("kakao-123")).willReturn(user)
        given(notificationLogRepository.countByUserIdAndIsReadFalse(1L)).willReturn(3L)

        val response = notificationQueryService.countUnreadNotifications(
            socialId = "kakao-123",
        )

        assertEquals(3L, response.unreadCount)
    }

    @Test
    fun `countUnreadNotifications returns zero`() {
        given(userRepository.findBySocialId("kakao-123")).willReturn(user)
        given(notificationLogRepository.countByUserIdAndIsReadFalse(1L)).willReturn(0L)

        val response = notificationQueryService.countUnreadNotifications(
            socialId = "kakao-123",
        )

        assertEquals(0L, response.unreadCount)
    }

    @Test
    fun `countUnreadNotifications throws when user not found`() {
        given(userRepository.findBySocialId("invalid-social-id")).willReturn(null)

        val exception = assertFailsWith<BusinessException> {
            notificationQueryService.countUnreadNotifications(
                socialId = "invalid-social-id",
            )
        }

        assertEquals(ErrorCode.INVALID_TOKEN, exception.errorCode)
    }

    companion object {
        private val NOTIFICATION_SORT = Sort.by(Sort.Direction.DESC, "createdAt", "id")
    }
}
