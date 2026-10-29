package com.safemedi.app.sefemedi.domain.drug.controller

import com.safemedi.app.sefemedi.domain.drug.dto.DrugSearchPageResponse
import com.safemedi.app.sefemedi.domain.drug.dto.DrugSearchResponse
import com.safemedi.app.sefemedi.domain.drug.service.DrugSearchService
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

class DrugSearchControllerTest {
    private lateinit var mockMvc: MockMvc
    private lateinit var drugSearchService: DrugSearchService

    @BeforeEach
    fun setUp() {
        drugSearchService = mock(DrugSearchService::class.java)
        mockMvc = MockMvcBuilders.standaloneSetup(DrugSearchController(drugSearchService))
            .setControllerAdvice(GlobalExceptionHandler())
            .build()
    }

    @Test
    fun `search returns paged matching drugs`() {
        val keyword = "tylenol"
        val mockResponse = DrugSearchPageResponse(
            content = listOf(
                DrugSearchResponse(
                    drugCode = "D001",
                    atcCode = "N02BE01",
                    drugName = "Tylenol 500mg",
                )
            ),
            page = 0,
            size = 20,
            isLast = true,
        )
        given(drugSearchService.search(keyword, 0, 20)).willReturn(mockResponse)

        val resultActions = mockMvc.perform(
            get("/api/v1/drugs/search")
                .param("keyword", keyword)
                .accept(MediaType.APPLICATION_JSON)
        )

        resultActions
            .andExpect(status().isOk)
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.content[0].drugCode").value("D001"))
            .andExpect(jsonPath("$.content[0].atcCode").value("N02BE01"))
            .andExpect(jsonPath("$.content[0].drugName").value("Tylenol 500mg"))
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(20))
            .andExpect(jsonPath("$.isLast").value(true))
    }

    @Test
    fun `search uses requested page and size`() {
        val keyword = "tylenol"
        val mockResponse = DrugSearchPageResponse(
            content = emptyList(),
            page = 1,
            size = 10,
            isLast = true,
        )
        given(drugSearchService.search(keyword, 1, 10)).willReturn(mockResponse)

        val resultActions = mockMvc.perform(
            get("/api/v1/drugs/search")
                .param("keyword", " tylenol ")
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
        val invalidKeyword = "a"

        val resultActions = mockMvc.perform(
            get("/api/v1/drugs/search")
                .param("keyword", invalidKeyword)
                .accept(MediaType.APPLICATION_JSON)
        )

        resultActions
            .andExpect(status().isBadRequest)
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.code").value("VAL_005"))
            .andExpect(jsonPath("$.message").value(ErrorCode.INVALID_SEARCH_KEYWORD.message))
    }
}
