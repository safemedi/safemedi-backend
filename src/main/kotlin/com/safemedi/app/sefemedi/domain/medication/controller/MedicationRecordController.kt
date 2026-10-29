package com.safemedi.app.sefemedi.domain.medication.controller

import com.safemedi.app.sefemedi.domain.medication.dto.MedicationRecordUpdateRequest
import com.safemedi.app.sefemedi.domain.medication.dto.MedicationRecordUpdateResponse
import com.safemedi.app.sefemedi.domain.medication.dto.MedicationRecordQueryResponse
import com.safemedi.app.sefemedi.domain.medication.dto.MedicationStatisticsResponse
import com.safemedi.app.sefemedi.domain.medication.dto.TodayMedicationScheduleResponse
import com.safemedi.app.sefemedi.domain.medication.service.MedicationRecordQueryService
import com.safemedi.app.sefemedi.domain.medication.service.MedicationRecordUpdateService
import com.safemedi.app.sefemedi.domain.medication.service.MedicationStatisticsService
import com.safemedi.app.sefemedi.domain.medication.service.TodayMedicationScheduleService
import com.safemedi.app.sefemedi.global.error.BusinessException
import com.safemedi.app.sefemedi.global.error.ErrorCode
import io.swagger.v3.oas.annotations.Parameter
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/medication-records")
class MedicationRecordController(
    private val todayMedicationScheduleService: TodayMedicationScheduleService,
    private val medicationRecordQueryService: MedicationRecordQueryService,
    private val medicationRecordUpdateService: MedicationRecordUpdateService,
    private val medicationStatisticsService: MedicationStatisticsService,
) {
    @GetMapping
    fun findRecords(
        @AuthenticationPrincipal socialId: String?,
        @RequestParam(required = false) type: String?,
        @RequestParam(required = false) date: String?,
        @RequestParam(required = false) familyId: Long?,
    ): MedicationRecordQueryResponse {
        return medicationRecordQueryService.findRecords(
            socialId = socialId ?: throw BusinessException(ErrorCode.INVALID_TOKEN),
            type = type,
            date = date,
            familyId = familyId,
        )
    }

    @GetMapping("/today")
    fun findTodaySchedules(
        @AuthenticationPrincipal socialId: String?,
        @Parameter(description = "조회할 가족 연결 ID. 생략 시 본인 조회, 지정 시 정보 공유가 허용된 가족만 조회")
        @RequestParam(required = false) familyId: Long?,
    ): TodayMedicationScheduleResponse {
        return todayMedicationScheduleService.findTodaySchedules(
            socialId = socialId ?: throw BusinessException(ErrorCode.INVALID_TOKEN),
            familyId = familyId,
        )
    }

    @GetMapping("/statistics")
    fun findStatistics(
        @AuthenticationPrincipal socialId: String?,
        @RequestParam(required = false) startDate: String?,
        @RequestParam(required = false) endDate: String?,
        @RequestParam(required = false) familyId: Long?,
    ): MedicationStatisticsResponse {
        return medicationStatisticsService.findStatistics(
            socialId = socialId ?: throw BusinessException(ErrorCode.INVALID_TOKEN),
            startDate = startDate,
            endDate = endDate,
            familyId = familyId,
        )
    }

    @PatchMapping
    fun update(
        @AuthenticationPrincipal socialId: String?,
        @RequestBody request: MedicationRecordUpdateRequest,
    ): MedicationRecordUpdateResponse {
        return medicationRecordUpdateService.update(
            socialId = socialId ?: throw BusinessException(ErrorCode.INVALID_TOKEN),
            request = request,
        )
    }
}
