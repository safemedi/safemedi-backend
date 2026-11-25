package com.safemedi.app.sefemedi.domain.drug.repository

import com.safemedi.app.sefemedi.domain.drug.entity.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@SpringBootTest(properties = [
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.datasource.url=jdbc:h2:mem:drug-search-test;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false;NON_KEYWORDS=USER",
])
@ActiveProfiles("test")
@Transactional
class DurRepositoriesTest @Autowired constructor(
    private val drugs: DrugMasterRepository,
    private val ages: DurAgeRepository,
    private val pregnancies: DurPregnancyRepository,
    private val elderly: DurElderlyRepository,
    private val interactions: DurInteractionRepository,
    private val coverage: DurCoverageRepository,
) {
    @Test
    fun `DUR 4종은 코드가 일치하는 활성 규칙만 조회한다`() {
        drugs.saveAllAndFlush(listOf(DrugMaster("DX1","같은이름"),DrugMaster("DX2","같은이름")))
        ages.saveAllAndFlush(listOf(
            DurAge(drugName="원본",drugCode="DX1",targetAge=6,ageUnit=DurAgeUnit.MONTH,ageCondition="UNDER",runtimeVerified=true),
            DurAge(drugName="원본",drugCode="DX1",targetAge=6,ageCondition="UNDER",runtimeVerified=false),
            DurAge(drugName="같은이름",drugCode="DX2",targetAge=6,ageCondition="UNDER",runtimeVerified=true),
        ))
        pregnancies.saveAllAndFlush(listOf(
            DurPregnancy(drugCode="DX1",durProductCode="P1",drugName="원본",ingredientCode="I1",grade="M",runtimeVerified=true),
            DurPregnancy(drugCode="DX1",durProductCode="P2",drugName="원본",ingredientCode="I1",grade="1",runtimeVerified=false),
        ))
        elderly.saveAllAndFlush(listOf(
            DurElderly(drugCode="DX1",durProductCode="P1",drugName="원본",ingredientCode="I1",runtimeVerified=true),
            DurElderly(drugCode="DX1",durProductCode="P2",drugName="원본",ingredientCode="I1",runtimeVerified=false),
        ))
        interactions.saveAllAndFlush(listOf(
            DurInteraction(drugCodeA="DX2",drugCodeB="DX1",drugNameA="원本B",drugNameB="원본A",runtimeVerified=true),
            DurInteraction(drugCodeA="DX1",drugCodeB="DX2",drugNameA="원본A",drugNameB="원본B",runtimeVerified=false),
        ))
        assertEquals(DurAgeUnit.MONTH,ages.findByDrugCodeInAndRuntimeVerifiedTrue(listOf("DX1")).single().ageUnit)
        assertEquals("M",pregnancies.findByDrugCodeInAndRuntimeVerifiedTrue(listOf("DX1")).single().grade)
        assertEquals(1,elderly.findByDrugCodeInAndRuntimeVerifiedTrue(listOf("DX1")).size)
        assertEquals(1,interactions.findInteractions(listOf("DX1","DX2")).size)
        assertTrue(interactions.findInteractions(listOf("DX1")).isEmpty())
    }

    @Test
    fun `범위 검증도 승인된 약품만 조회한다`() {
        coverage.saveAllAndFlush(listOf(
            DurCoverage("AGE:DX1","DX1",DurType.AGE,"test",true),
            DurCoverage("AGE:DX2","DX2",DurType.AGE,"test",false),
        ))
        assertEquals(listOf("DX1"),coverage.findByDrugCodeInAndVerifiedTrue(listOf("DX1","DX2")).map { it.drugCode })
    }
}
