package com.safemedi.app.sefemedi.domain.notification.service

import com.safemedi.app.sefemedi.domain.family.entity.Family
import com.safemedi.app.sefemedi.domain.family.repository.FamilyRepository
import com.safemedi.app.sefemedi.domain.notification.entity.NotificationLog
import com.safemedi.app.sefemedi.domain.notification.entity.NotificationOutbox
import com.safemedi.app.sefemedi.domain.notification.entity.NotificationOutboxStatus
import com.safemedi.app.sefemedi.domain.notification.entity.NotificationTargetType
import com.safemedi.app.sefemedi.domain.notification.entity.NotificationType
import com.safemedi.app.sefemedi.domain.notification.fcm.NotificationPushRequest
import com.safemedi.app.sefemedi.domain.notification.fcm.NotificationPushResult
import com.safemedi.app.sefemedi.domain.notification.fcm.NotificationPushSender
import com.safemedi.app.sefemedi.domain.notification.fcm.NotificationPushStatus
import com.safemedi.app.sefemedi.domain.notification.repository.NotificationOutboxRepository
import com.safemedi.app.sefemedi.domain.user.entity.User
import com.safemedi.app.sefemedi.domain.user.entity.UserDevice
import com.safemedi.app.sefemedi.domain.user.repository.UserDeviceRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import org.springframework.data.domain.PageRequest
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.TransactionStatus
import org.springframework.transaction.support.SimpleTransactionStatus
import org.springframework.transaction.support.TransactionTemplate
import java.time.LocalDateTime
import java.util.Optional
import kotlin.test.assertEquals

class NotificationOutboxWorkerServiceTest {

    private lateinit var notificationOutboxRepository: NotificationOutboxRepository
    private lateinit var familyRepository: FamilyRepository
    private lateinit var userDeviceRepository: UserDeviceRepository
    private lateinit var notificationPushSender: FakeNotificationPushSender
    private lateinit var notificationOutboxWorkerService: NotificationOutboxWorkerService

    private val user = User(
        id = 1L,
        socialId = "kakao-123",
    )

    @BeforeEach
    fun setUp() {
        notificationOutboxRepository = mock(NotificationOutboxRepository::class.java)
        userDeviceRepository = mock(UserDeviceRepository::class.java)
        familyRepository = mock(FamilyRepository::class.java)
        notificationPushSender = FakeNotificationPushSender()
        notificationOutboxWorkerService = NotificationOutboxWorkerService(
            notificationOutboxRepository = notificationOutboxRepository,
            userDeviceRepository = userDeviceRepository,
            familyRepository = familyRepository,
            notificationPushSender = notificationPushSender,
            transactionTemplate = TransactionTemplate(NoOpTransactionManager()),
        )
    }

    @Test
    fun `처리 대상 아웃박스를 FCM으로 발송하고 SENT 상태로 변경한다`() {
        val now = LocalDateTime.of(2026, 6, 29, 9, 0)
        val outbox = notificationOutbox()
        val userDevice = userDevice()
        given(userDeviceRepository.findById(20L)).willReturn(Optional.of(userDevice))

        givenDueOutboxes(now, listOf(outbox))
        given(userDeviceRepository.findFirstByUser_IdAndIsActiveTrueOrderByCreatedAtDesc(1L)).willReturn(userDevice)
        given(notificationOutboxRepository.findById(10L)).willReturn(Optional.of(outbox))
        notificationPushSender.result = NotificationPushResult(NotificationPushStatus.SUCCESS)

        val processedCount = notificationOutboxWorkerService.processDueOutboxes(now, batchSize = 50)

        val request = requireNotNull(notificationPushSender.requests.single())

        assertEquals(1, processedCount)
        assertEquals(NotificationOutboxStatus.SENT, outbox.status)
        assertEquals("device-token", request.token)
        assertEquals("Medication reminder", request.title)
        assertEquals("Time to take medicine", request.body)
        assertEquals("100", request.data["notificationId"])
        assertEquals("MEDICATION_REMINDER", request.data["type"])
        assertEquals("MEDICATION_RECORD", request.data["targetType"])
        assertEquals("500", request.data["targetId"])
    }

    @Test
    fun `유효하지 않은 토큰이면 기기 토큰을 비활성화하고 아웃박스를 DEAD 처리한다`() {
        val now = LocalDateTime.of(2026, 6, 29, 9, 0)
        val outbox = notificationOutbox()
        val userDevice = userDevice()
        given(userDeviceRepository.findById(20L)).willReturn(Optional.of(userDevice))

        givenDueOutboxes(now, listOf(outbox))
        given(userDeviceRepository.findFirstByUser_IdAndIsActiveTrueOrderByCreatedAtDesc(1L)).willReturn(userDevice)
        given(notificationOutboxRepository.findById(10L)).willReturn(Optional.of(outbox))
        given(userDeviceRepository.findByDeviceToken("device-token")).willReturn(userDevice)
        notificationPushSender.result = NotificationPushResult(NotificationPushStatus.INVALID_TOKEN, "UNREGISTERED")

        notificationOutboxWorkerService.processDueOutboxes(now, batchSize = 50)

        assertEquals(NotificationOutboxStatus.DEAD, outbox.status)
        assertEquals(false, userDevice.isActive)
    }

    @Test
    fun `일시 실패이면 재시도 시간을 예약하고 FAILED 상태로 변경한다`() {
        val now = LocalDateTime.of(2026, 6, 29, 9, 0)
        val outbox = notificationOutbox()
        val userDevice = userDevice()
        given(userDeviceRepository.findById(20L)).willReturn(Optional.of(userDevice))

        givenDueOutboxes(now, listOf(outbox))
        given(userDeviceRepository.findFirstByUser_IdAndIsActiveTrueOrderByCreatedAtDesc(1L)).willReturn(userDevice)
        given(notificationOutboxRepository.findById(10L)).willReturn(Optional.of(outbox))
        notificationPushSender.result = NotificationPushResult(NotificationPushStatus.TEMPORARY_FAILURE, "UNAVAILABLE")

        notificationOutboxWorkerService.processDueOutboxes(now, batchSize = 50)

        assertEquals(NotificationOutboxStatus.FAILED, outbox.status)
        assertEquals(1, outbox.retryCount)
        assertEquals(now.plusMinutes(1), outbox.nextRetryAt)
    }

    @Test
    fun `활성 기기가 없으면 아웃박스를 DEAD 처리한다`() {
        val now = LocalDateTime.of(2026, 6, 29, 9, 0)
        val outbox = notificationOutbox()

        givenDueOutboxes(now, listOf(outbox))
        given(userDeviceRepository.findFirstByUser_IdAndIsActiveTrueOrderByCreatedAtDesc(1L)).willReturn(null)

        notificationOutboxWorkerService.processDueOutboxes(now, batchSize = 50)

        assertEquals(NotificationOutboxStatus.DEAD, outbox.status)
        assertEquals(0, notificationPushSender.requests.size)
    }

    @Test
    fun `발송 직전 비활성화되거나 소유자가 바뀌거나 삭제된 기기에는 발송하지 않는다`() {
        val now = LocalDateTime.of(2026, 6, 29, 9, 0)
        val changedDevices = listOf(
            userDevice().apply { deactivate() },
            userDevice().apply { register(User(id = 2L, socialId = "other"), "ANDROID") },
            null,
        )
        changedDevices.forEach { currentDevice ->
            val outbox = notificationOutbox()
            givenDueOutboxes(now, listOf(outbox))
            given(userDeviceRepository.findFirstByUser_IdAndIsActiveTrueOrderByCreatedAtDesc(1L))
                .willReturn(userDevice())
            given(userDeviceRepository.findById(20L)).willReturn(Optional.ofNullable(currentDevice))
            given(notificationOutboxRepository.findById(10L)).willReturn(Optional.of(outbox))

            notificationOutboxWorkerService.processDueOutboxes(now, batchSize = 50)

            assertEquals(NotificationOutboxStatus.DEAD, outbox.status)
            assertEquals(0, notificationPushSender.requests.size)
        }
    }

    @Test
    fun `FCM 응답을 기다리는 동안 소유권이 바뀌면 새 계정의 기기를 비활성화하지 않는다`() {
        val now = LocalDateTime.of(2026, 6, 29, 9, 0)
        val outbox = notificationOutbox()
        val device = userDevice()
        givenDueOutboxes(now, listOf(outbox))
        given(userDeviceRepository.findFirstByUser_IdAndIsActiveTrueOrderByCreatedAtDesc(1L)).willReturn(device)
        given(userDeviceRepository.findById(20L)).willReturn(Optional.of(device))
        given(userDeviceRepository.findByDeviceToken("device-token")).willAnswer {
            device.apply { register(User(id = 2L, socialId = "other"), "ANDROID") }
        }
        given(notificationOutboxRepository.findById(10L)).willReturn(Optional.of(outbox))
        notificationPushSender.result = NotificationPushResult(NotificationPushStatus.INVALID_TOKEN)

        notificationOutboxWorkerService.processDueOutboxes(now, batchSize = 50)

        assertEquals(true, device.isActive)
        assertEquals(2L, device.user.id)
        assertEquals(NotificationOutboxStatus.DEAD, outbox.status)
    }

    @Test
    fun `기존에 대기중인 복약 완료 알림도 발송하지 않는다`() {
        val now = LocalDateTime.of(2026, 6, 29, 9, 0)
        val outbox = notificationOutbox(NotificationType.MEDICATION_COMPLETED)
        givenDueOutboxes(now, listOf(outbox))

        notificationOutboxWorkerService.processDueOutboxes(now, batchSize = 50)

        assertEquals(NotificationOutboxStatus.DEAD, outbox.status)
        assertEquals(0, notificationPushSender.requests.size)
    }

    @Test
    fun `가족 미복약 알림은 발송 직전 수신 설정과 공유 상태 및 연결을 다시 확인한다`() {
        val now = LocalDateTime.of(2026, 6, 29, 22, 0)
        val member = User(id = 2L, socialId = "member")
        val family = Family(id = 500L, user = user, connectedUser = member, relation = "가족")
        val reverse = Family(id = 501L, user = member, connectedUser = user, relation = "가족")
        val device = userDevice()
        given(userDeviceRepository.findById(20L)).willReturn(Optional.of(device))
        given(userDeviceRepository.findFirstByUser_IdAndIsActiveTrueOrderByCreatedAtDesc(1L)).willReturn(device)
        given(familyRepository.findByIdAndUser_Id(500L, 1L)).willReturn(family)
        given(familyRepository.findByUser_IdAndConnectedUser_Id(2L, 1L)).willReturn(reverse)

        // 생성 이후 설정 변경, 공유 중단, 연결 해제를 각각 재현한다.
        for (scenario in 0..5) {
            device.isMissedAlertOn = scenario != 0
            device.isFamilyReminderOn = scenario != 1
            family.isReceiveAlert = scenario != 2
            reverse.isAllowMyInfo = scenario != 3
            given(familyRepository.findByIdAndUser_Id(500L, 1L))
                .willReturn(if (scenario == 4) null else family)
            val outbox = notificationOutbox(NotificationType.FAMILY_MEDICATION_MISSED)
            givenDueOutboxes(now, listOf(outbox))
            given(notificationOutboxRepository.findById(10L)).willReturn(Optional.of(outbox))

            notificationOutboxWorkerService.processDueOutboxes(now, batchSize = 50)

            assertEquals(if (scenario == 5) NotificationOutboxStatus.SENT else NotificationOutboxStatus.DEAD, outbox.status)
        }
        assertEquals(1, notificationPushSender.requests.size)
    }

    @Test
    fun `주간 알림 생성 이후 본인 알림을 끄면 발송하지 않는다`() {
        val now = LocalDateTime.of(2026, 11, 22, 22, 0)
        val outbox = notificationOutbox(NotificationType.WEEKLY_MEDICATION_SUMMARY)
        val device = userDevice().apply { isMyReminderOn = false }
        givenDueOutboxes(now, listOf(outbox))
        given(userDeviceRepository.findFirstByUser_IdAndIsActiveTrueOrderByCreatedAtDesc(1L)).willReturn(device)
        given(userDeviceRepository.findById(20L)).willReturn(Optional.of(device))
        given(notificationOutboxRepository.findById(10L)).willReturn(Optional.of(outbox))

        notificationOutboxWorkerService.processDueOutboxes(now, batchSize = 50)

        assertEquals(NotificationOutboxStatus.DEAD, outbox.status)
        assertEquals(0, notificationPushSender.requests.size)
    }

    private fun givenDueOutboxes(
        now: LocalDateTime,
        outboxes: List<NotificationOutbox>,
    ) {
        given(
            notificationOutboxRepository.findDueOutboxes(
                pendingStatus = NotificationOutboxStatus.PENDING,
                failedStatus = NotificationOutboxStatus.FAILED,
                now = now,
                pageable = PageRequest.of(0, 50),
            )
        ).willReturn(outboxes)
    }

    private fun notificationOutbox(type: NotificationType = NotificationType.MEDICATION_REMINDER): NotificationOutbox {
        val notificationLog = NotificationLog(
            id = 100L,
            user = user,
            type = type,
            title = "Medication reminder",
            message = "Time to take medicine",
            targetType = NotificationTargetType.MEDICATION_RECORD,
            targetId = 500L,
        )

        return NotificationOutbox(
            id = 10L,
            notificationLog = notificationLog,
            user = user,
            eventKey = "event-key",
            scheduledAt = LocalDateTime.of(2026, 6, 29, 9, 0),
        )
    }

    private fun userDevice(): UserDevice {
        return UserDevice(
            id = 20L,
            user = user,
            deviceToken = "device-token",
            deviceType = "ANDROID",
            isActive = true,
        )
    }

    private class FakeNotificationPushSender : NotificationPushSender {
        var result: NotificationPushResult = NotificationPushResult(NotificationPushStatus.SUCCESS)
        val requests: MutableList<NotificationPushRequest> = mutableListOf()

        override fun send(
            request: NotificationPushRequest,
        ): NotificationPushResult {
            requests.add(request)
            return result
        }
    }

    private class NoOpTransactionManager : PlatformTransactionManager {
        override fun getTransaction(
            definition: TransactionDefinition?,
        ): TransactionStatus {
            return SimpleTransactionStatus()
        }

        override fun commit(
            status: TransactionStatus,
        ) = Unit

        override fun rollback(
            status: TransactionStatus,
        ) = Unit
    }
}
