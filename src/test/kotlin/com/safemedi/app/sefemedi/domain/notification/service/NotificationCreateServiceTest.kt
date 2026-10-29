package com.safemedi.app.sefemedi.domain.notification.service

import com.safemedi.app.sefemedi.domain.notification.dto.NotificationCreateCommand
import com.safemedi.app.sefemedi.domain.notification.dto.NotificationCreateStatus
import com.safemedi.app.sefemedi.domain.notification.entity.NotificationLog
import com.safemedi.app.sefemedi.domain.notification.entity.NotificationOutbox
import com.safemedi.app.sefemedi.domain.notification.entity.NotificationOutboxStatus
import com.safemedi.app.sefemedi.domain.notification.entity.NotificationTargetType
import com.safemedi.app.sefemedi.domain.notification.entity.NotificationType
import com.safemedi.app.sefemedi.domain.notification.repository.NotificationLogRepository
import com.safemedi.app.sefemedi.domain.notification.repository.NotificationOutboxRepository
import com.safemedi.app.sefemedi.domain.user.entity.User
import com.safemedi.app.sefemedi.domain.user.entity.UserDevice
import com.safemedi.app.sefemedi.domain.user.repository.UserDeviceRepository
import com.safemedi.app.sefemedi.domain.user.repository.UserRepository
import com.safemedi.app.sefemedi.global.error.BusinessException
import com.safemedi.app.sefemedi.global.error.ErrorCode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import java.time.LocalDateTime
import java.util.Optional

class NotificationCreateServiceTest {

    private lateinit var userRepository: UserRepository
    private lateinit var userDeviceRepository: UserDeviceRepository
    private lateinit var notificationLogRepository: NotificationLogRepository
    private lateinit var notificationOutboxRepository: NotificationOutboxRepository
    private lateinit var notificationCreateService: NotificationCreateService

    @BeforeEach
    fun setUp() {
        userRepository = mock(UserRepository::class.java)
        userDeviceRepository = mock(UserDeviceRepository::class.java)
        notificationLogRepository = mock(NotificationLogRepository::class.java)
        notificationOutboxRepository = mock(NotificationOutboxRepository::class.java)
        notificationCreateService = NotificationCreateService(
            userRepository = userRepository,
            userDeviceRepository = userDeviceRepository,
            notificationLogRepository = notificationLogRepository,
            notificationOutboxRepository = notificationOutboxRepository,
        )
    }

    @Test
    fun `create stores notification and outbox when policy allows`() {
        val user = User(
            id = 1L,
            socialId = "4903042739",
        )
        val userDevice = UserDevice(
            id = 10L,
            user = user,
            deviceToken = "device-token",
            deviceType = "ANDROID",
        )
        val scheduledAt = LocalDateTime.of(2026, 6, 27, 9, 0)
        val command = createCommand(
            scheduledAt = scheduledAt,
        )
        val savedNotification = NotificationLog(
            id = 100L,
            user = user,
            type = command.type,
            title = command.title,
            message = command.content,
            targetType = command.targetType,
            targetId = command.targetId,
            deduplicationKey = command.deduplicationKey,
        )

        given(notificationLogRepository.findByDeduplicationKey(command.deduplicationKey)).willReturn(null)
        given(userRepository.findById(1L)).willReturn(Optional.of(user))
        given(userDeviceRepository.findFirstByUser_IdAndIsActiveTrueOrderByCreatedAtDesc(1L)).willReturn(userDevice)
        given(notificationLogRepository.save(any(NotificationLog::class.java))).willReturn(savedNotification)

        val result = notificationCreateService.create(command)

        assertEquals(NotificationCreateStatus.CREATED, result.status)
        assertEquals(savedNotification, result.notification)

        val outboxCaptor = ArgumentCaptor.forClass(NotificationOutbox::class.java)
        verify(notificationOutboxRepository).save(outboxCaptor.capture())
        assertEquals(savedNotification, outboxCaptor.value.notificationLog)
        assertEquals(user, outboxCaptor.value.user)
        assertEquals(command.deduplicationKey, outboxCaptor.value.eventKey)
        assertEquals(scheduledAt, outboxCaptor.value.scheduledAt)
        assertEquals(NotificationOutboxStatus.PENDING, outboxCaptor.value.status)
    }

    @Test
    fun `create returns duplicated when deduplication key already exists`() {
        val user = User(
            id = 1L,
        )
        val existingNotification = NotificationLog(
            id = 100L,
            user = user,
            type = NotificationType.MEDICATION_REMINDER,
            title = "약 복용 시간입니다",
            message = "타이레놀을 복용할 시간이에요",
            deduplicationKey = "MEDICATION_REMINDER:MEDICATION_RECORD:500:1",
        )
        val command = createCommand()

        given(notificationLogRepository.findByDeduplicationKey(command.deduplicationKey)).willReturn(existingNotification)

        val result = notificationCreateService.create(command)

        assertEquals(NotificationCreateStatus.DUPLICATED, result.status)
        assertEquals(existingNotification, result.notification)
        verify(notificationLogRepository, never()).save(any(NotificationLog::class.java))
        verify(notificationOutboxRepository, never()).save(any(NotificationOutbox::class.java))
    }

    @Test
    fun `create skips notification when user has no active device`() {
        val user = User(
            id = 1L,
        )
        val command = createCommand()

        given(notificationLogRepository.findByDeduplicationKey(command.deduplicationKey)).willReturn(null)
        given(userRepository.findById(1L)).willReturn(Optional.of(user))
        given(userDeviceRepository.findFirstByUser_IdAndIsActiveTrueOrderByCreatedAtDesc(1L)).willReturn(null)

        val result = notificationCreateService.create(command)

        assertEquals(NotificationCreateStatus.SKIPPED_NO_ACTIVE_DEVICE, result.status)
        verify(notificationLogRepository, never()).save(any(NotificationLog::class.java))
        verify(notificationOutboxRepository, never()).save(any(NotificationOutbox::class.java))
    }

    @Test
    fun `create skips medication reminder when my reminder setting is off`() {
        val user = User(
            id = 1L,
        )
        val userDevice = UserDevice(
            id = 10L,
            user = user,
            deviceToken = "device-token",
            deviceType = "ANDROID",
            isMyReminderOn = false,
        )
        val command = createCommand()

        given(notificationLogRepository.findByDeduplicationKey(command.deduplicationKey)).willReturn(null)
        given(userRepository.findById(1L)).willReturn(Optional.of(user))
        given(userDeviceRepository.findFirstByUser_IdAndIsActiveTrueOrderByCreatedAtDesc(1L)).willReturn(userDevice)

        val result = notificationCreateService.create(command)

        assertEquals(NotificationCreateStatus.SKIPPED_BY_SETTINGS, result.status)
        verify(notificationLogRepository, never()).save(any(NotificationLog::class.java))
        verify(notificationOutboxRepository, never()).save(any(NotificationOutbox::class.java))
    }

    @Test
    fun `create throws USER_NOT_FOUND when receiver does not exist`() {
        val command = createCommand()

        given(notificationLogRepository.findByDeduplicationKey(command.deduplicationKey)).willReturn(null)
        given(userRepository.findById(1L)).willReturn(Optional.empty())

        val exception = assertThrows(BusinessException::class.java) {
            notificationCreateService.create(command)
        }

        assertEquals(ErrorCode.USER_NOT_FOUND, exception.errorCode)
    }

    @Test
    fun `create throws INVALID_REQUEST when required values are blank`() {
        val command = createCommand(
            title = " ",
        )

        val exception = assertThrows(BusinessException::class.java) {
            notificationCreateService.create(command)
        }

        assertEquals(ErrorCode.INVALID_REQUEST, exception.errorCode)
    }

    private fun createCommand(
        title: String = "약 복용 시간입니다",
        scheduledAt: LocalDateTime? = null,
    ): NotificationCreateCommand {
        return NotificationCreateCommand(
            userId = 1L,
            type = NotificationType.MEDICATION_REMINDER,
            title = title,
            content = "타이레놀을 복용할 시간이에요",
            targetType = NotificationTargetType.MEDICATION_RECORD,
            targetId = 500L,
            deduplicationKey = "MEDICATION_REMINDER:MEDICATION_RECORD:500:1",
            scheduledAt = scheduledAt,
        )
    }
}
