package com.safemedi.app.sefemedi.domain.drug.entity

import jakarta.persistence.*
import java.time.LocalDate

@Entity
@Table(name = "dur_age")
class DurAge(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "drug_name", length = 1000)
    val drugName: String,

    @Column(name = "target_age")
    val targetAge: Int,

    @Column(name = "age_condition", length = 20)
    val ageCondition: String,

    @Column(name = "notice_number", length = 50)
    val noticeNumber: String? = null,

    @Column(name = "notice_date")
    val noticeDate: LocalDate? = null,

    @Column(name = "warning_message", columnDefinition = "TEXT")
    val warningMessage: String? = null,

    @Column(name = "drug_code", length = 50)
    val drugCode: String? = null,
    @Column(name = "dur_product_code", length = 50)
    val durProductCode: String? = null,
    @Column(name = "ingredient_code", length = 50)
    val ingredientCode: String? = null,
    @Enumerated(EnumType.STRING)
    @Column(name = "age_unit", length = 10, nullable = false)
    val ageUnit: DurAgeUnit = DurAgeUnit.YEAR,
    @Column(name = "source_record_key", length = 64)
    val sourceRecordKey: String? = null,
    @Column(name = "runtime_verified", nullable = false)
    val runtimeVerified: Boolean = false,
) {
    fun matches(birthDate: LocalDate, today: LocalDate): Boolean {
        if (birthDate > today || targetAge < 0) return false
        val boundary = when (ageUnit) {
            DurAgeUnit.YEAR -> birthDate.plusYears(targetAge.toLong())
            DurAgeUnit.MONTH -> birthDate.plusMonths(targetAge.toLong())
            DurAgeUnit.WEEK -> birthDate.plusWeeks(targetAge.toLong())
        }
        return when (ageCondition.trim().uppercase()) {
            "UNDER", "LESS_THAN", "LT" -> today < boundary
            "UNDER_OR_EQUAL", "LESS_THAN_OR_EQUAL", "LTE" -> {
                // 만 나이 N 이하: 다음 나이 단위에 도달하기 전까지다.
                val end = when (ageUnit) {
                    DurAgeUnit.YEAR -> birthDate.plusYears(targetAge.toLong() + 1)
                    DurAgeUnit.MONTH -> birthDate.plusMonths(targetAge.toLong() + 1)
                    DurAgeUnit.WEEK -> birthDate.plusWeeks(targetAge.toLong() + 1)
                }
                today < end
            }
            "OVER_OR_EQUAL", "GREATER_THAN_OR_EQUAL", "GTE" -> today >= boundary
            "OVER", "GREATER_THAN", "GT" -> {
                val next = when (ageUnit) {
                    DurAgeUnit.YEAR -> birthDate.plusYears(targetAge.toLong() + 1)
                    DurAgeUnit.MONTH -> birthDate.plusMonths(targetAge.toLong() + 1)
                    DurAgeUnit.WEEK -> birthDate.plusWeeks(targetAge.toLong() + 1)
                }
                today >= next
            }
            else -> false
        }
    }
}

enum class DurAgeUnit { YEAR, MONTH, WEEK }
