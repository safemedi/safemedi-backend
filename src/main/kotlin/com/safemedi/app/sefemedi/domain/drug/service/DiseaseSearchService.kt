package com.safemedi.app.sefemedi.domain.drug.service

import com.safemedi.app.sefemedi.domain.drug.dto.DiseaseSearchPageResponse
import com.safemedi.app.sefemedi.domain.drug.dto.DiseaseSearchResponse
import com.safemedi.app.sefemedi.domain.drug.repository.DiseaseMasterRepository
import com.safemedi.app.sefemedi.global.error.BusinessException
import com.safemedi.app.sefemedi.global.error.ErrorCode
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class DiseaseSearchService(
    private val diseaseMasterRepository: DiseaseMasterRepository,
) {
    @Transactional(readOnly = true)
    fun search(
        keyword: String,
        page: Int,
        size: Int,
    ): DiseaseSearchPageResponse {
        validatePageRequest(
            page = page,
            size = size,
        )

        val normalizedSize = size.coerceAtMost(MAX_SIZE)
        val diseases = diseaseMasterRepository.findByDiseaseNameContainingOrderByDiseaseNameAsc(
            keyword = keyword,
            pageable = PageRequest.of(page, normalizedSize),
        )

        return DiseaseSearchPageResponse(
            content = diseases.content.map {
                DiseaseSearchResponse(
                    diseaseCode = it.diseaseCode,
                    diseaseName = it.diseaseName,
                )
            },
            page = page,
            size = normalizedSize,
            isLast = diseases.isLast,
        )
    }

    private fun validatePageRequest(
        page: Int,
        size: Int,
    ) {
        if (page < MIN_PAGE || size < MIN_SIZE) {
            throw BusinessException(ErrorCode.INVALID_PAGE_REQUEST)
        }
    }

    private companion object {
        const val MIN_PAGE = 0
        const val MIN_SIZE = 1
        const val MAX_SIZE = 50
    }
}
