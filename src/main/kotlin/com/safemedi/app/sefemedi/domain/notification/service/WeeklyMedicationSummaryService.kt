package com.safemedi.app.sefemedi.domain.notification.service

import com.safemedi.app.sefemedi.domain.medication.entity.MedicationStatus
import com.safemedi.app.sefemedi.domain.medication.repository.MedicationRecordRepository
import com.safemedi.app.sefemedi.domain.notification.dto.NotificationCreateCommand
import com.safemedi.app.sefemedi.domain.notification.entity.NotificationType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.temporal.TemporalAdjusters

@Service
class WeeklyMedicationSummaryService(
    private val medicationRecordRepository: MedicationRecordRepository,
    private val notificationCreateService: NotificationCreateService,
) {
    @Transactional
    fun create(now: LocalDateTime) {
        val weekStart = now.toLocalDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val cutoff = weekStart.plusDays(6).atTime(22, 0)
        medicationRecordRepository.findAdherenceSummaries(
            startAt = weekStart.atStartOfDay(),
            endAt = cutoff,
            successStatus = MedicationStatus.SUCCESS,
        ).forEach { summary ->
            // 소수점은 버려 표시하며, 80%와 40% 경계에서도 동일한 기준을 사용한다.
            val percentage = summary.successCount * 100 / summary.totalCount
            val guidance = when {
                percentage >= 80 -> "꾸준히 잘 챙겨 드셨어요! 다음 주에도 함께 건강을 지켜요!"
                percentage >= 40 -> "조금 더 노력해 볼까요? 다음 주에는 복약 시간을 더 잘 챙겨 주세요!"
                else -> "복약 관리에 주의가 필요해요. 복약 일정을 확인하고 꾸준히 챙겨 주세요!"
            }
            notificationCreateService.create(
                NotificationCreateCommand(
                    userId = summary.userId,
                    type = NotificationType.WEEKLY_MEDICATION_SUMMARY,
                    title = "이번 주 복약 안내",
                    content = "이번 주 복약률은 ${percentage}%예요. $guidance",
                    deduplicationKey = "WEEKLY_MEDICATION_SUMMARY:$weekStart:${summary.userId}",
                    scheduledAt = cutoff,
                ),
            )
        }
    }
}
