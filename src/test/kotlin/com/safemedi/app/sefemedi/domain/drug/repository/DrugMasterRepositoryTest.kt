package com.safemedi.app.sefemedi.domain.drug.repository

import com.safemedi.app.sefemedi.domain.drug.entity.DrugMaster
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageRequest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import kotlin.test.assertEquals

@SpringBootTest(properties = [
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.datasource.url=jdbc:h2:mem:drug-search-test;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false;NON_KEYWORDS=USER",
])
@ActiveProfiles("test")
@Transactional
class DrugMasterRepositoryTest @Autowired constructor(
    private val drugMasterRepository: DrugMasterRepository,
) {
    @Test
    fun `일반 제품이 있으면 수출용을 숨기고 수출용만 있으면 표시한다`() {
        drugMasterRepository.saveAllAndFlush(
            listOf(
                DrugMaster("D001", "테스트정100mg", "N02BE01"),
                DrugMaster("D002", "테스트정100mg", "N02BE01"),
                DrugMaster("D003", "테스트정100mg(수출용)Export name", "N02BE01"),
                DrugMaster("D004", "테스트정200mg", "N02BE01"),
                DrugMaster("D005", "테스트정300mg", null),
                DrugMaster("D006", "테스트정400mg", ""),
                DrugMaster("D007", "테스트정500mg", "   "),
                DrugMaster("D008", "테스트정600mg(수출용)Export name", "N02BE01"),
            ),
        )

        val result = drugMasterRepository.searchDistinctByDrugNameAndAtcCode(
            "테스트정",
            PageRequest.of(0, 1),
        )
        val secondPage = drugMasterRepository.searchDistinctByDrugNameAndAtcCode(
            "테스트정",
            PageRequest.of(1, 1),
        )
        val thirdPage = drugMasterRepository.searchDistinctByDrugNameAndAtcCode(
            "테스트정",
            PageRequest.of(2, 1),
        )

        assertEquals(listOf("D001"), result.content.map { it.drugCode })
        assertEquals(false, result.isLast)
        assertEquals(listOf("D004"), secondPage.content.map { it.drugCode })
        assertEquals(false, secondPage.isLast)
        assertEquals(listOf("D008"), thirdPage.content.map { it.drugCode })
        assertEquals(true, thirdPage.isLast)
    }

    @Test
    fun `이스케이프한 검색어는 와일드카드 문자를 그대로 검색한다`() {
        drugMasterRepository.saveAllAndFlush(
            listOf(
                DrugMaster("P001", "테스트정5%", "N02BE01"),
                DrugMaster("P002", "테스트정50mg", "N02BE01"),
                DrugMaster("P003", "테스트_정", "N02BE01"),
                DrugMaster("P004", "테스트A정", "N02BE01"),
                DrugMaster("P005", "테스트!정", "N02BE01"),
            ),
        )

        fun search(keyword: String) = drugMasterRepository.searchDistinctByDrugNameAndAtcCode(
            keyword,
            PageRequest.of(0, 20),
        ).content.map { it.drugCode }

        assertEquals(listOf("P001"), search("5!%"))
        assertEquals(listOf("P003"), search("테스트!_정"))
        assertEquals(listOf("P005"), search("테스트!!정"))
    }
}
