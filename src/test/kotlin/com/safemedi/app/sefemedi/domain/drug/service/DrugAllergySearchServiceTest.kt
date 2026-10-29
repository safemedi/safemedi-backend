package com.safemedi.app.sefemedi.domain.drug.service

import com.safemedi.app.sefemedi.domain.drug.entity.AtcGroupMaster
import com.safemedi.app.sefemedi.domain.drug.repository.AtcGroupMasterRepository
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

class DrugAllergySearchServiceTest {
    private lateinit var atcGroupMasterRepository: AtcGroupMasterRepository
    private lateinit var drugAllergySearchService: DrugAllergySearchService

    @BeforeEach
    fun setUp() {
        atcGroupMasterRepository = mock(AtcGroupMasterRepository::class.java)
        drugAllergySearchService = DrugAllergySearchService(atcGroupMasterRepository)
    }

    @Test
    fun `search returns paged drug allergy search result`() {
        val atcGroup = AtcGroupMaster(
            atcCode = "J01C",
            atcNameKo = "Penicillin beta-lactam antibacterials",
            atcNameEn = "BETA-LACTAM ANTIBACTERIALS, PENICILLINS",
            atcLevel = 3,
        )

        given(
            atcGroupMasterRepository.searchByKeyword(
                keyword = "penicillin",
                pageable = PageRequest.of(0, 20),
            )
        ).willReturn(SliceImpl(listOf(atcGroup), PageRequest.of(0, 20), false))

        val response = drugAllergySearchService.search(
            keyword = "penicillin",
            page = 0,
            size = 20,
        )

        assertEquals(0, response.page)
        assertEquals(20, response.size)
        assertEquals(true, response.isLast)
        assertEquals(1, response.content.size)

        val item = response.content.single()
        assertEquals("ATC_GROUP", item.allergyType)
        assertEquals("J01C", item.allergyValue)
        assertEquals("Penicillin beta-lactam antibacterials", item.allergyName)
    }

    @Test
    fun `search uses english name when korean name is null`() {
        val atcGroup = AtcGroupMaster(
            atcCode = "J01C",
            atcNameKo = null,
            atcNameEn = "BETA-LACTAM ANTIBACTERIALS, PENICILLINS",
            atcLevel = 3,
        )

        given(
            atcGroupMasterRepository.searchByKeyword(
                keyword = "penicillin",
                pageable = PageRequest.of(0, 20),
            )
        ).willReturn(SliceImpl(listOf(atcGroup), PageRequest.of(0, 20), false))

        val response = drugAllergySearchService.search(
            keyword = "penicillin",
            page = 0,
            size = 20,
        )

        assertEquals("BETA-LACTAM ANTIBACTERIALS, PENICILLINS", response.content.single().allergyName)
    }

    @Test
    fun `search coerces size when size exceeds max`() {
        given(
            atcGroupMasterRepository.searchByKeyword(
                keyword = "penicillin",
                pageable = PageRequest.of(0, 50),
            )
        ).willReturn(SliceImpl(emptyList(), PageRequest.of(0, 50), false))

        val response = drugAllergySearchService.search(
            keyword = "penicillin",
            page = 0,
            size = 100,
        )

        assertEquals(50, response.size)
    }

    @Test
    fun `search throws when page is invalid`() {
        val exception = assertFailsWith<BusinessException> {
            drugAllergySearchService.search(
                keyword = "penicillin",
                page = -1,
                size = 20,
            )
        }

        assertEquals(ErrorCode.INVALID_PAGE_REQUEST, exception.errorCode)
    }

    @Test
    fun `search throws when size is less than minimum`() {
        val exception = assertFailsWith<BusinessException> {
            drugAllergySearchService.search(
                keyword = "penicillin",
                page = 0,
                size = 0,
            )
        }

        assertEquals(ErrorCode.INVALID_PAGE_REQUEST, exception.errorCode)
    }
}
