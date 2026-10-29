package com.safemedi.app.sefemedi.domain.medication.service

import com.safemedi.app.sefemedi.domain.medication.dto.PrescriptionMedicationUpdateRequest
import com.safemedi.app.sefemedi.domain.medication.dto.PrescriptionUpdateRequest
import com.safemedi.app.sefemedi.domain.medication.dto.PrescriptionUpdateResponse
import com.safemedi.app.sefemedi.domain.medication.entity.MedicationRecord
import com.safemedi.app.sefemedi.domain.medication.entity.MedicationStatus
import com.safemedi.app.sefemedi.domain.medication.entity.Prescription
import com.safemedi.app.sefemedi.domain.medication.entity.PrescriptionDrugTime
import com.safemedi.app.sefemedi.domain.medication.repository.MedicationRecordRepository
import com.safemedi.app.sefemedi.domain.medication.repository.PrescriptionDrugRepository
import com.safemedi.app.sefemedi.domain.medication.repository.PrescriptionDrugTimeRepository
import com.safemedi.app.sefemedi.domain.medication.repository.PrescriptionRepository
import com.safemedi.app.sefemedi.domain.user.repository.UserRepository
import com.safemedi.app.sefemedi.global.error.BusinessException
import com.safemedi.app.sefemedi.global.error.ErrorCode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

@Service
class PrescriptionUpdateService(
    private val userRepository: UserRepository,
    private val prescriptionRepository: PrescriptionRepository,
    private val prescriptionDrugRepository: PrescriptionDrugRepository,
    private val prescriptionDrugTimeRepository: PrescriptionDrugTimeRepository,
    private val medicationRecordRepository: MedicationRecordRepository,
) {
    @Transactional
    fun update(
        socialId: String,
        prescriptionId: Long,
        request: PrescriptionUpdateRequest,
    ): PrescriptionUpdateResponse {
        val user = userRepository.findBySocialId(socialId)
            ?: throw BusinessException(ErrorCode.INVALID_TOKEN)
        val userId = user.id ?: throw BusinessException(ErrorCode.INVALID_TOKEN)
        val prescription = prescriptionRepository.findByIdAndDeletedAtIsNull(prescriptionId)
            ?: throw BusinessException(ErrorCode.PRESCRIPTION_NOT_FOUND)

        if (prescription.user.id != userId) {
            throw BusinessException(ErrorCode.PRESCRIPTION_ACCESS_DENIED)
        }

        request.title?.let {
            validateTitle(it)
            prescription.updateTitle(it)
        }

        val medicationRequests = request.medications.orEmpty()
        if (medicationRequests.isNotEmpty()) {
            updateMedicationTimes(
                prescription = prescription,
                medicationRequests = medicationRequests,
            )
        }

        return PrescriptionUpdateResponse(
            prescriptionId = prescription.id ?: throw BusinessException(ErrorCode.INTERNAL_SERVER_ERROR),
            title = prescription.title,
        )
    }

    private fun updateMedicationTimes(
        prescription: Prescription,
        medicationRequests: List<PrescriptionMedicationUpdateRequest>,
    ) {
        val now = LocalDateTime.now(SERVICE_ZONE_ID)
        val today = LocalDate.now(SERVICE_ZONE_ID)

        if (prescription.endDate.isBefore(today)) {
            throw BusinessException(ErrorCode.ENDED_PRESCRIPTION_UPDATE_NOT_ALLOWED)
        }

        val prescriptionId = prescription.id ?: throw BusinessException(ErrorCode.INTERNAL_SERVER_ERROR)
        val prescriptionDrugIds = medicationRequests.map { it.prescriptionDrugId }
        if (prescriptionDrugIds.distinct().size != prescriptionDrugIds.size) {
            throw BusinessException(ErrorCode.INVALID_REQUEST)
        }

        medicationRequests.forEach { validateTakeTimes(it.takeTimes) }

        val prescriptionDrugs = prescriptionDrugRepository.findByPrescriptionIdAndIds(
            prescriptionId = prescriptionId,
            prescriptionDrugIds = prescriptionDrugIds,
        )
        if (prescriptionDrugs.size != prescriptionDrugIds.size) {
            throw BusinessException(ErrorCode.PRESCRIPTION_DRUG_NOT_FOUND)
        }

        val prescriptionDrugsById = prescriptionDrugs.associateBy {
            it.id ?: throw BusinessException(ErrorCode.INTERNAL_SERVER_ERROR)
        }

        val activeTimes = prescriptionDrugTimeRepository.findByPrescriptionDrugIds(prescriptionDrugIds)
        activeTimes.forEach { it.delete(now) }

        medicationRecordRepository.deleteFuturePendingByPrescriptionIdAndPrescriptionDrugIds(
            prescriptionId = prescriptionId,
            prescriptionDrugIds = prescriptionDrugIds,
            status = MedicationStatus.PENDING,
            now = now,
        )

        val newTimes = medicationRequests.flatMap { request ->
            val prescriptionDrug = prescriptionDrugsById[request.prescriptionDrugId]
                ?: throw BusinessException(ErrorCode.PRESCRIPTION_DRUG_NOT_FOUND)

            request.takeTimes.map {
                PrescriptionDrugTime(
                    prescriptionDrug = prescriptionDrug,
                    takeTime = parseTakeTime(it),
                )
            }
        }.let { prescriptionDrugTimeRepository.saveAll(it).toList() }

        medicationRecordRepository.saveAll(
            createFutureMedicationRecords(
                prescription = prescription,
                prescriptionDrugTimes = newTimes,
                now = now,
                today = today,
            )
        )
    }

    private fun validateTakeTimes(takeTimes: List<String>) {
        if (takeTimes.isEmpty() || takeTimes.distinct().size != takeTimes.size) {
            throw BusinessException(ErrorCode.INVALID_TAKE_TIMES)
        }
        takeTimes.forEach(::parseTakeTime)
    }

    private fun validateTitle(title: String) {
        if (title.isBlank() || title.length > MAX_TITLE_LENGTH) {
            throw BusinessException(ErrorCode.INVALID_REQUEST)
        }
    }

    private fun parseTakeTime(takeTime: String): LocalTime {
        return try {
            LocalTime.parse(takeTime, TAKE_TIME_FORMATTER)
        } catch (exception: DateTimeParseException) {
            throw BusinessException(ErrorCode.INVALID_TAKE_TIMES)
        }
    }

    private fun createFutureMedicationRecords(
        prescription: Prescription,
        prescriptionDrugTimes: Iterable<PrescriptionDrugTime>,
        now: LocalDateTime,
        today: LocalDate,
    ): List<MedicationRecord> {
        val records = mutableListOf<MedicationRecord>()
        var date = maxOf(prescription.startDate, today)

        while (!date.isAfter(prescription.endDate)) {
            prescriptionDrugTimes.forEach { prescriptionDrugTime ->
                val scheduledAt = date.atTime(prescriptionDrugTime.takeTime)
                if (scheduledAt.isAfter(now)) {
                    records.add(
                        MedicationRecord(
                            user = prescription.user,
                            prescription = prescription,
                            prescriptionDrugTime = prescriptionDrugTime,
                            scheduledAt = scheduledAt,
                        )
                    )
                }
            }
            date = date.plusDays(1)
        }

        return records
    }

    private companion object {
        const val MAX_TITLE_LENGTH = 255
        val SERVICE_ZONE_ID: ZoneId = ZoneId.of("Asia/Seoul")
        val TAKE_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    }
}
