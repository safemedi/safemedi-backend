package com.safemedi.app.sefemedi.domain.drug.entity

import jakarta.persistence.*
import java.time.LocalDate

@Entity
@Table(name = "dur_age")
class DurAge(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "drug_name", length = 255)
    val drugName: String,

    @Column(name = "target_age")
    val targetAge: Int,

    @Column(name = "age_condition", length = 20)
    val ageCondition: String,

    @Column(name = "notice_number", length = 50)
    val noticeNumber: String,

    @Column(name = "notice_date")
    val noticeDate: LocalDate,

    @Column(name = "warning_message", columnDefinition = "TEXT")
    val warningMessage: String
)
