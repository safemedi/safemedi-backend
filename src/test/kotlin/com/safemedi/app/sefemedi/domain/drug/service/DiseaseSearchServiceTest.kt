package com.safemedi.app.sefemedi.domain.drug.service

import com.safemedi.app.sefemedi.domain.drug.entity.DiseaseMaster
import com.safemedi.app.sefemedi.domain.drug.repository.DiseaseMasterRepository
import com.safemedi.app.sefemedi.global.error.BusinessException
import com.safemedi.app.sefemedi.global.error.ErrorCode
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.SliceImpl
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DiseaseSearchServiceTest {
    private lateinit var diseaseMasterRepository: DiseaseMasterRepository
    private lateinit var diseaseSearchService: DiseaseSearchService

    @BeforeEach
    fun setUp() {
        diseaseMasterRepository = mock(DiseaseMasterRepository::class.java)
        diseaseSearchService = DiseaseSearchService(diseaseMasterRepository)
    }

    @Test
    fun `search returns paged disease search result`() {
        val disease = DiseaseMaster(
            diseaseCode = "I10",
            diseaseName = "Essential hypertension",
        )

        given(
            diseaseMasterRepository.findByDiseaseNameContainingOrderByDiseaseNameAsc(
                keyword = "hypertension",
                pageable = PageRequest.of(0, 20),
            )
        ).willReturn(SliceImpl(listOf(disease), PageRequest.of(0, 20), false))

        val response = diseaseSearchService.search(
            keyword = "hypertension",
            page = 0,
            size = 20,
        )

        assertEquals(0, response.page)
        assertEquals(20, response.size)
        assertEquals(true, response.isLast)
        assertEquals(1, response.content.size)

        val item = response.content.single()
        assertEquals("I10", item.diseaseCode)
        assertEquals("Essential hypertension", item.diseaseName)
    }

    @Test
    fun `search coerces size when size exceeds max`() {
        given(
            diseaseMasterRepository.findByDiseaseNameContainingOrderByDiseaseNameAsc(
                keyword = "hypertension",
                pageable = PageRequest.of(0, 50),
            )
        ).willReturn(SliceImpl(emptyList(), PageRequest.of(0, 50), false))

        val response = diseaseSearchService.search(
            keyword = "hypertension",
            page = 0,
            size = 100,
        )

        assertEquals(50, response.size)
    }

    @Test
    fun `search throws when page is invalid`() {
        val exception = assertFailsWith<BusinessException> {
            diseaseSearchService.search(
                keyword = "hypertension",
                page = -1,
                size = 20,
            )
        }

        assertEquals(ErrorCode.INVALID_PAGE_REQUEST, exception.errorCode)
    }

    @Test
    fun `search throws when size is less than minimum`() {
        val exception = assertFailsWith<BusinessException> {
            diseaseSearchService.search(
                keyword = "hypertension",
                page = 0,
                size = 0,
            )
        }

        assertEquals(ErrorCode.INVALID_PAGE_REQUEST, exception.errorCode)
    }
}
