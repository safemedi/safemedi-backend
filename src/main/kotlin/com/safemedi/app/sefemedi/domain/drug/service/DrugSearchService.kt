package com.safemedi.app.sefemedi.domain.drug.service

import com.safemedi.app.sefemedi.domain.drug.dto.DrugSearchPageResponse
import com.safemedi.app.sefemedi.domain.drug.dto.DrugSearchResponse
import com.safemedi.app.sefemedi.domain.drug.repository.DrugMasterRepository
import com.safemedi.app.sefemedi.global.error.BusinessException
import com.safemedi.app.sefemedi.global.error.ErrorCode
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class DrugSearchService(
    private val drugMasterRepository: DrugMasterRepository,
) {
    @Transactional(readOnly = true)
    fun search(
        keyword: String,
        page: Int,
        size: Int,
    ): DrugSearchPageResponse {
        validatePageRequest(
            page = page,
            size = size,
        )

        val normalizedSize = size.coerceAtMost(MAX_SIZE)
        val drugs = drugMasterRepository.findByDrugNameContainingAndAtcCodeIsNotNullOrderByDrugNameAsc(
            keyword = keyword,
            pageable = PageRequest.of(page, normalizedSize),
        )

        return DrugSearchPageResponse(
            content = drugs.content.mapNotNull {
                DrugSearchResponse(
                    drugCode = it.drugCode,
                    atcCode = it.atcCode ?: return@mapNotNull null,
                    drugName = it.drugName ?: return@mapNotNull null,
                )
            },
            page = page,
            size = normalizedSize,
            isLast = drugs.isLast,
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
