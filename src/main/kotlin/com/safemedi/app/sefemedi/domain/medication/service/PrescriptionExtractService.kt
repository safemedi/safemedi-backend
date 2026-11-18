package com.safemedi.app.sefemedi.domain.medication.service

import com.safemedi.app.sefemedi.domain.drug.repository.DrugMasterRepository
import com.safemedi.app.sefemedi.domain.medication.dto.PrescriptionExtractResponse
import com.safemedi.app.sefemedi.domain.medication.extraction.AiPrescriptionExtractor
import com.safemedi.app.sefemedi.global.error.BusinessException
import com.safemedi.app.sefemedi.global.error.ErrorCode
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.ByteArrayInputStream
import java.time.LocalDate
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Semaphore
import javax.imageio.ImageIO

@Service
class PrescriptionExtractService(
    private val extractor: AiPrescriptionExtractor,
    private val drugs: DrugMasterRepository,
) {
    private val activeUsers = ConcurrentHashMap.newKeySet<String>()
    private val capacity = Semaphore(4)

    // 외부 AI 응답을 기다리는 동안 DB 트랜잭션을 열지 않는다.
    fun extract(socialId: String, image: MultipartFile): PrescriptionExtractResponse {
        if (!activeUsers.add(socialId)) throw BusinessException(ErrorCode.EXTRACTION_BUSY)
        try {
            if (!capacity.tryAcquire()) throw BusinessException(ErrorCode.EXTRACTION_BUSY)
            try {
                if (image.size > MAX_IMAGE_BYTES) throw BusinessException(ErrorCode.IMAGE_TOO_LARGE)
                val bytes = image.bytes
                val mimeType = validateImage(bytes)
                val result = extractor.extract(bytes, mimeType)
                if (result.medications.isEmpty()) throw BusinessException(ErrorCode.PRESCRIPTION_NOT_READABLE)
                val names = result.medications.map { it.drugName.trim() }.distinct()
                val matches = drugs.findByDrugNameIn(names).groupBy { it.drugName }
                val start = parseDate(result.startDate)
                val end = parseDate(result.endDate)
                val isValidRange = start != null && end != null && !end.isBefore(start) &&
                    !end.isAfter(start.plusMonths(6))
                return result.copy(
                    title = "처방전 스캔 등록",
                    startDate = if (isValidRange) start.toString() else null,
                    endDate = if (isValidRange) end.toString() else null,
                    medications = result.medications.map { medication ->
                        val name = medication.drugName.trim()
                        val match = matches[name]?.singleOrNull()?.takeIf {
                            it.drugCode.isNotBlank() && !it.atcCode.isNullOrBlank()
                        }
                        medication.copy(
                            drugName = name,
                            drugCode = match?.drugCode,
                            atcCode = match?.atcCode?.takeIf { it.isNotBlank() },
                        )
                    },
                )
            } finally {
                capacity.release()
            }
        } finally {
            activeUsers.remove(socialId)
        }
    }

    private fun parseDate(value: String?): LocalDate? = try {
        value?.let(LocalDate::parse)
    } catch (_: java.time.DateTimeException) { null }

    private fun validateImage(bytes: ByteArray): String {
        try {
            ImageIO.createImageInputStream(ByteArrayInputStream(bytes)).use { input ->
                val readers = ImageIO.getImageReaders(input)
                if (!readers.hasNext()) throw BusinessException(ErrorCode.INVALID_PRESCRIPTION_IMAGE)
                val reader = readers.next()
                try {
                    reader.input = input
                    val format = reader.formatName.lowercase()
                    if (format !in setOf("jpeg", "png")) throw BusinessException(ErrorCode.INVALID_PRESCRIPTION_IMAGE)
                    val width = reader.getWidth(0)
                    val height = reader.getHeight(0)
                    if (width <= 0 || height <= 0 || width.toLong() * height > 20_000_000) {
                        throw BusinessException(ErrorCode.INVALID_PRESCRIPTION_IMAGE)
                    }
                    reader.read(0) ?: throw BusinessException(ErrorCode.INVALID_PRESCRIPTION_IMAGE)
                    return if (format == "jpeg") "image/jpeg" else "image/png"
                } finally { reader.dispose() }
            }
        } catch (exception: BusinessException) {
            throw exception
        } catch (_: Exception) {
            throw BusinessException(ErrorCode.INVALID_PRESCRIPTION_IMAGE)
        }
    }

    private companion object { const val MAX_IMAGE_BYTES = 6 * 1024 * 1024 }
}
