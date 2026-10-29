package com.safemedi.app.sefemedi.domain.drug.controller

import com.safemedi.app.sefemedi.domain.drug.dto.DiseaseSearchPageResponse
import com.safemedi.app.sefemedi.domain.drug.service.DiseaseSearchService
import com.safemedi.app.sefemedi.global.error.BusinessException
import com.safemedi.app.sefemedi.global.error.ErrorCode
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/diseases")
class DiseaseSearchController(
    private val diseaseSearchService: DiseaseSearchService,
) {
    @GetMapping("/search")
    fun search(
        @RequestParam keyword: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): DiseaseSearchPageResponse {
        val normalizedKeyword = keyword?.trim()
        if (normalizedKeyword.isNullOrEmpty() || normalizedKeyword.length < MIN_KEYWORD_LENGTH) {
            throw BusinessException(ErrorCode.INVALID_DISEASE_SEARCH_KEYWORD)
        }

        return diseaseSearchService.search(
            keyword = normalizedKeyword,
            page = page,
            size = size,
        )
    }

    private companion object {
        const val MIN_KEYWORD_LENGTH = 1
    }
}
