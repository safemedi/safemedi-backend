package com.safemedi.app.sefemedi.domain.medication.repository

import com.safemedi.app.sefemedi.domain.medication.entity.MedicationRecord
import com.safemedi.app.sefemedi.domain.medication.entity.MedicationStatus
import com.safemedi.app.sefemedi.domain.medication.entity.Prescription
import com.safemedi.app.sefemedi.domain.medication.entity.PrescriptionDrug
import com.safemedi.app.sefemedi.domain.medication.entity.PrescriptionDrugTime
import jakarta.persistence.LockModeType
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDate
import java.time.LocalDateTime

interface PrescriptionRepository : JpaRepository<Prescription, Long> {
    @Query(
        """
        select p
        from Prescription p
        where p.user.id = :userId
          and p.deletedAt is null
          and p.startDate <= :today
          and p.endDate >= :today
        order by p.endDate asc, p.id asc
        """
    )
    fun findActiveByUserId(
        @Param("userId") userId: Long,
        @Param("today") today: LocalDate,
    ): List<Prescription>

    fun findByUserIdAndDeletedAtIsNullOrderByCreatedAtDescIdDesc(
        userId: Long,
        pageable: Pageable,
    ): Slice<Prescription>

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        delete from Prescription p
        where p.user.id = :userId
        """
    )
    fun deleteAllByUserId(
        @Param("userId") userId: Long,
    ): Int

    fun findByIdAndDeletedAtIsNull(
        id: Long,
    ): Prescription?

    fun findByIdAndUserIdAndDeletedAtIsNull(
        id: Long,
        userId: Long,
    ): Prescription?
}

interface PrescriptionDrugRepository : JpaRepository<PrescriptionDrug, Long> {
    @Query(
        """
        select pd
        from PrescriptionDrug pd
        join fetch pd.prescription p
        where p.id in :prescriptionIds
        order by p.endDate asc, p.id asc, pd.id asc
        """
    )
    fun findAllByPrescriptionIds(
        @Param("prescriptionIds") prescriptionIds: Collection<Long>,
    ): List<PrescriptionDrug>

    @Query(
        """
        select pd.prescription.id as prescriptionId, count(pd.id) as drugCount
        from PrescriptionDrug pd
        where pd.prescription.id in :prescriptionIds
        group by pd.prescription.id
        """
    )
    fun countByPrescriptionIds(
        @Param("prescriptionIds") prescriptionIds: Collection<Long>,
    ): List<PrescriptionDrugCountProjection>

    @Query(
        """
        select pd
        from PrescriptionDrug pd
        left join fetch pd.drug
        where pd.prescription.id = :prescriptionId
        order by pd.id asc
        """
    )
    fun findDetailsByPrescriptionId(
        @Param("prescriptionId") prescriptionId: Long,
    ): List<PrescriptionDrug>

    @Query(
        """
        select pd
        from PrescriptionDrug pd
        where pd.prescription.id = :prescriptionId
          and pd.id in :prescriptionDrugIds
        """
    )
    fun findByPrescriptionIdAndIds(
        @Param("prescriptionId") prescriptionId: Long,
        @Param("prescriptionDrugIds") prescriptionDrugIds: Collection<Long>,
    ): List<PrescriptionDrug>

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        delete from PrescriptionDrug pd
        where pd.prescription.user.id = :userId
        """
    )
    fun deleteAllByUserId(
        @Param("userId") userId: Long,
    ): Int
}

interface PrescriptionDrugCountProjection {
    val prescriptionId: Long
    val drugCount: Long
}

interface PrescriptionDrugTimeRepository : JpaRepository<PrescriptionDrugTime, Long> {
    @Query(
        """
        select pdt
        from PrescriptionDrugTime pdt
        where pdt.prescriptionDrug.id in :prescriptionDrugIds
          and pdt.deletedAt is null
        order by pdt.takeTime asc, pdt.id asc
        """
    )
    fun findByPrescriptionDrugIds(
        @Param("prescriptionDrugIds") prescriptionDrugIds: Collection<Long>,
    ): List<PrescriptionDrugTime>

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        delete from PrescriptionDrugTime pdt
        where pdt.prescriptionDrug.prescription.user.id = :userId
        """
    )
    fun deleteAllByUserId(
        @Param("userId") userId: Long,
    ): Int
}

interface MedicationRecordRepository : JpaRepository<MedicationRecord, Long> {
    @Query(
        """
        select mr
        from MedicationRecord mr
        join fetch mr.user u
        join fetch mr.prescription p
        where mr.id = :recordId
          and p.deletedAt is null
        """
    )
    fun findActiveById(
        @Param("recordId") recordId: Long,
    ): MedicationRecord?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
        """
        select mr
        from MedicationRecord mr
        join fetch mr.user u
        join fetch mr.prescription p
        join fetch mr.prescriptionDrugTime pdt
        join fetch pdt.prescriptionDrug pd
        where mr.id in :recordIds
          and p.deletedAt is null
        """
    )
    fun findActiveAllByIdIn(
        @Param("recordIds") recordIds: Collection<Long>,
    ): List<MedicationRecord>

    @Query(
        """
        select mr
        from MedicationRecord mr
        where mr.prescription.id = :prescriptionId
          and mr.scheduledAt = :scheduledAt
          and mr.user.id = :userId
        """
    )
    fun findAllByPrescriptionIdAndScheduledAtAndUserId(
        @Param("prescriptionId") prescriptionId: Long,
        @Param("scheduledAt") scheduledAt: LocalDateTime,
        @Param("userId") userId: Long,
    ): List<MedicationRecord>

    @Query(
        """
        select mr
        from MedicationRecord mr
        join fetch mr.prescription p
        join fetch mr.prescriptionDrugTime pdt
        join fetch pdt.prescriptionDrug pd
        where mr.user.id = :userId
          and mr.scheduledAt >= :startAt
          and mr.scheduledAt < :endAt
          and p.deletedAt is null
        order by mr.scheduledAt asc, p.id asc, pd.id asc
        """
    )
    fun findTodaySchedules(
        @Param("userId") userId: Long,
        @Param("startAt") startAt: LocalDateTime,
        @Param("endAt") endAt: LocalDateTime,
    ): List<MedicationRecord>

    @Query(
        """
        select mr
        from MedicationRecord mr
        join fetch mr.user u
        join fetch mr.prescription p
        join fetch mr.prescriptionDrugTime pdt
        join fetch pdt.prescriptionDrug pd
        where mr.status = :status
          and mr.scheduledAt > :startAt
          and mr.scheduledAt <= :endAt
          and p.deletedAt is null
          and pdt.deletedAt is null
        order by mr.scheduledAt asc, mr.id asc
        """
    )
    fun findPendingRecordsScheduledBetween(
        @Param("status") status: MedicationStatus,
        @Param("startAt") startAt: LocalDateTime,
        @Param("endAt") endAt: LocalDateTime,
    ): List<MedicationRecord>

    @Query(
        """
        select mr
        from MedicationRecord mr
        where mr.user.id = :userId
          and mr.scheduledAt >= :startAt
          and mr.scheduledAt < :endAt
        order by mr.scheduledAt asc, mr.id asc
        """
    )
    fun findStatisticsRecords(
        @Param("userId") userId: Long,
        @Param("startAt") startAt: LocalDateTime,
        @Param("endAt") endAt: LocalDateTime,
    ): List<MedicationRecord>

    @Query(
        """
        select mr
        from MedicationRecord mr
        join fetch mr.prescription p
        join fetch mr.prescriptionDrugTime pdt
        join fetch pdt.prescriptionDrug pd
        where mr.user.id = :userId
          and mr.scheduledAt >= :startAt
          and mr.scheduledAt < :endAt
          and p.deletedAt is null
          and pdt.deletedAt is null
        order by mr.scheduledAt asc, p.id asc, pd.id asc, mr.id asc
        """
    )
    fun findRecordsForPeriod(
        @Param("userId") userId: Long,
        @Param("startAt") startAt: LocalDateTime,
        @Param("endAt") endAt: LocalDateTime,
    ): List<MedicationRecord>

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        delete from MedicationRecord mr
        where mr.prescription.id = :prescriptionId
          and mr.status = :status
          and mr.scheduledAt > :now
        """
    )
    fun deleteFuturePendingByPrescriptionId(
        @Param("prescriptionId") prescriptionId: Long,
        @Param("status") status: MedicationStatus,
        @Param("now") now: LocalDateTime,
    ): Int

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        delete from MedicationRecord mr
        where mr.prescription.id = :prescriptionId
          and mr.prescriptionDrugTime.prescriptionDrug.id in :prescriptionDrugIds
          and mr.status = :status
          and mr.scheduledAt > :now
        """
    )
    fun deleteFuturePendingByPrescriptionIdAndPrescriptionDrugIds(
        @Param("prescriptionId") prescriptionId: Long,
        @Param("prescriptionDrugIds") prescriptionDrugIds: Collection<Long>,
        @Param("status") status: MedicationStatus,
        @Param("now") now: LocalDateTime,
    ): Int

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        delete from MedicationRecord mr
        where mr.user.id = :userId
        """
    )
    fun deleteAllByUserId(
        @Param("userId") userId: Long,
    ): Int
}
