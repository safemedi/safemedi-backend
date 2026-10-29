package com.safemedi.app.sefemedi.domain.medication.service

import com.safemedi.app.sefemedi.domain.medication.dto.PrescriptionDeleteResponse
import com.safemedi.app.sefemedi.domain.medication.entity.MedicationStatus
import com.safemedi.app.sefemedi.domain.medication.repository.MedicationRecordRepository
import com.safemedi.app.sefemedi.domain.medication.repository.PrescriptionRepository
import com.safemedi.app.sefemedi.domain.user.repository.UserRepository
import com.safemedi.app.sefemedi.global.error.BusinessException
import com.safemedi.app.sefemedi.global.error.ErrorCode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.ZoneId

@Service
class PrescriptionDeleteService(
    private val userRepository: UserRepository,
    private val prescriptionRepository: PrescriptionRepository,
    private val medicationRecordRepository: MedicationRecordRepository,
) {
    @Transactional
    fun delete(
        socialId: String,
        prescriptionId: Long,
    ): PrescriptionDeleteResponse {
        val user = userRepository.findBySocialId(socialId)
            ?: throw BusinessException(ErrorCode.INVALID_TOKEN)
        val userId = user.id ?: throw BusinessException(ErrorCode.INVALID_TOKEN)
        val prescription = prescriptionRepository.findByIdAndDeletedAtIsNull(prescriptionId)
            ?: throw BusinessException(ErrorCode.PRESCRIPTION_NOT_FOUND)

        if (prescription.user.id != userId) {
            throw BusinessException(ErrorCode.PRESCRIPTION_ACCESS_DENIED)
        }

        val now = LocalDateTime.now(SERVICE_ZONE_ID)
        prescription.delete(now)
        medicationRecordRepository.deleteFuturePendingByPrescriptionId(
            prescriptionId = prescriptionId,
            status = MedicationStatus.PENDING,
            now = now,
        )

        return PrescriptionDeleteResponse()
    }

    private companion object {
        val SERVICE_ZONE_ID: ZoneId = ZoneId.of("Asia/Seoul")
    }
}
