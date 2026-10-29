package com.safemedi.app.sefemedi.domain.drug.controller

import com.safemedi.app.sefemedi.domain.drug.dto.DiseaseSearchPageResponse
import com.safemedi.app.sefemedi.domain.drug.dto.DiseaseSearchResponse
import com.safemedi.app.sefemedi.domain.drug.service.DiseaseSearchService
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

class DiseaseSearchControllerTest {
    private lateinit var mockMvc: MockMvc
    private lateinit var diseaseSearchService: DiseaseSearchService

    @BeforeEach
    fun setUp() {
        diseaseSearchService = mock(DiseaseSearchService::class.java)
        mockMvc = MockMvcBuilders.standaloneSetup(DiseaseSearchController(diseaseSearchService))
            .setControllerAdvice(GlobalExceptionHandler())
            .build()
    }

    @Test
    fun `search returns paged matching diseases`() {
        val keyword = "hypertension"
        val mockResponse = DiseaseSearchPageResponse(
            content = listOf(
                DiseaseSearchResponse(
                    diseaseCode = "I10",
                    diseaseName = "Essential hypertension",
                )
            ),
            page = 0,
            size = 20,
            isLast = true,
        )
        given(diseaseSearchService.search(keyword, 0, 20)).willReturn(mockResponse)

        val resultActions = mockMvc.perform(
            get("/api/v1/diseases/search")
                .param("keyword", keyword)
                .accept(MediaType.APPLICATION_JSON)
        )

        resultActions
            .andExpect(status().isOk)
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.content[0].diseaseCode").value("I10"))
            .andExpect(jsonPath("$.content[0].diseaseName").value("Essential hypertension"))
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(20))
            .andExpect(jsonPath("$.isLast").value(true))
    }

    @Test
    fun `search uses requested page and size`() {
        val keyword = "hypertension"
        val mockResponse = DiseaseSearchPageResponse(
            content = emptyList(),
            page = 1,
            size = 10,
            isLast = true,
        )
        given(diseaseSearchService.search(keyword, 1, 10)).willReturn(mockResponse)

        val resultActions = mockMvc.perform(
            get("/api/v1/diseases/search")
                .param("keyword", " hypertension ")
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
    fun `search allows one character keyword`() {
        val keyword = "a"
        val mockResponse = DiseaseSearchPageResponse(
            content = emptyList(),
            page = 0,
            size = 20,
            isLast = true,
        )
        given(diseaseSearchService.search(keyword, 0, 20)).willReturn(mockResponse)

        val resultActions = mockMvc.perform(
            get("/api/v1/diseases/search")
                .param("keyword", keyword)
                .accept(MediaType.APPLICATION_JSON)
        )

        resultActions
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content").isArray)
            .andExpect(jsonPath("$.size").value(20))
    }

    @Test
    fun `invalid keyword returns validation error`() {
        val resultActions = mockMvc.perform(
            get("/api/v1/diseases/search")
                .param("keyword", " ")
                .accept(MediaType.APPLICATION_JSON)
        )

        resultActions
            .andExpect(status().isBadRequest)
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.code").value("VAL_006"))
            .andExpect(jsonPath("$.message").value(ErrorCode.INVALID_DISEASE_SEARCH_KEYWORD.message))
    }
}
