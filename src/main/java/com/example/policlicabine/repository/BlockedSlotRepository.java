package com.example.policlicabine.repository;

import com.example.policlicabine.entity.BlockedSlot;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface BlockedSlotRepository extends JpaRepository<BlockedSlot, UUID> {

    /**
     * Find blocked slots that overlap with the given time range for a doctor.
     * Overlap condition: existing.startTime < newEnd AND existing.endTime > newStart
     */
    @Query("SELECT b FROM BlockedSlot b WHERE b.doctor.doctorId = :doctorId " +
           "AND b.startTime < :endTime AND b.endTime > :startTime")
    List<BlockedSlot> findOverlappingBlockedSlots(
        @Param("doctorId") UUID doctorId,
        @Param("startTime") OffsetDateTime startTime,
        @Param("endTime") OffsetDateTime endTime
    );

    /**
     * Find overlapping blocked slots excluding a specific slot (used when updating).
     */
    @Query("SELECT b FROM BlockedSlot b WHERE b.doctor.doctorId = :doctorId " +
           "AND b.id != :excludeId " +
           "AND b.startTime < :endTime AND b.endTime > :startTime")
    List<BlockedSlot> findOverlappingBlockedSlotsExcluding(
        @Param("doctorId") UUID doctorId,
        @Param("excludeId") UUID excludeId,
        @Param("startTime") OffsetDateTime startTime,
        @Param("endTime") OffsetDateTime endTime
    );

    /**
     * Find all blocked slots for a doctor within a date range (inclusive start, exclusive end).
     * Use dayStart = date.atStartOfDay().atOffset(UTC), dayEnd = dayStart + 1 day.
     */
    @EntityGraph(attributePaths = {"doctor", "createdBy"})
    List<BlockedSlot> findByDoctorDoctorIdAndStartTimeBetweenOrderByStartTimeAsc(
        UUID doctorId, OffsetDateTime dayStart, OffsetDateTime dayEnd);
}
