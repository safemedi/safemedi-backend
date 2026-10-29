package com.safemedi.app.sefemedi.domain.user.service

import com.safemedi.app.sefemedi.domain.auth.repository.RefreshTokenRepository
import com.safemedi.app.sefemedi.domain.family.repository.FamilyRepository
import com.safemedi.app.sefemedi.domain.medication.repository.MedicationRecordRepository
import com.safemedi.app.sefemedi.domain.medication.repository.PrescriptionDrugRepository
import com.safemedi.app.sefemedi.domain.medication.repository.PrescriptionDrugTimeRepository
import com.safemedi.app.sefemedi.domain.medication.repository.PrescriptionRepository
import com.safemedi.app.sefemedi.domain.notification.repository.NotificationLogRepository
import com.safemedi.app.sefemedi.domain.notification.repository.NotificationOutboxRepository
import com.safemedi.app.sefemedi.domain.user.entity.User
import com.safemedi.app.sefemedi.domain.user.repository.UserAllergyRepository
import com.safemedi.app.sefemedi.domain.user.repository.UserDeviceRepository
import com.safemedi.app.sefemedi.domain.user.repository.UserDiseaseMapRepository
import com.safemedi.app.sefemedi.domain.user.repository.UserHealthProfileRepository
import com.safemedi.app.sefemedi.domain.user.repository.UserRepository
import com.safemedi.app.sefemedi.global.error.BusinessException
import com.safemedi.app.sefemedi.global.error.ErrorCode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.inOrder
import org.mockito.Mockito.mock
import org.mockito.Mockito.verifyNoInteractions

class UserWithdrawalServiceTest {

    private lateinit var userRepository: UserRepository
    private lateinit var userHealthProfileRepository: UserHealthProfileRepository
    private lateinit var userDiseaseMapRepository: UserDiseaseMapRepository
    private lateinit var userAllergyRepository: UserAllergyRepository
    private lateinit var userDeviceRepository: UserDeviceRepository
    private lateinit var familyRepository: FamilyRepository
    private lateinit var refreshTokenRepository: RefreshTokenRepository
    private lateinit var prescriptionRepository: PrescriptionRepository
    private lateinit var prescriptionDrugRepository: PrescriptionDrugRepository
    private lateinit var prescriptionDrugTimeRepository: PrescriptionDrugTimeRepository
    private lateinit var medicationRecordRepository: MedicationRecordRepository
    private lateinit var notificationLogRepository: NotificationLogRepository
    private lateinit var notificationOutboxRepository: NotificationOutboxRepository
    private lateinit var userWithdrawalService: UserWithdrawalService

    @BeforeEach
    fun setUp() {
        userRepository = mock(UserRepository::class.java)
        userHealthProfileRepository = mock(UserHealthProfileRepository::class.java)
        userDiseaseMapRepository = mock(UserDiseaseMapRepository::class.java)
        userAllergyRepository = mock(UserAllergyRepository::class.java)
        userDeviceRepository = mock(UserDeviceRepository::class.java)
        familyRepository = mock(FamilyRepository::class.java)
        refreshTokenRepository = mock(RefreshTokenRepository::class.java)
        prescriptionRepository = mock(PrescriptionRepository::class.java)
        prescriptionDrugRepository = mock(PrescriptionDrugRepository::class.java)
        prescriptionDrugTimeRepository = mock(PrescriptionDrugTimeRepository::class.java)
        medicationRecordRepository = mock(MedicationRecordRepository::class.java)
        notificationLogRepository = mock(NotificationLogRepository::class.java)
        notificationOutboxRepository = mock(NotificationOutboxRepository::class.java)

        userWithdrawalService = UserWithdrawalService(
            userRepository = userRepository,
            userHealthProfileRepository = userHealthProfileRepository,
            userDiseaseMapRepository = userDiseaseMapRepository,
            userAllergyRepository = userAllergyRepository,
            userDeviceRepository = userDeviceRepository,
            familyRepository = familyRepository,
            refreshTokenRepository = refreshTokenRepository,
            prescriptionRepository = prescriptionRepository,
            prescriptionDrugRepository = prescriptionDrugRepository,
            prescriptionDrugTimeRepository = prescriptionDrugTimeRepository,
            medicationRecordRepository = medicationRecordRepository,
            notificationLogRepository = notificationLogRepository,
            notificationOutboxRepository = notificationOutboxRepository,
        )
    }

    @Test
    fun `withdrawMyAccount deletes related data and marks user withdrawn`() {
        val user = User(
            id = 1L,
            nickname = "홍길동",
            socialId = "4903042739",
            inviteCode = "A8F9K2",
            isTutorialCompleted = true,
        )

        given(userRepository.findBySocialIdIncludingDeleted("4903042739")).willReturn(user)
        given(userRepository.saveAndFlush(user)).willReturn(user)

        val response = userWithdrawalService.withdrawMyAccount("4903042739")

        assertEquals("회원 탈퇴가 정상적으로 완료되었습니다.", response.message)
        assertNotNull(user.deletedAt)
        assertNull(user.nickname)
        assertNull(user.inviteCode)
        assertEquals(false, user.isTutorialCompleted)

        val order = inOrder(
            userRepository,
            refreshTokenRepository,
            notificationOutboxRepository,
            notificationLogRepository,
            medicationRecordRepository,
            prescriptionDrugTimeRepository,
            prescriptionDrugRepository,
            prescriptionRepository,
            familyRepository,
            userDeviceRepository,
            userAllergyRepository,
            userDiseaseMapRepository,
            userHealthProfileRepository,
        )

        order.verify(userRepository).findBySocialIdIncludingDeleted("4903042739")
        order.verify(userRepository).saveAndFlush(user)
        order.verify(refreshTokenRepository).deleteByUserId(1L)
        order.verify(notificationOutboxRepository).deleteAllByUserId(1L)
        order.verify(notificationLogRepository).deleteAllByUserId(1L)
        order.verify(medicationRecordRepository).deleteAllByUserId(1L)
        order.verify(prescriptionDrugTimeRepository).deleteAllByUserId(1L)
        order.verify(prescriptionDrugRepository).deleteAllByUserId(1L)
        order.verify(prescriptionRepository).deleteAllByUserId(1L)
        order.verify(familyRepository).deleteAllByUserIdOrConnectedUserId(1L)
        order.verify(userDeviceRepository).deleteAllByUserId(1L)
        order.verify(userAllergyRepository).deleteAllByUserId(1L)
        order.verify(userDiseaseMapRepository).deleteAllByUserId(1L)
        order.verify(userHealthProfileRepository).deleteByUserId(1L)
    }

    @Test
    fun `withdrawMyAccount throws USER_ALREADY_WITHDRAWN when user is already withdrawn`() {
        val user = User(
            id = 1L,
            socialId = "4903042739",
            deletedAt = java.time.LocalDateTime.now(),
        )

        given(userRepository.findBySocialIdIncludingDeleted("4903042739")).willReturn(user)

        val exception = assertThrows(BusinessException::class.java) {
            userWithdrawalService.withdrawMyAccount("4903042739")
        }

        assertEquals(ErrorCode.USER_ALREADY_WITHDRAWN, exception.errorCode)
        verifyNoInteractions(
            refreshTokenRepository,
            notificationOutboxRepository,
            notificationLogRepository,
            medicationRecordRepository,
            prescriptionDrugTimeRepository,
            prescriptionDrugRepository,
            prescriptionRepository,
            familyRepository,
            userDeviceRepository,
            userAllergyRepository,
            userDiseaseMapRepository,
            userHealthProfileRepository,
        )
    }

    @Test
    fun `withdrawMyAccount throws USER_NOT_FOUND when user does not exist`() {
        given(userRepository.findBySocialIdIncludingDeleted("4903042739")).willReturn(null)

        val exception = assertThrows(BusinessException::class.java) {
            userWithdrawalService.withdrawMyAccount("4903042739")
        }

        assertEquals(ErrorCode.USER_NOT_FOUND, exception.errorCode)
        verifyNoInteractions(
            refreshTokenRepository,
            notificationOutboxRepository,
            notificationLogRepository,
            medicationRecordRepository,
            prescriptionDrugTimeRepository,
            prescriptionDrugRepository,
            prescriptionRepository,
            familyRepository,
            userDeviceRepository,
            userAllergyRepository,
            userDiseaseMapRepository,
            userHealthProfileRepository,
        )
    }

    @Test
    fun `withdrawMyAccount throws USER_NOT_FOUND when user id is missing`() {
        val user = User(
            socialId = "4903042739",
        )

        given(userRepository.findBySocialIdIncludingDeleted("4903042739")).willReturn(user)
        given(userRepository.saveAndFlush(user)).willReturn(user)

        val exception = assertThrows(BusinessException::class.java) {
            userWithdrawalService.withdrawMyAccount("4903042739")
        }

        assertEquals(ErrorCode.USER_NOT_FOUND, exception.errorCode)
        verifyNoInteractions(
            refreshTokenRepository,
            notificationOutboxRepository,
            notificationLogRepository,
            medicationRecordRepository,
            prescriptionDrugTimeRepository,
            prescriptionDrugRepository,
            prescriptionRepository,
            familyRepository,
            userDeviceRepository,
            userAllergyRepository,
            userDiseaseMapRepository,
            userHealthProfileRepository,
        )
    }
}
