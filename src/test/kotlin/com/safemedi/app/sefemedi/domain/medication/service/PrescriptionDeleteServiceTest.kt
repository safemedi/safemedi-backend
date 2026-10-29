package com.safemedi.app.sefemedi.domain.medication.service

import com.safemedi.app.sefemedi.domain.medication.entity.Prescription
import com.safemedi.app.sefemedi.domain.medication.entity.MedicationStatus
import com.safemedi.app.sefemedi.domain.medication.repository.MedicationRecordRepository
import com.safemedi.app.sefemedi.domain.medication.repository.PrescriptionRepository
import com.safemedi.app.sefemedi.domain.user.entity.User
import com.safemedi.app.sefemedi.domain.user.repository.UserRepository
import com.safemedi.app.sefemedi.global.error.BusinessException
import com.safemedi.app.sefemedi.global.error.ErrorCode
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

class PrescriptionDeleteServiceTest {
    private lateinit var userRepository: UserRepository
    private lateinit var prescriptionRepository: PrescriptionRepository
    private lateinit var medicationRecordRepository: MedicationRecordRepository
    private lateinit var service: PrescriptionDeleteService

    private val user = User(
        id = 1L,
        socialId = "kakao-123",
    )

    @BeforeEach
    fun setUp() {
        userRepository = mock(UserRepository::class.java)
        prescriptionRepository = mock(PrescriptionRepository::class.java)
        medicationRecordRepository = mock(MedicationRecordRepository::class.java)

        service = PrescriptionDeleteService(
            userRepository = userRepository,
            prescriptionRepository = prescriptionRepository,
            medicationRecordRepository = medicationRecordRepository,
        )
    }

    @Test
    fun `처방전을 삭제 처리하고 미래 예정 복약 기록을 삭제한다`() {
        val prescription = Prescription(
            id = 10L,
            user = user,
            title = "Prescription",
            startDate = LocalDate.of(2026, 6, 1),
            endDate = LocalDate.of(2026, 6, 7),
        )

        given(userRepository.findBySocialId("kakao-123")).willReturn(user)
        given(prescriptionRepository.findByIdAndDeletedAtIsNull(10L)).willReturn(prescription)

        val response = service.delete(
            socialId = "kakao-123",
            prescriptionId = 10L,
        )

        assertEquals("처방전이 삭제 처리되었으며, 예정된 복약 스케줄이 삭제되었습니다.", response.message)
        assertNotNull(prescription.deletedAt)
        verify(medicationRecordRepository).deleteFuturePendingByPrescriptionId(
            prescriptionId = 10L,
            status = MedicationStatus.PENDING,
            now = prescription.deletedAt ?: throw AssertionError("deletedAt should be set"),
        )
    }

    @Test
    fun `존재하지 않는 처방전이면 오류`() {
        given(userRepository.findBySocialId("kakao-123")).willReturn(user)
        given(prescriptionRepository.findByIdAndDeletedAtIsNull(999L)).willReturn(null)

        val exception = assertFailsWith<BusinessException> {
            service.delete(
                socialId = "kakao-123",
                prescriptionId = 999L,
            )
        }

        assertEquals(ErrorCode.PRESCRIPTION_NOT_FOUND, exception.errorCode)
    }

    @Test
    fun `본인 처방전이 아니면 오류`() {
        val otherUserPrescription = Prescription(
            id = 10L,
            user = User(id = 2L, socialId = "kakao-456"),
            title = "Prescription",
            startDate = LocalDate.of(2026, 6, 1),
            endDate = LocalDate.of(2026, 6, 7),
        )

        given(userRepository.findBySocialId("kakao-123")).willReturn(user)
        given(prescriptionRepository.findByIdAndDeletedAtIsNull(10L)).willReturn(otherUserPrescription)

        val exception = assertFailsWith<BusinessException> {
            service.delete(
                socialId = "kakao-123",
                prescriptionId = 10L,
            )
        }

        assertEquals(ErrorCode.PRESCRIPTION_ACCESS_DENIED, exception.errorCode)
    }
}
