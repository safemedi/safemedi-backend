package com.safemedi.app.sefemedi.domain.drug.controller

import com.safemedi.app.sefemedi.domain.drug.dto.DrugAllergySearchPageResponse
import com.safemedi.app.sefemedi.domain.drug.dto.DrugAllergySearchResponse
import com.safemedi.app.sefemedi.domain.drug.service.DrugAllergySearchService
import com.safemedi.app.sefemedi.global.error.ErrorCode
import com.safemedi.app.sefemedi.global.error.GlobalExceptionHandler
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders

class DrugAllergySearchControllerTest {
    private lateinit var mockMvc: MockMvc
    private lateinit var drugAllergySearchService: DrugAllergySearchService

    @BeforeEach
    fun setUp() {
        drugAllergySearchService = mock(DrugAllergySearchService::class.java)
        mockMvc = MockMvcBuilders.standaloneSetup(DrugAllergySearchController(drugAllergySearchService))
            .setControllerAdvice(GlobalExceptionHandler())
            .build()
    }

    @Test
    fun `search returns paged matching drug allergies`() {
        val keyword = "penicillin"
        val mockResponse = DrugAllergySearchPageResponse(
            content = listOf(
                DrugAllergySearchResponse(
                    allergyType = "ATC_GROUP",
                    allergyValue = "J01C",
                    allergyName = "Beta-lactam antibacterials, penicillins",
                )
            ),
            page = 0,
            size = 20,
            isLast = true,
        )
        given(drugAllergySearchService.search(keyword, 0, 20)).willReturn(mockResponse)

        val resultActions = mockMvc.perform(
            get("/api/v1/allergies/drugs/search")
                .param("keyword", keyword)
                .accept(MediaType.APPLICATION_JSON)
        )

        resultActions
            .andExpect(status().isOk)
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.content[0].allergyType").value("ATC_GROUP"))
            .andExpect(jsonPath("$.content[0].allergyValue").value("J01C"))
            .andExpect(jsonPath("$.content[0].allergyName").value("Beta-lactam antibacterials, penicillins"))
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(20))
            .andExpect(jsonPath("$.isLast").value(true))
    }

    @Test
    fun `search uses requested page and size`() {
        val keyword = "penicillin"
        val mockResponse = DrugAllergySearchPageResponse(
            content = emptyList(),
            page = 1,
            size = 10,
            isLast = true,
        )
        given(drugAllergySearchService.search(keyword, 1, 10)).willReturn(mockResponse)

        val resultActions = mockMvc.perform(
            get("/api/v1/allergies/drugs/search")
                .param("keyword", " penicillin ")
                .param("page", "1")
                .param("size", "10")
                .accept(MediaType.APPLICATION_JSON)
        )

        resultActions
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.page").value(1))
            .andExpect(jsonPath("$.size").value(10))
            .andExpect(jsonPath("$.isLast").value(true))
    }

    @Test
    fun `invalid keyword returns validation error`() {
        val resultActions = mockMvc.perform(
            get("/api/v1/allergies/drugs/search")
                .param("keyword", " ")
                .accept(MediaType.APPLICATION_JSON)
        )

        resultActions
            .andExpect(status().isBadRequest)
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.code").value("VAL_007"))
            .andExpect(jsonPath("$.message").value(ErrorCode.INVALID_DRUG_ALLERGY_SEARCH_KEYWORD.message))
    }
}
