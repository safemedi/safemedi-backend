package com.safemedi.app.sefemedi.domain.medication.service

import com.safemedi.app.sefemedi.domain.medication.dto.PrescriptionDetailResponse
import com.safemedi.app.sefemedi.domain.medication.dto.PrescriptionListResponse
import com.safemedi.app.sefemedi.domain.medication.dto.PrescriptionMedicationResponse
import com.safemedi.app.sefemedi.domain.medication.dto.PrescriptionSummaryResponse
import com.safemedi.app.sefemedi.domain.medication.repository.PrescriptionDrugRepository
import com.safemedi.app.sefemedi.domain.medication.repository.PrescriptionDrugTimeRepository
import com.safemedi.app.sefemedi.domain.medication.repository.PrescriptionRepository
import com.safemedi.app.sefemedi.domain.user.repository.UserRepository
import com.safemedi.app.sefemedi.global.error.BusinessException
import com.safemedi.app.sefemedi.global.error.ErrorCode
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.format.DateTimeFormatter

@Service
class PrescriptionQueryService(
    private val userRepository: UserRepository,
    private val prescriptionRepository: PrescriptionRepository,
    private val prescriptionDrugRepository: PrescriptionDrugRepository,
    private val prescriptionDrugTimeRepository: PrescriptionDrugTimeRepository,
) {
    @Transactional(readOnly = true)
    fun findPrescriptions(
        socialId: String,
        page: Int,
        size: Int,
    ): PrescriptionListResponse {
        validatePageRequest(page, size)

        val user = userRepository.findBySocialId(socialId)
            ?: throw BusinessException(ErrorCode.INVALID_TOKEN)
        val userId = user.id ?: throw BusinessException(ErrorCode.INVALID_TOKEN)

        val prescriptions = prescriptionRepository.findByUserIdAndDeletedAtIsNullOrderByCreatedAtDescIdDesc(
            userId = userId,
            pageable = PageRequest.of(page, size),
        )
        val prescriptionIds = prescriptions.content.mapNotNull { it.id }
        val drugCountsByPrescriptionId = findDrugCounts(prescriptionIds)

        return PrescriptionListResponse(
            content = prescriptions.content.map {
                val prescriptionId = it.id ?: throw BusinessException(ErrorCode.INTERNAL_SERVER_ERROR)

                PrescriptionSummaryResponse(
                    prescriptionId = prescriptionId,
                    title = it.title,
                    createdAt = it.createdAt?.toLocalDate()
                        ?: throw BusinessException(ErrorCode.INTERNAL_SERVER_ERROR),
                    drugCount = drugCountsByPrescriptionId[prescriptionId] ?: 0,
                    hasAllergyConflict = it.hasAllergyConflict,
                )
            },
            isLast = prescriptions.isLast,
        )
    }

    @Transactional(readOnly = true)
    fun findPrescriptionDetail(
        socialId: String,
        prescriptionId: Long,
    ): PrescriptionDetailResponse {
        val user = userRepository.findBySocialId(socialId)
            ?: throw BusinessException(ErrorCode.INVALID_TOKEN)
        val userId = user.id ?: throw BusinessException(ErrorCode.INVALID_TOKEN)
        val prescription = prescriptionRepository.findByIdAndUserIdAndDeletedAtIsNull(
            id = prescriptionId,
            userId = userId,
        ) ?: throw BusinessException(ErrorCode.PRESCRIPTION_NOT_FOUND)

        val prescriptionDrugs = prescriptionDrugRepository.findDetailsByPrescriptionId(prescriptionId)
        val prescriptionDrugIds = prescriptionDrugs.map {
            it.id ?: throw BusinessException(ErrorCode.INTERNAL_SERVER_ERROR)
        }
        val takeTimesByPrescriptionDrugId = findTakeTimes(prescriptionDrugIds)

        return PrescriptionDetailResponse(
            prescriptionId = prescription.id ?: throw BusinessException(ErrorCode.INTERNAL_SERVER_ERROR),
            title = prescription.title,
            startDate = prescription.startDate,
            endDate = prescription.endDate,
            isDoctorApproved = prescription.isDoctorApproved,
            hasAllergyConflict = prescription.hasAllergyConflict,
            medications = prescriptionDrugs.map {
                val prescriptionDrugId = it.id ?: throw BusinessException(ErrorCode.INTERNAL_SERVER_ERROR)

                PrescriptionMedicationResponse(
                    prescriptionDrugId = prescriptionDrugId,
                    drugCode = it.drug?.drugCode,
                    drugName = it.drugName,
                    atcCode = it.atcCode,
                    takeTimes = takeTimesByPrescriptionDrugId[prescriptionDrugId].orEmpty(),
                )
            },
        )
    }

    private fun validatePageRequest(
        page: Int,
        size: Int,
    ) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw BusinessException(ErrorCode.INVALID_PAGE_REQUEST)
        }
    }

    private fun findDrugCounts(prescriptionIds: List<Long>): Map<Long, Int> {
        if (prescriptionIds.isEmpty()) {
            return emptyMap()
        }

        return prescriptionDrugRepository.countByPrescriptionIds(prescriptionIds)
            .associate { it.prescriptionId to it.drugCount.toInt() }
    }

    private fun findTakeTimes(prescriptionDrugIds: List<Long>): Map<Long, List<String>> {
        if (prescriptionDrugIds.isEmpty()) {
            return emptyMap()
        }

        return prescriptionDrugTimeRepository.findByPrescriptionDrugIds(prescriptionDrugIds)
            .groupBy(
                keySelector = {
                    it.prescriptionDrug.id ?: throw BusinessException(ErrorCode.INTERNAL_SERVER_ERROR)
                },
                valueTransform = { it.takeTime.format(TAKE_TIME_FORMATTER) },
            )
    }

    companion object {
        private const val MAX_PAGE_SIZE = 100
        private val TAKE_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    }
}
