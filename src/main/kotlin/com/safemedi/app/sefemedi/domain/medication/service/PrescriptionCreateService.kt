package com.safemedi.app.sefemedi.domain.medication.service

import com.safemedi.app.sefemedi.domain.drug.repository.DrugMasterRepository
import com.safemedi.app.sefemedi.domain.medication.dto.PrescriptionCreateRequest
import com.safemedi.app.sefemedi.domain.medication.dto.PrescriptionCreateResponse
import com.safemedi.app.sefemedi.domain.medication.entity.MedicationRecord
import com.safemedi.app.sefemedi.domain.medication.entity.Prescription
import com.safemedi.app.sefemedi.domain.medication.entity.PrescriptionDrug
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
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

@Service
class PrescriptionCreateService(
    private val userRepository: UserRepository,
    private val drugMasterRepository: DrugMasterRepository,
    private val prescriptionRepository: PrescriptionRepository,
    private val prescriptionDrugRepository: PrescriptionDrugRepository,
    private val prescriptionDrugTimeRepository: PrescriptionDrugTimeRepository,
    private val medicationRecordRepository: MedicationRecordRepository,
) {
    @Transactional
    fun create(
        socialId: String,
        request: PrescriptionCreateRequest,
    ): PrescriptionCreateResponse {
        validateRequest(request)

        val user = userRepository.findBySocialId(socialId)
            ?: throw BusinessException(ErrorCode.INVALID_TOKEN)
        val drugCodes = request.medications.map { it.drugCode.trim() }
        val drugsByCode = drugMasterRepository.findAllById(drugCodes)
            .associateBy { it.drugCode.uppercase() }

        val prescription = prescriptionRepository.save(
            Prescription(
                user = user,
                title = request.title,
                isDoctorApproved = request.isDoctorApproved,
                startDate = request.startDate,
                endDate = request.endDate,
            )
        )

        val prescriptionDrugs = request.medications.map { medication ->
            val drugCode = medication.drugCode.trim()
            val drug = drugsByCode[drugCode.uppercase()]
                ?: throw BusinessException(ErrorCode.INVALID_REQUEST)
            val drugName = medication.drugName?.takeIf { it.isNotBlank() }
                ?: drug.drugName
                ?: throw BusinessException(ErrorCode.INVALID_REQUEST)
            val atcCode = medication.atcCode?.takeIf { it.isNotBlank() }
                ?: drug.atcCode
                ?: throw BusinessException(ErrorCode.INVALID_REQUEST)

            PrescriptionDrug(
                prescription = prescription,
                drugName = drugName,
                drug = drug,
                atcCode = atcCode,
            )
        }.let { prescriptionDrugRepository.saveAll(it).toList() }

        val prescriptionDrugTimes = request.medications.zip(prescriptionDrugs).flatMap { (medication, prescriptionDrug) ->
            medication.takeTimes.map { takeTime ->
                PrescriptionDrugTime(
                    prescriptionDrug = prescriptionDrug,
                    takeTime = parseTakeTime(takeTime),
                )
            }
        }.let { prescriptionDrugTimeRepository.saveAll(it) }

        medicationRecordRepository.saveAll(
            createMedicationRecords(
                prescription = prescription,
                prescriptionDrugTimes = prescriptionDrugTimes,
            )
        )

        return PrescriptionCreateResponse(
            prescriptionId = prescription.id ?: throw BusinessException(ErrorCode.INTERNAL_SERVER_ERROR),
        )
    }

    private fun validateRequest(request: PrescriptionCreateRequest) {
        validateTitle(request.title)

        if (request.endDate.isBefore(request.startDate)) {
            throw BusinessException(ErrorCode.INVALID_PRESCRIPTION_DATE)
        }

        if (request.endDate.isAfter(request.startDate.plusMonths(MAX_PRESCRIPTION_PERIOD_MONTHS))) {
            throw BusinessException(ErrorCode.INVALID_PRESCRIPTION_DATE)
        }

        if (request.medications.isEmpty()) {
            throw BusinessException(ErrorCode.EMPTY_MEDICATIONS)
        }

        request.medications.forEach {
            if (it.drugCode.isBlank()) {
                throw BusinessException(ErrorCode.INVALID_REQUEST)
            }
            if (it.takeTimes.isEmpty()) {
                throw BusinessException(ErrorCode.INVALID_TAKE_TIMES)
            }
            it.takeTimes.forEach(::parseTakeTime)
        }
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

    private fun createMedicationRecords(
        prescription: Prescription,
        prescriptionDrugTimes: Iterable<PrescriptionDrugTime>,
    ): List<MedicationRecord> {
        val records = mutableListOf<MedicationRecord>()
        var date = prescription.startDate

        while (!date.isAfter(prescription.endDate)) {
            prescriptionDrugTimes.forEach { prescriptionDrugTime ->
                records.add(
                    MedicationRecord(
                        user = prescription.user,
                        prescription = prescription,
                        prescriptionDrugTime = prescriptionDrugTime,
                        scheduledAt = date.atTime(prescriptionDrugTime.takeTime),
                    )
                )
            }
            date = date.plusDays(1)
        }

        return records
    }

    private companion object {
        const val MAX_TITLE_LENGTH = 255
        const val MAX_PRESCRIPTION_PERIOD_MONTHS = 6L
        val TAKE_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    }
}
