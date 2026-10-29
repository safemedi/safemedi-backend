package com.safemedi.app.sefemedi.domain.drug.service

import com.safemedi.app.sefemedi.domain.drug.dto.DrugAllergySearchPageResponse
import com.safemedi.app.sefemedi.domain.drug.dto.DrugAllergySearchResponse
import com.safemedi.app.sefemedi.domain.drug.repository.AtcGroupMasterRepository
import com.safemedi.app.sefemedi.global.error.BusinessException
import com.safemedi.app.sefemedi.global.error.ErrorCode
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class DrugAllergySearchService(
    private val atcGroupMasterRepository: AtcGroupMasterRepository,
) {
    @Transactional(readOnly = true)
    fun search(
        keyword: String,
        page: Int,
        size: Int,
    ): DrugAllergySearchPageResponse {
        validatePageRequest(
            page = page,
            size = size,
        )

        val normalizedSize = size.coerceAtMost(MAX_SIZE)
        val atcGroups = atcGroupMasterRepository.searchByKeyword(
            keyword = keyword,
            pageable = PageRequest.of(page, normalizedSize),
        )

        return DrugAllergySearchPageResponse(
            content = atcGroups.content.mapNotNull {
                val allergyName = it.atcNameKo ?: it.atcNameEn ?: return@mapNotNull null
                DrugAllergySearchResponse(
                    allergyType = ALLERGY_TYPE,
                    allergyValue = it.atcCode,
                    allergyName = allergyName,
                )
            },
            page = page,
            size = normalizedSize,
            isLast = atcGroups.isLast,
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
        const val ALLERGY_TYPE = "ATC_GROUP"
        const val MIN_PAGE = 0
        const val MIN_SIZE = 1
        const val MAX_SIZE = 50
    }
}
