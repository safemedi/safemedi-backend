package com.safemedi.app.sefemedi.domain.user.service

import com.safemedi.app.sefemedi.domain.auth.repository.RefreshTokenRepository
import com.safemedi.app.sefemedi.domain.family.repository.FamilyRepository
import com.safemedi.app.sefemedi.domain.medication.repository.MedicationRecordRepository
import com.safemedi.app.sefemedi.domain.medication.repository.PrescriptionDrugRepository
import com.safemedi.app.sefemedi.domain.medication.repository.PrescriptionDrugTimeRepository
import com.safemedi.app.sefemedi.domain.medication.repository.PrescriptionRepository
import com.safemedi.app.sefemedi.domain.notification.repository.NotificationLogRepository
import com.safemedi.app.sefemedi.domain.notification.repository.NotificationOutboxRepository
import com.safemedi.app.sefemedi.domain.user.dto.UserWithdrawalResponse
import com.safemedi.app.sefemedi.domain.user.entity.User
import com.safemedi.app.sefemedi.domain.user.repository.UserAllergyRepository
import com.safemedi.app.sefemedi.domain.user.repository.UserDeviceRepository
import com.safemedi.app.sefemedi.domain.user.repository.UserDiseaseMapRepository
import com.safemedi.app.sefemedi.domain.user.repository.UserHealthProfileRepository
import com.safemedi.app.sefemedi.domain.user.repository.UserRepository
import com.safemedi.app.sefemedi.global.error.BusinessException
import com.safemedi.app.sefemedi.global.error.ErrorCode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.ZoneId

@Service
class UserWithdrawalService(
    private val userRepository: UserRepository,
    private val userHealthProfileRepository: UserHealthProfileRepository,
    private val userDiseaseMapRepository: UserDiseaseMapRepository,
    private val userAllergyRepository: UserAllergyRepository,
    private val userDeviceRepository: UserDeviceRepository,
    private val familyRepository: FamilyRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val prescriptionRepository: PrescriptionRepository,
    private val prescriptionDrugRepository: PrescriptionDrugRepository,
    private val prescriptionDrugTimeRepository: PrescriptionDrugTimeRepository,
    private val medicationRecordRepository: MedicationRecordRepository,
    private val notificationLogRepository: NotificationLogRepository,
    private val notificationOutboxRepository: NotificationOutboxRepository,
) {

    @Transactional
    fun withdrawMyAccount(
        socialId: String,
    ): UserWithdrawalResponse {
        val user = userRepository.findBySocialIdIncludingDeleted(socialId)
            ?: throw BusinessException(ErrorCode.USER_NOT_FOUND)

        if (user.deletedAt != null) {
            throw BusinessException(ErrorCode.USER_ALREADY_WITHDRAWN)
        }

        val withdrawnAt = LocalDateTime.now(SERVICE_ZONE_ID)
        user.withdraw(withdrawnAt)
        userRepository.saveAndFlush(user)

        val userId = requireUserId(user)
        deleteRelatedData(userId)

        return UserWithdrawalResponse()
    }

    private fun deleteRelatedData(userId: Long) {
        refreshTokenRepository.deleteByUserId(userId)
        notificationOutboxRepository.deleteAllByUserId(userId)
        notificationLogRepository.deleteAllByUserId(userId)
        medicationRecordRepository.deleteAllByUserId(userId)
        prescriptionDrugTimeRepository.deleteAllByUserId(userId)
        prescriptionDrugRepository.deleteAllByUserId(userId)
        prescriptionRepository.deleteAllByUserId(userId)
        familyRepository.deleteAllByUserIdOrConnectedUserId(userId)
        userDeviceRepository.deleteAllByUserId(userId)
        userAllergyRepository.deleteAllByUserId(userId)
        userDiseaseMapRepository.deleteAllByUserId(userId)
        userHealthProfileRepository.deleteByUserId(userId)
    }

    private fun requireUserId(user: User): Long {
        return user.id ?: throw BusinessException(ErrorCode.USER_NOT_FOUND)
    }

    private companion object {
        val SERVICE_ZONE_ID: ZoneId = ZoneId.of("Asia/Seoul")
    }
}
