package com.safemedi.app.sefemedi.domain.drug.entity

import jakarta.persistence.*
import java.time.LocalDate

@Entity
@Table(name = "dur_pregnancy")
class DurPregnancy(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "drug_code", length = 50, nullable = false)
    val drugCode: String,

    @Column(name = "dur_product_code", length = 50, nullable = false)
    val durProductCode: String,

    @Column(name = "drug_name", length = 1000, nullable = false)
    val drugName: String,

    @Column(name = "ingredient_code", length = 50, nullable = false)
    val ingredientCode: String,

    @Column(name = "grade", length = 10, nullable = false)
    val grade: String,

    @Column(name = "notice_number", length = 50)
    val noticeNumber: String? = null,

    @Column(name = "notice_date")
    val noticeDate: LocalDate? = null,

    @Column(name = "warning_message", columnDefinition = "TEXT")
    val warningMessage: String? = null,
    @Column(name = "source_record_key", length = 64)
    val sourceRecordKey: String? = null,
    @Column(name = "runtime_verified", nullable = false)
    val runtimeVerified: Boolean = false,
)
