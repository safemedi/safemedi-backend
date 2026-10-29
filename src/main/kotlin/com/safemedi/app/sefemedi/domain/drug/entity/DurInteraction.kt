package com.safemedi.app.sefemedi.domain.drug.entity

import jakarta.persistence.*
import java.time.LocalDate

@Entity
@Table(name = "dur_interaction")
class DurInteraction(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "drug_name_a", length = 255)
    val drugNameA: String,

    @Column(name = "drug_name_b", length = 255)
    val drugNameB: String,

    @Column(name = "notice_number", length = 50)
    val noticeNumber: String,

    @Column(name = "notice_date")
    val noticeDate: LocalDate,

    @Column(name = "warning_message", columnDefinition = "TEXT")
    val warningMessage: String
)
