package com.safemedi.app.sefemedi.domain.notification.service

import com.safemedi.app.sefemedi.domain.medication.dto.MedicationRecordUpdateRequest
import com.safemedi.app.sefemedi.domain.medication.service.MedicationRecordUpdateService
import com.safemedi.app.sefemedi.domain.family.entity.Family
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
    "spring.datasource.url=jdbc:h2:mem:family-missed-test;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false;NON_KEYWORDS=USER",
])
@ActiveProfiles("test")
@Transactional
class FamilyMedicationMissedServiceTest @Autowired constructor(
    private val service: FamilyMedicationMissedService,
    private val recordUpdateService: MedicationRecordUpdateService,
    private val entityManager: EntityManager,
    private val logs: NotificationLogRepository,
    private val outboxes: NotificationOutboxRepository,
) {
    private val now = LocalDateTime.of(2026, 11, 16, 22, 0)
    private var sequence = 0

    @Test
    fun `복약 완료 처리 후에도 알림함과 발송 대기열에 알림을 생성하지 않는다`() {
        val member = user("복약자")
        val record = record(member, MedicationStatus.PENDING)

        val response = recordUpdateService.update(
            requireNotNull(member.socialId),
            MedicationRecordUpdateRequest(recordIds = listOf(requireNotNull(record.id)), status = MedicationStatus.SUCCESS),
        )

        assertEquals("SUCCESS", response.status)
        assertEquals(0L, logs.count())
        assertEquals(0L, outboxes.count())
    }

    @Test
    fun `복약률이 80퍼센트 미만이면 연결 가족 모두에게 닉네임으로 하루 한번 알린다`() {
        val member = user("할머니")
        val first = user("첫째")
        val second = user("둘째")
        val firstFamily = connect(first, member)
        connect(second, member)
        record(member, MedicationStatus.SUCCESS)
        record(member, MedicationStatus.PENDING)
        record(member, MedicationStatus.SKIP)
        record(member, MedicationStatus.FAIL)

        service.create(now)
        service.create(now)

        val notifications = logs.findAll()
        assertEquals(setOf(first.id, second.id), notifications.map { it.user.id }.toSet())
        assertEquals(2, notifications.size)
        assertTrue(notifications.all { it.type == NotificationType.FAMILY_MEDICATION_MISSED })
        assertTrue(notifications.all { it.message == "할머니님이 약을 다 먹지 않았어요. 건강을 위해 함께 노력해요!" })
        assertEquals(firstFamily.id, notifications.single { it.user.id == first.id }.targetId)
        assertEquals(2L, outboxes.count())
        assertTrue(outboxes.findAll().all { it.scheduledAt == now })
    }

    @Test
    fun `정확히 80퍼센트와 100퍼센트 및 예정건수가 없는 가족은 알리지 않는다`() {
        val recipient = user("수신자")
        val eighty = user("80퍼센트")
        val complete = user("완료")
        val empty = user("예정없음")
        listOf(eighty, complete, empty).forEach { connect(recipient, it) }
        repeat(4) { record(eighty, MedicationStatus.SUCCESS) }
        record(eighty, MedicationStatus.PENDING)
        record(complete, MedicationStatus.SUCCESS)

        service.create(now)

        assertEquals(0L, logs.count())
    }

    @Test
    fun `당일 자정과 22시는 포함하고 다른 날짜와 22시 이후 및 삭제된 일정은 제외한다`() {
        val recipient = user("수신자")
        val member = user("가족원")
        connect(recipient, member)
        record(member, MedicationStatus.SUCCESS, now.toLocalDate().atStartOfDay())
        record(member, MedicationStatus.SUCCESS, now)
        record(member, MedicationStatus.PENDING, now.plusSeconds(1))
        record(member, MedicationStatus.PENDING, now.toLocalDate().atStartOfDay().minusSeconds(1))
        record(member, MedicationStatus.PENDING, now.plusDays(1))
        record(member, MedicationStatus.PENDING, deletedPrescription = true)
        record(member, MedicationStatus.PENDING, deletedTime = true)

        service.create(now)

        assertEquals(0L, logs.count())
        record(member, MedicationStatus.PENDING, now)
        service.create(now)
        assertEquals(1L, logs.count())
    }

    @Test
    fun `복약률은 같은 약을 여러번 복용하는 경우에도 예정건수로 계산한다`() {
        val recipient = user("수신자")
        val member = user("가족원")
        connect(recipient, member)
        val first = record(member, MedicationStatus.SUCCESS)
        // 같은 약의 두 번째 복용 예정건도 분모에 포함한다.
        entityManager.persist(MedicationRecord(
            user = member, prescription = first.prescription,
            prescriptionDrugTime = first.prescriptionDrugTime,
            scheduledAt = now.minusHours(1), status = MedicationStatus.PENDING,
        ))

        service.create(now)

        assertEquals(1L, logs.count())
    }

    @Test
    fun `가족별 수신 설정 또는 정보 공유가 꺼져 있거나 역방향 연결이 없으면 알리지 않는다`() {
        val member = user("가족원")
        val receiveOff = user("가족알림끔")
        val sharingOff = user("공유안함")
        val oneWay = user("단방향")
        connect(receiveOff, member).isReceiveAlert = false
        connect(sharingOff, member, share = false)
        entityManager.persist(Family(user = oneWay, connectedUser = member, relation = "가족"))
        record(member, MedicationStatus.PENDING)

        service.create(now)

        assertEquals(0L, logs.count())
    }

    @Test
    fun `수신자의 미복약 알림 또는 가족 알림이 꺼져 있거나 활성 기기가 없으면 알리지 않는다`() {
        val member = user("가족원")
        val missedOff = user("미복약알림끔", missed = false)
        val familyOff = user("가족알림끔", family = false)
        val inactive = user("비활성", active = false)
        listOf(missedOff, familyOff, inactive).forEach { connect(it, member) }
        record(member, MedicationStatus.PENDING)

        service.create(now)

        assertEquals(0L, logs.count())
    }

    @Test
    fun `복약 시간 변경 전의 미복약 기록도 포함하여 가족에게 알린다`() {
        val recipient = user("수신자")
        val member = user("가족원")
        connect(recipient, member)
        record(member, MedicationStatus.SUCCESS)
        repeat(4) {
            val missed = record(member, MedicationStatus.PENDING, now.minusHours(3))
            missed.prescriptionDrugTime.delete(now.minusHours(1))
        }

        service.create(now)

        assertEquals(recipient.id, logs.findAll().single().user.id)
        assertEquals(1L, outboxes.count())
    }

    @Test
    fun `처방 삭제 이전 미복약 기록은 가족 알림 집계에 남는다`() {
        val recipient = user("수신자")
        val member = user("가족원")
        connect(recipient, member)
        val deletionTime = now.minusHours(1)
        val completed = record(member, MedicationStatus.SUCCESS, deletionTime)
        completed.prescription.delete(deletionTime)
        completed.prescriptionDrugTime.delete(deletionTime)
        val missed = record(member, MedicationStatus.FAIL, deletionTime.minusHours(1))
        missed.prescription.delete(deletionTime)

        service.create(now)

        assertEquals(recipient.id, logs.findAll().single().user.id)
    }

    private fun user(nickname: String, missed: Boolean = true, family: Boolean = true, active: Boolean = true): User {
        val user = User(socialId = "missed-${sequence++}", nickname = nickname)
        entityManager.persist(user)
        entityManager.persist(UserDevice(
            user = user, deviceToken = "token-${sequence++}", deviceType = "ANDROID",
            isMissedAlertOn = missed, isFamilyReminderOn = family, isActive = active,
        ))
        return user
    }

    private fun connect(recipient: User, member: User, share: Boolean = true): Family {
        val family = Family(user = recipient, connectedUser = member, relation = "가족")
        entityManager.persist(family)
        entityManager.persist(Family(user = member, connectedUser = recipient, relation = "가족", isAllowMyInfo = share))
        return family
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
            deletedAt = if (deletedPrescription) scheduledAt.minusSeconds(1) else null,
        )
        entityManager.persist(prescription)
        val drug = PrescriptionDrug(prescription = prescription, drugName = "약")
        entityManager.persist(drug)
        val time = PrescriptionDrugTime(
            prescriptionDrug = drug, takeTime = scheduledAt.toLocalTime(),
            deletedAt = if (deletedTime) scheduledAt.minusSeconds(1) else null,
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
