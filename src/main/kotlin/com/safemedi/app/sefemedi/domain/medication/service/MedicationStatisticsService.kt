package com.safemedi.app.sefemedi.domain.medication.service

import com.safemedi.app.sefemedi.domain.family.repository.FamilyRepository
import com.safemedi.app.sefemedi.domain.medication.dto.DailyMedicationComplianceResponse
import com.safemedi.app.sefemedi.domain.medication.dto.MedicationStatisticsResponse
import com.safemedi.app.sefemedi.domain.medication.entity.MedicationRecord
import com.safemedi.app.sefemedi.domain.medication.entity.MedicationStatus
import com.safemedi.app.sefemedi.domain.medication.repository.MedicationRecordRepository
import com.safemedi.app.sefemedi.domain.user.entity.User
import com.safemedi.app.sefemedi.domain.user.repository.UserRepository
import com.safemedi.app.sefemedi.global.error.BusinessException
import com.safemedi.app.sefemedi.global.error.ErrorCode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit

@Service
class MedicationStatisticsService(
    private val userRepository: UserRepository,
    private val familyRepository: FamilyRepository,
    private val medicationRecordRepository: MedicationRecordRepository,
) {

    @Transactional(readOnly = true)
    fun findStatistics(
        socialId: String,
        startDate: String?,
        endDate: String?,
        familyId: Long?,
    ): MedicationStatisticsResponse {
        val user = userRepository.findBySocialId(socialId)
            ?: throw BusinessException(ErrorCode.USER_NOT_FOUND)
        val userId = requireUserId(user)
        val parsedStartDate = parseRequiredDate(startDate)
        val parsedEndDate = parseRequiredDate(endDate)
        validateDateRange(parsedStartDate, parsedEndDate)

        val target = resolveTargetUser(
            userId = userId,
            familyId = familyId,
        )

        val records = medicationRecordRepository.findStatisticsRecords(
            userId = target.userId,
            startAt = parsedStartDate.atStartOfDay(),
            endAt = resolveEndAt(parsedEndDate),
        )
        val totalCount = records.size
        val takenCount = records.countTaken()

        return MedicationStatisticsResponse(
            startDate = parsedStartDate,
            endDate = parsedEndDate,
            familyId = familyId,
            relation = target.relation,
            totalCount = totalCount,
            takenCount = takenCount,
            fraction = "$takenCount/$totalCount",
            dailyCompliance = records.toDailyCompliance(),
        )
    }

    private fun resolveTargetUser(
        userId: Long,
        familyId: Long?,
    ): StatisticsTarget {
        if (familyId == null) {
            return StatisticsTarget(
                userId = userId,
                relation = null,
            )
        }

        val family = familyRepository.findByIdAndUser_Id(
            id = familyId,
            userId = userId,
        ) ?: throw BusinessException(ErrorCode.FAMILY_ACCESS_DENIED)

        val targetUserId = family.connectedUser.id ?: throw BusinessException(ErrorCode.INTERNAL_SERVER_ERROR)
        val reverseFamily = familyRepository.findByUser_IdAndConnectedUser_Id(
            userId = targetUserId,
            connectedUserId = userId,
        )
        if (reverseFamily?.isAllowMyInfo != true) {
            throw BusinessException(ErrorCode.FAMILY_ACCESS_DENIED)
        }

        return StatisticsTarget(
            userId = targetUserId,
            relation = family.relation,
        )
    }

    private fun parseRequiredDate(
        value: String?,
    ): LocalDate {
        val trimmed = value?.trim()
        if (trimmed.isNullOrBlank()) {
            throw BusinessException(ErrorCode.STATISTICS_DATE_REQUIRED)
        }

        return try {
            LocalDate.parse(trimmed)
        } catch (_: DateTimeParseException) {
            throw BusinessException(ErrorCode.STATISTICS_INVALID_DATE_FORMAT)
        }
    }

    private fun validateDateRange(
        startDate: LocalDate,
        endDate: LocalDate,
    ) {
        if (startDate.isAfter(endDate)) {
            throw BusinessException(ErrorCode.STATISTICS_INVALID_DATE_RANGE)
        }

        if (ChronoUnit.DAYS.between(startDate, endDate) > MAX_QUERY_DAYS) {
            throw BusinessException(ErrorCode.STATISTICS_DATE_RANGE_TOO_LONG)
        }
    }

    private fun resolveEndAt(
        endDate: LocalDate,
    ): LocalDateTime {
        val requestedEndAt = endDate.plusDays(1).atStartOfDay()
        val now = LocalDateTime.now(SERVICE_ZONE_ID)

        return if (requestedEndAt.isAfter(now)) now else requestedEndAt
    }

    private fun List<MedicationRecord>.toDailyCompliance(): List<DailyMedicationComplianceResponse> {
        return groupBy { it.scheduledAt.toLocalDate() }
            .toSortedMap()
            .map { (date, records) ->
                val totalCount = records.size
                val takenCount = records.countTaken()
                DailyMedicationComplianceResponse(
                    date = date,
                    takenCount = takenCount,
                    totalCount = totalCount,
                    fraction = "$takenCount/$totalCount",
                )
            }
    }

    private fun List<MedicationRecord>.countTaken(): Int {
        return count { it.status == MedicationStatus.SUCCESS }
    }

    private fun requireUserId(
        user: User,
    ): Long {
        return user.id ?: throw BusinessException(ErrorCode.USER_NOT_FOUND)
    }

    private data class StatisticsTarget(
        val userId: Long,
        val relation: String?,
    )

    private companion object {
        val SERVICE_ZONE_ID: ZoneId = ZoneId.of("Asia/Seoul")
        const val MAX_QUERY_DAYS = 366L
    }
}
