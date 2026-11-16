package com.safemedi.app.sefemedi.domain.notification.service

import com.safemedi.app.sefemedi.domain.medication.entity.MedicationRecord
import com.safemedi.app.sefemedi.domain.medication.entity.MedicationStatus
import com.safemedi.app.sefemedi.domain.medication.entity.Prescription
import com.safemedi.app.sefemedi.domain.medication.entity.PrescriptionDrug
import com.safemedi.app.sefemedi.domain.medication.entity.PrescriptionDrugTime
import com.safemedi.app.sefemedi.domain.notification.entity.NotificationType
import com.safemedi.app.sefemedi.domain.notification.repository.NotificationLogRepository
import com.safemedi.app.sefemedi.domain.notification.repository.NotificationOutboxRepository
import com.safemedi.app.sefemedi.domain.user.entity.User
import com.safemedi.app.sefemedi.domain.user.entity.UserDevice
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@SpringBootTest(properties = [
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.datasource.url=jdbc:h2:mem:weekly-summary-test;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false;NON_KEYWORDS=USER",
])
@ActiveProfiles("test")
@Transactional
class WeeklyMedicationSummaryServiceTest @Autowired constructor(
    private val service: WeeklyMedicationSummaryService,
    private val entityManager: EntityManager,
    private val logs: NotificationLogRepository,
    private val outboxes: NotificationOutboxRepository,
) {
    private val now = LocalDateTime.of(2026, 11, 22, 22, 0)
    private var sequence = 0

    @Test
    fun `주간 복약률의 80퍼센트와 40퍼센트 경계에 맞춰 안내하고 주마다 한번만 생성한다`() {
        val cases = listOf(
            Triple(0, 1, "복약 관리에 주의가 필요해요"),
            Triple(39, 100, "복약 관리에 주의가 필요해요"),
            Triple(2, 5, "조금 더 노력해 볼까요?"),
            Triple(79, 100, "조금 더 노력해 볼까요?"),
            Triple(4, 5, "꾸준히 잘 챙겨 드셨어요!"),
            Triple(1, 1, "꾸준히 잘 챙겨 드셨어요!"),
        )
        val users = cases.map { (success, total, _) ->
            val member = user("복약자")
            repeat(total) { index ->
                record(member, if (index < success) MedicationStatus.SUCCESS else MedicationStatus.PENDING)
            }
            member
        }

        service.create(now)
        service.create(now)

        val notifications = logs.findAll()
        assertEquals(cases.size, notifications.size)
        cases.zip(users).forEach { (case, member) ->
            val (success, total, guidance) = case
            val notification = notifications.single { it.user.id == member.id }
            assertEquals(NotificationType.WEEKLY_MEDICATION_SUMMARY, notification.type)
            assertTrue(notification.message.startsWith("이번 주 복약률은 ${success * 100 / total}%예요."))
            assertTrue(notification.message.contains(guidance))
            assertEquals("WEEKLY_MEDICATION_SUMMARY:2026-11-16:${member.id}", notification.deduplicationKey)
        }
        assertEquals(cases.size.toLong(), outboxes.count())
        assertTrue(outboxes.findAll().all { it.scheduledAt == now })
    }

    @Test
    fun `월요일 자정부터 일요일 22시까지 집계하고 삭제된 일정과 기간 밖 기록은 제외한다`() {
        val member = user("복약자")
        val monday = now.toLocalDate().minusDays(6).atStartOfDay()
        record(member, MedicationStatus.SUCCESS, monday)
        record(member, MedicationStatus.SUCCESS, now)
        record(member, MedicationStatus.PENDING, monday.minusSeconds(1))
        record(member, MedicationStatus.PENDING, now.plusSeconds(1))
        record(member, MedicationStatus.PENDING, now.plusDays(1))
        record(member, MedicationStatus.PENDING, deletedPrescription = true)
        record(member, MedicationStatus.PENDING, deletedTime = true)

        service.create(now)

        assertTrue(logs.findAll().single().message.startsWith("이번 주 복약률은 100%예요."))
    }

    @Test
    fun `완료 외의 상태는 미복약으로 계산하고 다음 주에는 새 알림을 생성한다`() {
        val member = user("복약자")
        record(member, MedicationStatus.SUCCESS)
        record(member, MedicationStatus.SKIP)
        record(member, MedicationStatus.FAIL)
        record(member, MedicationStatus.PENDING)
        record(member, MedicationStatus.SUCCESS, now.plusWeeks(1))

        service.create(now)
        service.create(now.plusWeeks(1))

        val notifications = logs.findAll()
        assertEquals(2, notifications.size)
        assertTrue(notifications.any { it.message.startsWith("이번 주 복약률은 25%예요.") })
        assertTrue(notifications.any { it.message.startsWith("이번 주 복약률은 100%예요.") })
    }

    @Test
    fun `복약 예정이 없거나 본인 알림을 끄거나 활성 기기가 없으면 생성하지 않는다`() {
        user("예정없음")
        val disabled = user("본인알림끔", reminder = false)
        val inactive = user("비활성", active = false)
        record(disabled, MedicationStatus.PENDING)
        record(inactive, MedicationStatus.PENDING)

        service.create(now)

        assertEquals(0L, logs.count())
        assertEquals(0L, outboxes.count())
    }

    private fun user(nickname: String, reminder: Boolean = true, active: Boolean = true): User {
        val user = User(socialId = "weekly-${sequence++}", nickname = nickname)
        entityManager.persist(user)
        entityManager.persist(UserDevice(
            user = user, deviceToken = "token-${sequence++}", deviceType = "ANDROID",
            isMyReminderOn = reminder, isActive = active,
        ))
        return user
    }

    private fun record(
        user: User,
        status: MedicationStatus,
        scheduledAt: LocalDateTime = now.minusHours(2),
        deletedPrescription: Boolean = false,
        deletedTime: Boolean = false,
    ): MedicationRecord {
        val prescription = Prescription(
            user = user, title = "처방", startDate = now.toLocalDate(), endDate = now.toLocalDate(),
            deletedAt = if (deletedPrescription) now else null,
        )
        entityManager.persist(prescription)
        val drug = PrescriptionDrug(prescription = prescription, drugName = "약")
        entityManager.persist(drug)
        val time = PrescriptionDrugTime(
            prescriptionDrug = drug, takeTime = scheduledAt.toLocalTime(),
            deletedAt = if (deletedTime) now else null,
        )
        entityManager.persist(time)
        val record = MedicationRecord(
            user = user, prescription = prescription, prescriptionDrugTime = time,
            scheduledAt = scheduledAt, status = status,
        )
        entityManager.persist(record)
        return record
    }
}
