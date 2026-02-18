package com.example.policlicabine.service;

import com.example.policlicabine.dto.BlockedSlotDto;
import com.example.policlicabine.dto.BookingConflictDto;
import com.example.policlicabine.dto.ConflictType;
import com.example.policlicabine.entity.AppointmentSession;
import com.example.policlicabine.entity.BlockedSlot;
import com.example.policlicabine.entity.Doctor;
import com.example.policlicabine.entity.User;
import com.example.policlicabine.entity.enums.BlockType;
import com.example.policlicabine.entity.enums.SessionStatus;
import com.example.policlicabine.exception.BookingConflictException;
import com.example.policlicabine.exception.BusinessException;
import com.example.policlicabine.exception.ResourceNotFoundException;
import com.example.policlicabine.mapper.BlockedSlotMapper;
import com.example.policlicabine.repository.AppointmentSessionRepository;
import com.example.policlicabine.repository.BlockedSlotRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class BlockedSlotService {

    private final BlockedSlotRepository blockedSlotRepository;
    private final BlockedSlotMapper blockedSlotMapper;
    private final AppointmentSessionRepository appointmentSessionRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private static final List<SessionStatus> EXCLUDED_STATUSES = List.of(
            SessionStatus.CANCELLED, SessionStatus.NO_SHOW
    );

    /**
     * Create a new blocked time slot for a doctor.
     */
    public BlockedSlotDto createBlockedSlot(UUID doctorId, OffsetDateTime startTime, OffsetDateTime endTime,
                                            BlockType blockType, String reason, UUID createdByUserId) {
        if (doctorId == null) {
            throw new BusinessException("Doctor ID is required");
        }
        if (createdByUserId == null) {
            throw new BusinessException("Created by user ID is required");
        }
        if (startTime == null) {
            throw new BusinessException("Start time is required");
        }
        if (endTime == null) {
            throw new BusinessException("End time is required");
        }
        if (!startTime.isBefore(endTime)) {
            throw new BusinessException("Start time must be before end time");
        }

        checkForOverlappingBlockedSlots(doctorId, startTime, endTime);
        checkForOverlappingAppointments(doctorId, startTime, endTime);

        Doctor doctorRef = entityManager.getReference(Doctor.class, doctorId);
        User createdByRef = entityManager.getReference(User.class, createdByUserId);

        BlockedSlot slot = BlockedSlot.builder()
                .doctor(doctorRef)
                .startTime(startTime)
                .endTime(endTime)
                .blockType(blockType != null ? blockType : BlockType.BLOCKED)
                .reason(reason)
                .createdBy(createdByRef)
                .build();

        BlockedSlot saved = blockedSlotRepository.save(slot);
        log.info("Blocked slot created: {} for doctor {} ({} - {})", saved.getId(), doctorId, startTime, endTime);

        return blockedSlotMapper.toDto(saved);
    }

    /**
     * Find all blocked slots for a doctor on a specific date.
     */
    @Transactional(readOnly = true)
    public List<BlockedSlotDto> findByDoctorAndDate(UUID doctorId, LocalDate date) {
        if (doctorId == null) {
            throw new BusinessException("Doctor ID is required");
        }
        if (date == null) {
            throw new BusinessException("Date is required");
        }

        OffsetDateTime dayStart = date.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime dayEnd = dayStart.plusDays(1);

        List<BlockedSlot> slots = blockedSlotRepository
                .findByDoctorDoctorIdAndStartTimeBetweenOrderByStartTimeAsc(doctorId, dayStart, dayEnd);

        return slots.stream()
                .map(blockedSlotMapper::toDto)
                .toList();
    }

    /**
     * Update an existing blocked slot.
     */
    public BlockedSlotDto updateBlockedSlot(UUID id, OffsetDateTime startTime, OffsetDateTime endTime,
                                            BlockType blockType, String reason) {
        if (id == null) {
            throw new BusinessException("Blocked slot ID is required");
        }

        BlockedSlot existing = blockedSlotRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("BlockedSlot", id));

        if (startTime != null && endTime != null && !startTime.isBefore(endTime)) {
            throw new BusinessException("Start time must be before end time");
        }

        OffsetDateTime effectiveStart = startTime != null ? startTime : existing.getStartTime();
        OffsetDateTime effectiveEnd = endTime != null ? endTime : existing.getEndTime();

        if (!effectiveStart.isBefore(effectiveEnd)) {
            throw new BusinessException("Start time must be before end time");
        }

        UUID doctorId = existing.getDoctor().getDoctorId();

        // Check overlapping blocked slots excluding self
        List<BlockedSlot> overlappingSlots = blockedSlotRepository
                .findOverlappingBlockedSlotsExcluding(doctorId, id, effectiveStart, effectiveEnd);
        if (!overlappingSlots.isEmpty()) {
            throw new BookingConflictException(
                    String.format("Blocked slot overlaps with %d existing blocked slot(s)", overlappingSlots.size()),
                    List.of()
            );
        }

        // Check overlapping appointments
        checkForOverlappingAppointments(doctorId, effectiveStart, effectiveEnd);

        existing.setStartTime(effectiveStart);
        existing.setEndTime(effectiveEnd);
        if (blockType != null) {
            existing.setBlockType(blockType);
        }
        if (reason != null) {
            existing.setReason(reason);
        }

        BlockedSlot saved = blockedSlotRepository.save(existing);
        log.info("Blocked slot updated: {} for doctor {}", id, doctorId);

        return blockedSlotMapper.toDto(saved);
    }

    /**
     * Delete a blocked slot.
     */
    public void deleteBlockedSlot(UUID id) {
        if (id == null) {
            throw new BusinessException("Blocked slot ID is required");
        }

        BlockedSlot existing = blockedSlotRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("BlockedSlot", id));

        blockedSlotRepository.delete(existing);
        log.info("Blocked slot deleted: {}", id);
    }

    /**
     * INTERNAL: Find overlapping blocked slots for a doctor within a time range.
     * Used by AppointmentSessionService for conflict detection.
     */
    @Transactional(readOnly = true)
    public List<BlockedSlot> findOverlappingBlockedSlots(UUID doctorId, OffsetDateTime startTime,
                                                         OffsetDateTime endTime) {
        return blockedSlotRepository.findOverlappingBlockedSlots(doctorId, startTime, endTime);
    }

    // --- Private helpers ---

    private void checkForOverlappingBlockedSlots(UUID doctorId, OffsetDateTime startTime, OffsetDateTime endTime) {
        List<BlockedSlot> overlapping = blockedSlotRepository
                .findOverlappingBlockedSlots(doctorId, startTime, endTime);
        if (!overlapping.isEmpty()) {
            throw new BookingConflictException(
                    String.format("Blocked slot overlaps with %d existing blocked slot(s)", overlapping.size()),
                    List.of()
            );
        }
    }

    private void checkForOverlappingAppointments(UUID doctorId, OffsetDateTime startTime, OffsetDateTime endTime) {
        List<AppointmentSession> overlapping = appointmentSessionRepository
                .findOverlappingAppointments(doctorId, startTime, endTime, EXCLUDED_STATUSES);
        if (!overlapping.isEmpty()) {
            List<BookingConflictDto> conflictDtos = overlapping.stream()
                    .map(session -> BookingConflictDto.builder()
                            .sessionId(session.getSessionId())
                            .patientName(session.getPatient().getFirstName() + " " + session.getPatient().getLastName())
                            .startTime(session.getScheduledDateTime())
                            .endTime(session.getEndTime())
                            .consultationNames(session.getConsultationNames())
                            .status(session.getStatus())
                            .conflictType(ConflictType.APPOINTMENT)
                            .build())
                    .toList();
            throw BookingConflictException.of(conflictDtos);
        }
    }
}
