package com.safemedi.app.sefemedi.domain.drug.controller

import com.safemedi.app.sefemedi.domain.drug.dto.DrugAllergySearchPageResponse
import com.safemedi.app.sefemedi.domain.drug.service.DrugAllergySearchService
import com.safemedi.app.sefemedi.global.error.BusinessException
import com.safemedi.app.sefemedi.global.error.ErrorCode
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/allergies/drugs")
class DrugAllergySearchController(
    private val drugAllergySearchService: DrugAllergySearchService,
) {
    @GetMapping("/search")
    fun search(
        @RequestParam keyword: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): DrugAllergySearchPageResponse {
        val normalizedKeyword = keyword?.trim()
        if (normalizedKeyword.isNullOrEmpty()) {
            throw BusinessException(ErrorCode.INVALID_DRUG_ALLERGY_SEARCH_KEYWORD)
        }

        return drugAllergySearchService.search(
            keyword = normalizedKeyword,
            page = page,
            size = size,
        )
    }
}
