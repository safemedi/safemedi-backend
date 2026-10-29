package com.safemedi.app.sefemedi.domain.user.service

import com.safemedi.app.sefemedi.domain.family.repository.FamilyRepository
import com.safemedi.app.sefemedi.domain.medication.repository.PrescriptionDrugRepository
import com.safemedi.app.sefemedi.domain.medication.repository.PrescriptionRepository
import com.safemedi.app.sefemedi.domain.user.dto.AllergyResponse
import com.safemedi.app.sefemedi.domain.user.dto.DiseaseResponse
import com.safemedi.app.sefemedi.domain.user.dto.MedicalSummaryActiveMedicationResponse
import com.safemedi.app.sefemedi.domain.user.dto.MedicalSummaryMedicationResponse
import com.safemedi.app.sefemedi.domain.user.dto.MedicalSummaryResponse
import com.safemedi.app.sefemedi.domain.user.entity.User
import com.safemedi.app.sefemedi.domain.user.repository.UserAllergyRepository
import com.safemedi.app.sefemedi.domain.user.repository.UserDiseaseMapRepository
import com.safemedi.app.sefemedi.domain.user.repository.UserHealthProfileRepository
import com.safemedi.app.sefemedi.domain.user.repository.UserRepository
import com.safemedi.app.sefemedi.global.error.BusinessException
import com.safemedi.app.sefemedi.global.error.ErrorCode
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.ZoneId

@Service
class MedicalSummaryService(
    private val userRepository: UserRepository,
    private val userHealthProfileRepository: UserHealthProfileRepository,
    private val userDiseaseMapRepository: UserDiseaseMapRepository,
    private val userAllergyRepository: UserAllergyRepository,
    private val familyRepository: FamilyRepository,
    private val prescriptionRepository: PrescriptionRepository,
    private val prescriptionDrugRepository: PrescriptionDrugRepository,
) {

    @Transactional(readOnly = true)
    fun getMyMedicalSummary(
        socialId: String,
    ): MedicalSummaryResponse {
        val user = findUserBySocialId(socialId)
        return buildMedicalSummary(user)
    }

    @Transactional(readOnly = true)
    fun getFamilyMedicalSummary(
        socialId: String,
        familyId: Long,
    ): MedicalSummaryResponse {
        val user = findUserBySocialId(socialId)
        val userId = requireUserId(user)
        val family = familyRepository.findByIdAndUser_Id(
            id = familyId,
            userId = userId,
        ) ?: throw BusinessException(ErrorCode.FAMILY_NOT_FOUND)

        val targetUserId = family.connectedUser.id ?: throw BusinessException(ErrorCode.INTERNAL_SERVER_ERROR)
        val reverseFamily = familyRepository.findByUser_IdAndConnectedUser_Id(
            userId = targetUserId,
            connectedUserId = userId,
        )
        if (reverseFamily?.isAllowMyInfo != true) {
            throw BusinessException(ErrorCode.FAMILY_INFO_ACCESS_DENIED)
        }

        return buildMedicalSummary(family.connectedUser)
    }

    private fun buildMedicalSummary(
        user: User,
    ): MedicalSummaryResponse {
        val userId = requireUserId(user)
        val profile = userHealthProfileRepository.findByIdOrNull(userId)
        val diseases = userDiseaseMapRepository.findAllByUser_IdOrderByCreatedAtAsc(userId)
            .map { diseaseMap ->
                DiseaseResponse(
                    code = diseaseMap.disease.diseaseCode,
                    name = diseaseMap.disease.diseaseName,
                )
            }
        val allergies = userAllergyRepository.findAllByUser_IdOrderByCreatedAtAsc(userId)
            .map { allergy ->
                AllergyResponse(
                    type = allergy.allergyType,
                    value = allergy.allergyValue,
                    name = allergy.allergyName,
                )
            }

        return MedicalSummaryResponse(
            name = user.nickname,
            birthDate = profile?.birthDate,
            gender = profile?.gender,
            height = profile?.height,
            weight = profile?.weight,
            bloodType = profile?.bloodType,
            rhType = profile?.rhType,
            diseases = diseases,
            allergies = allergies,
            activeMedications = findActiveMedications(userId),
        )
    }

    private fun findActiveMedications(
        userId: Long,
    ): List<MedicalSummaryActiveMedicationResponse> {
        val today = LocalDate.now(SERVICE_ZONE_ID)
        val prescriptions = prescriptionRepository.findActiveByUserId(
            userId = userId,
            today = today,
        )

        if (prescriptions.isEmpty()) {
            return emptyList()
        }

        val prescriptionIds = prescriptions.mapNotNull { it.id }
        val medicationsByPrescriptionId = prescriptionDrugRepository.findAllByPrescriptionIds(prescriptionIds)
            .groupBy { prescriptionDrug ->
                prescriptionDrug.prescription.id ?: throw BusinessException(ErrorCode.INTERNAL_SERVER_ERROR)
            }

        return prescriptions.map { prescription ->
            val prescriptionId = prescription.id ?: throw BusinessException(ErrorCode.INTERNAL_SERVER_ERROR)
            val medications = medicationsByPrescriptionId[prescriptionId].orEmpty()
                .map { prescriptionDrug ->
                    MedicalSummaryMedicationResponse(
                        drugName = prescriptionDrug.drugName,
                    )
                }

            MedicalSummaryActiveMedicationResponse(
                prescriptionId = prescriptionId,
                prescriptionTitle = prescription.title,
                startDate = prescription.startDate,
                endDate = prescription.endDate,
                medications = medications,
            )
        }
    }

    private fun findUserBySocialId(
        socialId: String,
    ): User {
        return userRepository.findBySocialId(socialId)
            ?: throw BusinessException(ErrorCode.USER_NOT_FOUND)
    }

    private fun requireUserId(
        user: User,
    ): Long {
        return user.id ?: throw BusinessException(ErrorCode.USER_NOT_FOUND)
    }

    private companion object {
        val SERVICE_ZONE_ID: ZoneId = ZoneId.of("Asia/Seoul")
    }
}
