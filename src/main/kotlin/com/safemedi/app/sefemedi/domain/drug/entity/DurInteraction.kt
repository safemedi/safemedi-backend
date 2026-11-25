package com.safemedi.app.sefemedi.domain.drug.entity

import jakarta.persistence.*
import java.time.LocalDate

@Entity
@Table(name = "dur_interaction")
class DurInteraction(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "drug_name_a", length = 1000)
    val drugNameA: String,

    @Column(name = "drug_name_b", length = 1000)
    val drugNameB: String,

    @Column(name = "notice_number", length = 50)
    val noticeNumber: String? = null,

    @Column(name = "notice_date")
    val noticeDate: LocalDate? = null,

    @Column(name = "warning_message", columnDefinition = "TEXT")
    val warningMessage: String? = null,
    @Column(name = "drug_code_a", length = 50)
    val drugCodeA: String? = null,
    @Column(name = "drug_code_b", length = 50)
    val drugCodeB: String? = null,
    @Column(name = "dur_product_code_a", length = 50)
    val durProductCodeA: String? = null,
    @Column(name = "dur_product_code_b", length = 50)
    val durProductCodeB: String? = null,
    @Column(name = "ingredient_code_a", length = 50)
    val ingredientCodeA: String? = null,
    @Column(name = "ingredient_code_b", length = 50)
    val ingredientCodeB: String? = null,
    @Column(name = "source_record_key", length = 64)
    val sourceRecordKey: String? = null,
    @Column(name = "runtime_verified", nullable = false)
    val runtimeVerified: Boolean = false,
)
