package com.safemedi.app.sefemedi.domain.medication.service

import com.safemedi.app.sefemedi.domain.drug.entity.DrugMaster
import com.safemedi.app.sefemedi.domain.drug.repository.DrugMasterRepository
import com.safemedi.app.sefemedi.domain.medication.dto.*
import com.safemedi.app.sefemedi.domain.medication.extraction.AiPrescriptionExtractor
import com.safemedi.app.sefemedi.global.error.*
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.SliceImpl
import org.springframework.mock.web.MockMultipartFile
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import kotlin.test.*

class PrescriptionExtractServiceTest {
    private val extractor = mock(AiPrescriptionExtractor::class.java)
    private val drugs = mock(DrugMasterRepository::class.java)
    private val service = PrescriptionExtractService(extractor, drugs)
    private val bytes = ByteArrayOutputStream().also {
        ImageIO.write(BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB), "png", it)
    }.toByteArray()
    private val image = MockMultipartFile("image", "photo.jpg", "image/jpeg", bytes)
    private fun extracted(start: String? = null, end: String? = null) = PrescriptionExtractResponse(
        "임의 제목", start, end, listOf(ExtractedMedication("테스트정", dailyDoseCount = 4, instructions = null)),
    )

    @Test
    fun `괄호 성분명과 밀리그램 표기를 정규화하되 다른 함량은 제외한다`() {
        val medications = listOf("무코바정", "페미로살정5mg").map {
            ExtractedMedication(it, dailyDoseCount = null, instructions = null)
        }
        `when`(extractor.extract(bytes, "image/png")).thenReturn(extracted().copy(medications = medications))
        val page = PageRequest.of(0, 100)
        `when`(drugs.findByDrugNameStartingWith("무코바정", page))
            .thenReturn(SliceImpl(listOf(DrugMaster("D1", "무코바정(레바미피드)", "A01"))))
        `when`(drugs.findByDrugNameStartingWith("페미로살정", page)).thenReturn(SliceImpl(listOf(
            DrugMaster("D2", "페미로살정5밀리그램(페미로라스트칼륨)", "A02"),
            DrugMaster("D3", "페미로살정10밀리그램(페미로라스트칼륨)", "A02"),
        )))
        assertEquals(listOf("D1", "D2"), service.extract("user", image).medications.map { it.drugCode })
    }

    @Test
    fun `실제 이미지 형식으로 추출하고 DB의 유일한 정확한 이름만 연결한다`() {
        `when`(extractor.extract(bytes, "image/png")).thenReturn(extracted())
        `when`(drugs.findByDrugNameStartingWith("테스트정", PageRequest.of(0, 100)))
            .thenReturn(SliceImpl(listOf(DrugMaster("D1", "테스트정", "A01"))))
        val result = service.extract("user", image)
        assertEquals("D1", result.medications.single().drugCode)
        assertEquals(4, result.medications.single().dailyDoseCount)
        assertNull(result.startDate)
    }

    @Test
    fun `이름이 같은 약품이 여러 개면 자동 확정하지 않고 잘못된 날짜를 비운다`() {
        `when`(extractor.extract(bytes, "image/png")).thenReturn(extracted("2026-02-30", "2026-03-03"))
        `when`(drugs.findByDrugNameStartingWith("테스트정", PageRequest.of(0, 100)))
            .thenReturn(SliceImpl(listOf(DrugMaster("D1", "테스트정", "A01"), DrugMaster("D2", "테스트정(성분)", null))))
        val result = service.extract("user", image)
        assertNull(result.medications.single().drugCode)
        assertNull(result.startDate)
        assertNull(result.endDate)
    }

    @Test
    fun `읽을 약물이 없으면 추출 실패를 반환한다`() {
        `when`(extractor.extract(bytes, "image/png")).thenReturn(extracted().copy(medications = emptyList()))
        assertEquals(ErrorCode.PRESCRIPTION_NOT_READABLE, assertFailsWith<BusinessException> {
            service.extract("user", image)
        }.errorCode)
        verifyNoInteractions(drugs)
    }

    @Test
    fun `이미지로 위장한 파일과 용량 초과 파일은 AI 호출 전에 거부한다`() {
        listOf(
            MockMultipartFile("image", "fake.jpg", "image/jpeg", "not image".toByteArray()) to ErrorCode.INVALID_PRESCRIPTION_IMAGE,
            MockMultipartFile("image", ByteArray(10 * 1024 * 1024 + 1)) to ErrorCode.IMAGE_TOO_LARGE,
        ).forEach { (file, expected) ->
            assertEquals(expected, assertFailsWith<BusinessException> { service.extract("user", file) }.errorCode)
        }
        verifyNoInteractions(extractor, drugs)
    }

    @Test
    fun `외부 실패 후에도 같은 사용자가 재시도할 수 있다`() {
        `when`(extractor.extract(bytes, "image/png"))
            .thenThrow(BusinessException(ErrorCode.EXTRACTION_UNAVAILABLE)).thenReturn(extracted())
        `when`(drugs.findByDrugNameStartingWith("테스트정", PageRequest.of(0, 100))).thenReturn(SliceImpl(emptyList()))
        assertFailsWith<BusinessException> { service.extract("user", image) }
        assertEquals(1, service.extract("user", image).medications.size)
    }
}
