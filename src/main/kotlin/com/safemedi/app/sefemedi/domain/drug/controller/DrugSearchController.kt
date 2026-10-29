package com.safemedi.app.sefemedi.domain.drug.controller

import com.safemedi.app.sefemedi.domain.drug.dto.DrugSearchPageResponse
import com.safemedi.app.sefemedi.domain.drug.service.DrugSearchService
import com.safemedi.app.sefemedi.global.error.BusinessException
import com.safemedi.app.sefemedi.global.error.ErrorCode
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/drugs")
class DrugSearchController(
    private val drugSearchService: DrugSearchService,
) {
    @GetMapping("/search")
    fun search(
        @RequestParam keyword: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): DrugSearchPageResponse {
        val normalizedKeyword = keyword?.trim()
        if (normalizedKeyword.isNullOrEmpty() || normalizedKeyword.length < 2) {
            throw BusinessException(ErrorCode.INVALID_SEARCH_KEYWORD)
        }

        return drugSearchService.search(
            keyword = normalizedKeyword,
            page = page,
            size = size,
        )
    }
}
