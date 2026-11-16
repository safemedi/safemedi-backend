package com.safemedi.app.sefemedi.domain.medication.service

import com.safemedi.app.sefemedi.domain.medication.dto.MedicationRecordUpdateRequest
import com.safemedi.app.sefemedi.domain.medication.dto.MedicationRecordUpdateResponse
import com.safemedi.app.sefemedi.domain.medication.entity.MedicationRecord
import com.safemedi.app.sefemedi.domain.medication.entity.MedicationStatus
import com.safemedi.app.sefemedi.domain.medication.repository.MedicationRecordRepository
import com.safemedi.app.sefemedi.domain.user.repository.UserRepository
import com.safemedi.app.sefemedi.global.error.BusinessException
import com.safemedi.app.sefemedi.global.error.ErrorCode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.ZoneId

@Service
class MedicationRecordUpdateService(
    private val userRepository: UserRepository,
    private val medicationRecordRepository: MedicationRecordRepository,
) {
    @Transactional
    fun update(
        socialId: String,
        request: MedicationRecordUpdateRequest,
    ): MedicationRecordUpdateResponse {
        validateRequestedStatus(request.status)
        val requestedIds = request.recordIds.toSet()
        if (requestedIds.isEmpty()) {
            throw BusinessException(ErrorCode.INVALID_REQUEST)
        }

        val user = userRepository.findBySocialId(socialId)
            ?: throw BusinessException(ErrorCode.INVALID_TOKEN)
        val userId = user.id ?: throw BusinessException(ErrorCode.INVALID_TOKEN)
        val records = medicationRecordRepository.findActiveAllByIdIn(requestedIds)

        if (records.mapNotNull { it.id }.toSet() != requestedIds || records.any { it.user.id != userId }) {
            throw BusinessException(ErrorCode.MEDICATION_RECORD_NOT_FOUND)
        }
        if (records.groupBy { it.prescription.id to it.scheduledAt }.size != 1) {
            throw BusinessException(ErrorCode.INVALID_REQUEST)
        }
        validateGroupIsComplete(records, userId)
        if (request.status != MedicationStatus.PENDING && records.any { it.status != MedicationStatus.PENDING }) {
            throw BusinessException(ErrorCode.MEDICATION_RECORD_ALREADY_PROCESSED)
        }

        val takenAt = when (request.status) {
            MedicationStatus.SUCCESS -> LocalDateTime.now(SERVICE_ZONE_ID)
            MedicationStatus.SKIP,
            MedicationStatus.PENDING -> null
            MedicationStatus.FAIL -> throw BusinessException(ErrorCode.INVALID_ENUM_VALUE)
        }
        records.forEach {
            it.updateStatus(
                status = request.status,
                takenAt = takenAt,
            )
        }

        return records.toResponse()
    }

    private fun validateRequestedStatus(status: MedicationStatus) {
        if (status !in ALLOWED_REQUEST_STATUSES) {
            throw BusinessException(ErrorCode.INVALID_ENUM_VALUE)
        }
    }

    private fun validateGroupIsComplete(
        records: List<MedicationRecord>,
        userId: Long,
    ) {
        val anchor = records.first()
        val prescriptionId = anchor.prescription.id ?: throw BusinessException(ErrorCode.INTERNAL_SERVER_ERROR)
        val fullGroupIds = medicationRecordRepository.findAllByPrescriptionIdAndScheduledAtAndUserId(
            prescriptionId = prescriptionId,
            scheduledAt = anchor.scheduledAt,
            userId = userId,
        ).mapNotNull { it.id }.toSet()

        if (fullGroupIds != records.mapNotNull { it.id }.toSet()) {
            throw BusinessException(ErrorCode.INVALID_REQUEST)
        }
    }

    private fun List<MedicationRecord>.toResponse(): MedicationRecordUpdateResponse {
        val firstRecord = first()
        return MedicationRecordUpdateResponse(
            recordIds = mapNotNull { it.id },
            prescriptionId = firstRecord.prescription.id ?: throw BusinessException(ErrorCode.INTERNAL_SERVER_ERROR),
            scheduledAt = firstRecord.scheduledAt,
            drugNames = map { it.prescriptionDrugTime.prescriptionDrug.drugName }.distinct(),
            takenAt = firstRecord.takenAt,
            status = firstRecord.status.name,
        )
    }

    private companion object {
        val SERVICE_ZONE_ID: ZoneId = ZoneId.of("Asia/Seoul")
        val ALLOWED_REQUEST_STATUSES = setOf(
            MedicationStatus.SUCCESS,
            MedicationStatus.SKIP,
            MedicationStatus.PENDING,
        )
    }
}
