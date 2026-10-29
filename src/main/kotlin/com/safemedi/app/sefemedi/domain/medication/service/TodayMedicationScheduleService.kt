package com.safemedi.app.sefemedi.domain.medication.service

import com.safemedi.app.sefemedi.domain.family.repository.FamilyRepository
import com.safemedi.app.sefemedi.domain.medication.dto.TodayMedicationScheduleItemResponse
import com.safemedi.app.sefemedi.domain.medication.dto.TodayMedicationScheduleResponse
import com.safemedi.app.sefemedi.domain.medication.dto.TodayMedicationSummaryResponse
import com.safemedi.app.sefemedi.domain.medication.entity.MedicationRecord
import com.safemedi.app.sefemedi.domain.medication.entity.MedicationStatus
import com.safemedi.app.sefemedi.domain.medication.repository.MedicationRecordRepository
import com.safemedi.app.sefemedi.domain.user.repository.UserRepository
import com.safemedi.app.sefemedi.global.error.BusinessException
import com.safemedi.app.sefemedi.global.error.ErrorCode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Service
class TodayMedicationScheduleService(
    private val userRepository: UserRepository,
    private val medicationRecordRepository: MedicationRecordRepository,
    private val familyRepository: FamilyRepository,
) {
    @Transactional(readOnly = true)
    fun findTodaySchedules(socialId: String, familyId: Long? = null): TodayMedicationScheduleResponse {
        val user = userRepository.findBySocialId(socialId)
            ?: throw BusinessException(ErrorCode.INVALID_TOKEN)
        val userId = user.id ?: throw BusinessException(ErrorCode.INVALID_TOKEN)
        val targetUserId = resolveTargetUserId(userId, familyId)

        val now = LocalDateTime.now(SERVICE_ZONE_ID)
        val today = LocalDate.now(SERVICE_ZONE_ID)
        val records = medicationRecordRepository.findTodaySchedules(
            userId = targetUserId,
            startAt = today.atStartOfDay(),
            endAt = today.plusDays(1).atStartOfDay(),
        )
        val schedules = records
            .groupBy(::scheduleKey)
            .values
            .map { it.toScheduleItem(now) }
            .sortedWith(compareBy<TodayMedicationScheduleItemResponse> { it.takeTime }
                .thenBy { it.prescriptionId })

        val completedCount = schedules.count { it.displayStatus == DisplayMedicationStatus.SUCCESS.name }
        val totalCount = schedules.size

        return TodayMedicationScheduleResponse(
            date = today,
            summary = TodayMedicationSummaryResponse(
                completedCount = completedCount,
                totalCount = totalCount,
                completionRate = calculateCompletionRate(
                    completedCount = completedCount,
                    totalCount = totalCount,
                ),
            ),
            schedules = schedules,
        )
    }

    private fun resolveTargetUserId(userId: Long, familyId: Long?): Long {
        if (familyId == null) {
            return userId
        }

        val family = familyRepository.findByIdAndUser_Id(id = familyId, userId = userId)
            ?: throw BusinessException(ErrorCode.FAMILY_ACCESS_DENIED)
        val targetUserId = family.connectedUser.id ?: throw BusinessException(ErrorCode.INTERNAL_SERVER_ERROR)
        val reverseFamily = familyRepository.findByUser_IdAndConnectedUser_Id(
            userId = targetUserId,
            connectedUserId = userId,
        )
        if (reverseFamily?.isAllowMyInfo != true) {
            throw BusinessException(ErrorCode.FAMILY_ACCESS_DENIED)
        }

        return targetUserId
    }

    private fun scheduleKey(record: MedicationRecord): ScheduleGroupKey {
        val prescription = record.prescription
        val prescriptionId = prescription.id ?: throw BusinessException(ErrorCode.INTERNAL_SERVER_ERROR)

        return ScheduleGroupKey(
            prescriptionId = prescriptionId,
            scheduledAt = record.scheduledAt,
        )
    }

    private fun List<MedicationRecord>.toScheduleItem(now: LocalDateTime): TodayMedicationScheduleItemResponse {
        val firstRecord = first()
        val prescription = firstRecord.prescription
        val prescriptionId = prescription.id ?: throw BusinessException(ErrorCode.INTERNAL_SERVER_ERROR)
        val recordStatus = resolveRecordStatus(this)
        val displayStatus = resolveDisplayStatus(
            records = this,
            recordStatus = recordStatus,
            now = now,
        )
        val prescriptionDrugs = map { it.prescriptionDrugTime.prescriptionDrug }
            .distinctBy { it.id ?: it.drugName }

        return TodayMedicationScheduleItemResponse(
            takeTime = firstRecord.scheduledAt.toLocalTime().format(TAKE_TIME_FORMATTER),
            recordStatus = recordStatus.name,
            displayStatus = displayStatus.name,
            prescriptionId = prescriptionId,
            prescriptionTitle = prescription.title,
            drugCount = prescriptionDrugs.size,
            drugNames = prescriptionDrugs.map { it.drugName },
            recordIds = mapNotNull { it.id },
        )
    }

    private fun resolveRecordStatus(records: List<MedicationRecord>): MedicationStatus {
        val statuses = records.map { it.status }.toSet()

        return when {
            statuses.size == 1 -> statuses.single()
            MedicationStatus.PENDING in statuses -> MedicationStatus.PENDING
            MedicationStatus.FAIL in statuses -> MedicationStatus.FAIL
            MedicationStatus.SKIP in statuses -> MedicationStatus.SKIP
            else -> MedicationStatus.PENDING
        }
    }

    private fun resolveDisplayStatus(
        records: List<MedicationRecord>,
        recordStatus: MedicationStatus,
        now: LocalDateTime,
    ): DisplayMedicationStatus {
        return when (recordStatus) {
            MedicationStatus.SUCCESS -> DisplayMedicationStatus.SUCCESS
            MedicationStatus.SKIP -> DisplayMedicationStatus.SKIP
            MedicationStatus.FAIL -> DisplayMedicationStatus.MISSED
            MedicationStatus.PENDING -> {
                val scheduledAt = records.minOf { it.scheduledAt }
                when {
                    now.isBefore(scheduledAt) -> DisplayMedicationStatus.WAITING
                    !now.isBefore(scheduledAt.plusHours(MISSED_AFTER_HOURS)) -> DisplayMedicationStatus.MISSED
                    else -> DisplayMedicationStatus.NEED_TAKE
                }
            }
        }
    }

    private fun calculateCompletionRate(
        completedCount: Int,
        totalCount: Int,
    ): Int {
        if (totalCount == 0) {
            return 0
        }

        return completedCount * 100 / totalCount
    }

    private data class ScheduleGroupKey(
        val prescriptionId: Long,
        val scheduledAt: LocalDateTime,
    )

    private enum class DisplayMedicationStatus {
        SUCCESS, NEED_TAKE, WAITING, MISSED, SKIP
    }

    private companion object {
        val SERVICE_ZONE_ID: ZoneId = ZoneId.of("Asia/Seoul")
        val TAKE_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
        const val MISSED_AFTER_HOURS = 4L
    }
}
