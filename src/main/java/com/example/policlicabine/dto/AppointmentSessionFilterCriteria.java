package com.example.policlicabine.dto;

import com.example.policlicabine.entity.enums.SessionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Filter criteria DTO for searching and filtering appointment sessions.
 * <p>
 * This DTO is used as a request parameter in search endpoints to specify
 * filtering conditions. All fields are optional (nullable), and null values
 * are ignored during specification building.
 * </p>
 * <p>
 * Supports:
 * <ul>
 *   <li>Patient filters: Exact ID match or partial name match (case-insensitive)</li>
 *   <li>Doctor filters: Exact ID match or partial name match (case-insensitive)</li>
 *   <li>Date range filters: scheduledAfter, scheduledBefore, completedAfter, completedBefore</li>
 *   <li>Status filter: Exact match (SCHEDULED, IN_PROGRESS, COMPLETED, CANCELLED, NO_SHOW)</li>
 *   <li>ConsultationType types filter: Sessions containing ANY of the specified consultation types</li>
 * </ul>
 * </p>
 *
 * @since 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentSessionFilterCriteria {

    /**
     * Filter by exact patient ID.
     * <p>
     * Example: "123e4567-e89b-12d3-a456-426614174000"
     * </p>
     */
    private UUID patientId;

    /**
     * Filter by patient name (partial match on first name OR last name, case-insensitive).
     * <p>
     * Example: "john" matches patients with firstName "John" or lastName "Johnson"
     * </p>
     */
    private String patientName;

    /**
     * Filter by exact doctor ID.
     * <p>
     * Example: "123e4567-e89b-12d3-a456-426614174001"
     * </p>
     */
    private UUID doctorId;

    /**
     * Filter by doctor name (partial match on doctor's user full name, case-insensitive).
     * <p>
     * Example: "smith" matches doctors whose associated user has fullName containing "Smith"
     * </p>
     */
    private String doctorName;

    /**
     * Filter sessions scheduled on or after this date/time.
     * <p>
     * Inclusive: sessions with scheduledDateTime >= scheduledAfter
     * </p>
     */
    private OffsetDateTime scheduledAfter;

    /**
     * Filter sessions scheduled on or before this date/time.
     * <p>
     * Inclusive: sessions with scheduledDateTime <= scheduledBefore
     * </p>
     */
    private OffsetDateTime scheduledBefore;

    /**
     * Filter sessions completed on or after this date/time.
     * <p>
     * Inclusive: sessions with actualCompletionTime >= completedAfter
     * </p>
     */
    private OffsetDateTime completedAfter;

    /**
     * Filter sessions completed on or before this date/time.
     * <p>
     * Inclusive: sessions with actualCompletionTime <= completedBefore
     * </p>
     */
    private OffsetDateTime completedBefore;

    /**
     * Filter by session status (exact match).
     * <p>
     * Example: SCHEDULED, IN_PROGRESS, COMPLETED, CANCELLED, NO_SHOW
     * </p>
     */
    private SessionStatus status;

    /**
     * Filter sessions containing ANY of the specified consultation types.
     * <p>
     * Sessions that have at least one consultation matching any name in this list will be included.
     * Example: ["General Checkup", "Dental ConsultationType"]
     * </p>
     */
    private List<String> consultationNames;
}
