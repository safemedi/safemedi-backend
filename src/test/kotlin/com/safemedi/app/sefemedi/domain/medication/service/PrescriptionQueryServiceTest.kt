package com.safemedi.app.sefemedi.domain.medication.service

import com.safemedi.app.sefemedi.domain.drug.entity.DrugMaster
import com.safemedi.app.sefemedi.domain.medication.entity.Prescription
import com.safemedi.app.sefemedi.domain.medication.entity.PrescriptionDrug
import com.safemedi.app.sefemedi.domain.medication.entity.PrescriptionDrugTime
import com.safemedi.app.sefemedi.domain.medication.repository.PrescriptionDrugCountProjection
import com.safemedi.app.sefemedi.domain.medication.repository.PrescriptionDrugRepository
import com.safemedi.app.sefemedi.domain.medication.repository.PrescriptionDrugTimeRepository
import com.safemedi.app.sefemedi.domain.medication.repository.PrescriptionRepository
import com.safemedi.app.sefemedi.domain.user.entity.User
import com.safemedi.app.sefemedi.domain.user.repository.UserRepository
import com.safemedi.app.sefemedi.global.error.BusinessException
import com.safemedi.app.sefemedi.global.error.ErrorCode
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.SliceImpl
import org.springframework.test.util.ReflectionTestUtils
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PrescriptionQueryServiceTest {
    private lateinit var userRepository: UserRepository
    private lateinit var prescriptionRepository: PrescriptionRepository
    private lateinit var prescriptionDrugRepository: PrescriptionDrugRepository
    private lateinit var prescriptionDrugTimeRepository: PrescriptionDrugTimeRepository
    private lateinit var service: PrescriptionQueryService

    private val user = User(
        id = 1L,
        socialId = "kakao-123",
    )

    @BeforeEach
    fun setUp() {
        userRepository = mock(UserRepository::class.java)
        prescriptionRepository = mock(PrescriptionRepository::class.java)
        prescriptionDrugRepository = mock(PrescriptionDrugRepository::class.java)
        prescriptionDrugTimeRepository = mock(PrescriptionDrugTimeRepository::class.java)

        service = PrescriptionQueryService(
            userRepository = userRepository,
            prescriptionRepository = prescriptionRepository,
            prescriptionDrugRepository = prescriptionDrugRepository,
            prescriptionDrugTimeRepository = prescriptionDrugTimeRepository,
        )
    }

    @Test
    fun `처방전 목록 조회`() {
        val prescription = Prescription(
            id = 25L,
            user = user,
            title = "Kidney prescription",
            hasAllergyConflict = true,
            isDoctorApproved = true,
            startDate = LocalDate.of(2026, 6, 1),
            endDate = LocalDate.of(2026, 6, 7),
        )
        ReflectionTestUtils.setField(
            prescription,
            "createdAt",
            LocalDateTime.of(2026, 6, 5, 10, 0),
        )

        given(userRepository.findBySocialId("kakao-123")).willReturn(user)
        given(
            prescriptionRepository.findByUserIdAndDeletedAtIsNullOrderByCreatedAtDescIdDesc(
                userId = 1L,
                pageable = PageRequest.of(0, 10),
            )
        ).willReturn(SliceImpl(listOf(prescription), PageRequest.of(0, 10), false))
        given(prescriptionDrugRepository.countByPrescriptionIds(listOf(25L)))
            .willReturn(
                listOf(
                    PrescriptionDrugCount(
                        prescriptionId = 25L,
                        drugCount = 3L,
                    )
                )
            )

        val response = service.findPrescriptions(
            socialId = "kakao-123",
            page = 0,
            size = 10,
        )

        assertEquals(true, response.isLast)
        assertEquals(1, response.content.size)
        assertEquals(25L, response.content.single().prescriptionId)
        assertEquals("Kidney prescription", response.content.single().title)
        assertEquals(LocalDate.of(2026, 6, 5), response.content.single().createdAt)
        assertEquals(3, response.content.single().drugCount)
        assertEquals(true, response.content.single().hasAllergyConflict)
    }

    @Test
    fun `페이징 오류`() {
        val exception = assertFailsWith<BusinessException> {
            service.findPrescriptions(
                socialId = "kakao-123",
                page = -1,
                size = 10,
            )
        }

        assertEquals(ErrorCode.INVALID_PAGE_REQUEST, exception.errorCode)
    }

    @Test
    fun `페이지가 많을때 오류`() {
        val exception = assertFailsWith<BusinessException> {
            service.findPrescriptions(
                socialId = "kakao-123",
                page = 0,
                size = 101,
            )
        }

        assertEquals(ErrorCode.INVALID_PAGE_REQUEST, exception.errorCode)
    }

    @Test
    fun `상세조회`() {
        val prescription = Prescription(
            id = 10L,
            user = user,
            title = "ENT cold medicine",
            hasAllergyConflict = false,
            isDoctorApproved = true,
            startDate = LocalDate.of(2026, 6, 1),
            endDate = LocalDate.of(2026, 6, 10),
        )
        val drug = DrugMaster(
            drugCode = "195700007",
            drugName = "Tylenol 500mg",
            atcCode = "N02BE01",
        )
        val prescriptionDrug = PrescriptionDrug(
            id = 1L,
            prescription = prescription,
            drugName = "Tylenol 500mg",
            drug = drug,
            atcCode = "N02BE01",
        )
        val takeTimes = listOf(
            PrescriptionDrugTime(
                id = 1L,
                prescriptionDrug = prescriptionDrug,
                takeTime = LocalTime.of(8, 0),
            ),
            PrescriptionDrugTime(
                id = 2L,
                prescriptionDrug = prescriptionDrug,
                takeTime = LocalTime.of(18, 0),
            ),
        )

        given(userRepository.findBySocialId("kakao-123")).willReturn(user)
        given(prescriptionRepository.findByIdAndUserIdAndDeletedAtIsNull(id = 10L, userId = 1L))
            .willReturn(prescription)
        given(prescriptionDrugRepository.findDetailsByPrescriptionId(10L))
            .willReturn(listOf(prescriptionDrug))
        given(prescriptionDrugTimeRepository.findByPrescriptionDrugIds(listOf(1L)))
            .willReturn(takeTimes)

        val response = service.findPrescriptionDetail(
            socialId = "kakao-123",
            prescriptionId = 10L,
        )

        assertEquals(10L, response.prescriptionId)
        assertEquals("ENT cold medicine", response.title)
        assertEquals(LocalDate.of(2026, 6, 1), response.startDate)
        assertEquals(LocalDate.of(2026, 6, 10), response.endDate)
        assertEquals(true, response.isDoctorApproved)
        assertEquals(false, response.hasAllergyConflict)
        assertEquals(1, response.medications.size)
        assertEquals(1L, response.medications.single().prescriptionDrugId)
        assertEquals("195700007", response.medications.single().drugCode)
        assertEquals("Tylenol 500mg", response.medications.single().drugName)
        assertEquals("N02BE01", response.medications.single().atcCode)
        assertEquals(listOf("08:00", "18:00"), response.medications.single().takeTimes)
    }

    @Test
    fun `상세 조회 오류`() {
        given(userRepository.findBySocialId("kakao-123")).willReturn(user)
        given(prescriptionRepository.findByIdAndUserIdAndDeletedAtIsNull(id = 999L, userId = 1L))
            .willReturn(null)

        val exception = assertFailsWith<BusinessException> {
            service.findPrescriptionDetail(
                socialId = "kakao-123",
                prescriptionId = 999L,
            )
        }

        assertEquals(ErrorCode.PRESCRIPTION_NOT_FOUND, exception.errorCode)
    }

    @Test
    fun `null 이 있어도 성공`() {
        val prescription = Prescription(
            id = 11L,
            user = user,
            title = "Unknown medicine",
            hasAllergyConflict = false,
            isDoctorApproved = false,
            startDate = LocalDate.of(2026, 6, 1),
            endDate = LocalDate.of(2026, 6, 10),
        )
        val prescriptionDrug = PrescriptionDrug(
            id = 2L,
            prescription = prescription,
            drugName = "Unknown medicine",
            drug = null,
            atcCode = null,
        )

        given(userRepository.findBySocialId("kakao-123")).willReturn(user)
        given(prescriptionRepository.findByIdAndUserIdAndDeletedAtIsNull(id = 11L, userId = 1L))
            .willReturn(prescription)
        given(prescriptionDrugRepository.findDetailsByPrescriptionId(11L))
            .willReturn(listOf(prescriptionDrug))
        given(prescriptionDrugTimeRepository.findByPrescriptionDrugIds(listOf(2L)))
            .willReturn(emptyList())

        val response = service.findPrescriptionDetail(
            socialId = "kakao-123",
            prescriptionId = 11L,
        )

        assertEquals(11L, response.prescriptionId)
        assertEquals(1, response.medications.size)
        assertEquals(null, response.medications.single().drugCode)
        assertEquals("Unknown medicine", response.medications.single().drugName)
        assertEquals(null, response.medications.single().atcCode)
        assertEquals(emptyList(), response.medications.single().takeTimes)
    }

    private data class PrescriptionDrugCount(
        override val prescriptionId: Long,
        override val drugCount: Long,
    ) : PrescriptionDrugCountProjection
}
