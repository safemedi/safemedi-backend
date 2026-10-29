package com.safemedi.app.sefemedi.domain.medication.service

import com.safemedi.app.sefemedi.domain.drug.repository.DrugIngredientMapRepository
import com.safemedi.app.sefemedi.domain.drug.repository.DrugMasterRepository
import com.safemedi.app.sefemedi.domain.medication.analyzer.MedicationAnalysisTarget
import com.safemedi.app.sefemedi.domain.medication.analyzer.PrescriptionAnalyzer
import com.safemedi.app.sefemedi.domain.medication.analyzer.PrescriptionContext
import com.safemedi.app.sefemedi.domain.medication.dto.AnalyzedMedicationResponse
import com.safemedi.app.sefemedi.domain.medication.dto.MedicationSafetyStatus
import com.safemedi.app.sefemedi.domain.medication.dto.PrescriptionAnalyzeRequest
import com.safemedi.app.sefemedi.domain.medication.dto.PrescriptionAnalyzeResponse
import com.safemedi.app.sefemedi.domain.medication.dto.SafetySummaryResponse
import com.safemedi.app.sefemedi.domain.notification.dto.NotificationCreateCommand
import com.safemedi.app.sefemedi.domain.notification.entity.NotificationType
import com.safemedi.app.sefemedi.domain.notification.service.NotificationCreateService
import com.safemedi.app.sefemedi.domain.user.repository.UserAllergyRepository
import com.safemedi.app.sefemedi.domain.user.repository.UserHealthProfileRepository
import com.safemedi.app.sefemedi.domain.user.repository.UserRepository
import com.safemedi.app.sefemedi.global.error.BusinessException
import com.safemedi.app.sefemedi.global.error.ErrorCode
import org.slf4j.LoggerFactory
import org.springframework.core.annotation.AnnotationAwareOrderComparator
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PrescriptionAnalyzeService(
    private val userRepository: UserRepository,
    private val userAllergyRepository: UserAllergyRepository,
    private val userHealthProfileRepository: UserHealthProfileRepository,
    private val drugIngredientMapRepository: DrugIngredientMapRepository,
    private val drugMasterRepository: DrugMasterRepository,
    private val notificationCreateService: NotificationCreateService,
    analyzers: List<PrescriptionAnalyzer>,
) {
    private val sortedAnalyzers: List<PrescriptionAnalyzer> =
        analyzers.sortedWith(AnnotationAwareOrderComparator.INSTANCE)
    private val log = LoggerFactory.getLogger(PrescriptionAnalyzeService::class.java)

    @Transactional
    fun analyze(
        socialId: String,
        request: PrescriptionAnalyzeRequest,
    ): PrescriptionAnalyzeResponse {
        if (request.medications.isEmpty()) {
            throw BusinessException(ErrorCode.EMPTY_MEDICATIONS)
        }

        val user = userRepository.findBySocialId(socialId)
            ?: throw BusinessException(ErrorCode.INVALID_TOKEN)
        val userId = user.id ?: throw BusinessException(ErrorCode.INVALID_TOKEN)
        val allergies = userAllergyRepository.findAllByUser_IdOrderByCreatedAtAsc(userId)
        val healthProfile = userHealthProfileRepository.findById(userId).orElse(null)
        val drugCodes = request.medications.map { it.drugCode.trim() }
        if (drugCodes.any { it.isBlank() }) {
            throw BusinessException(ErrorCode.INVALID_REQUEST)
        }

        val drugsByCode = drugMasterRepository.findAllById(drugCodes)
            .associateBy { it.drugCode.uppercase() }
        val medications = drugCodes.map { drugCode ->
            val drug = drugsByCode[drugCode.uppercase()] ?: throw BusinessException(ErrorCode.INVALID_REQUEST)
            MedicationAnalysisTarget(
                drugCode = drug.drugCode,
                atcCode = drug.atcCode ?: throw BusinessException(ErrorCode.INVALID_REQUEST),
                drugName = drug.drugName ?: throw BusinessException(ErrorCode.INVALID_REQUEST),
            )
        }
        val ingredientMaps = drugIngredientMapRepository.findAllByDrugCodes(drugCodes)

        val context = PrescriptionContext(
            user = user,
            healthProfile = healthProfile,
            allergies = allergies,
            medications = medications,
            ingredientMaps = ingredientMaps,
        )

        sortedAnalyzers.forEach { it.analyze(context) }

        val response = context.toResponse()
        createDrugRiskWarningIfNeeded(
            userId = userId,
            drugCodes = drugCodes,
            response = response,
        )

        return response
    }

    private fun PrescriptionContext.toResponse(): PrescriptionAnalyzeResponse {
        val analyzedResponses = analyzedMedications.map {
            AnalyzedMedicationResponse(
                atcCode = it.medication.atcCode,
                drugName = it.medication.drugName,
                status = it.status,
                efficacy = it.efficacy,
                precautions = it.precautions,
                warnings = it.warnings,
            )
        }

        return PrescriptionAnalyzeResponse(
            safetySummary = SafetySummaryResponse(
                safeCount = analyzedResponses.count { it.status == MedicationSafetyStatus.SAFE },
                warningCount = analyzedResponses.count { it.status == MedicationSafetyStatus.WARNING },
                dangerCount = analyzedResponses.count { it.status == MedicationSafetyStatus.DANGER },
            ),
            analyzedMedications = analyzedResponses,
        )
    }

    private fun createDrugRiskWarningIfNeeded(
        userId: Long,
        drugCodes: List<String>,
        response: PrescriptionAnalyzeResponse,
    ) {
        val dangerMedications = response.analyzedMedications.filter {
            it.status == MedicationSafetyStatus.DANGER
        }
        if (dangerMedications.isEmpty()) {
            return
        }

        val drugNames = dangerMedications
            .map { it.drugName }
            .distinct()
            .joinToString(", ")
        val normalizedDrugCodes = drugCodes
            .map { it.trim().uppercase() }
            .distinct()
            .sorted()
            .joinToString(",")

        try {
            notificationCreateService.create(
            NotificationCreateCommand(
                userId = userId,
                type = NotificationType.DRUG_INTERACTION_WARNING,
                title = "약물 위험 경고",
                content = "${drugNames} 약물에 위험 경고가 있어요",
                deduplicationKey = "DRUG_RISK_WARNING:ANALYZE:$userId:$normalizedDrugCodes",
            )
            )
        } catch (exception: RuntimeException) {
            log.warn(
                "Failed to create drug risk notification. userId={}, drugCodes={}",
                userId,
                normalizedDrugCodes,
                exception,
            )
        }
    }
}
