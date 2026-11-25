package com.safemedi.app.sefemedi.domain.drug.entity

import jakarta.persistence.*

@Entity
@Table(name = "dur_coverage")
class DurCoverage(
    @Id
    @Column(name = "coverage_key", length = 100)
    val coverageKey: String,
    @Column(name = "drug_code", length = 50, nullable = false)
    val drugCode: String,
    @Enumerated(EnumType.STRING)
    @Column(name = "dur_type", length = 20, nullable = false)
    val durType: DurType,
    @Column(name = "source_version", length = 100, nullable = false)
    val sourceVersion: String,
    @Column(nullable = false)
    val verified: Boolean = false,
)

enum class DurType { PREGNANCY, ELDERLY, AGE, INTERACTION }
