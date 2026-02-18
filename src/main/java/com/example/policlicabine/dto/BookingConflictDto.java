package com.example.policlicabine.dto;

import com.example.policlicabine.entity.enums.SessionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO representing a conflicting appointment session.
 * Used in BookingConflictException to provide detailed information about scheduling conflicts.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Details of an appointment that conflicts with the requested time slot")
public class BookingConflictDto {

    @Schema(description = "ID of the conflicting appointment session", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID sessionId;

    @Schema(description = "Name of the patient for the conflicting appointment", example = "John Doe")
    private String patientName;

    @Schema(description = "Start time of the conflicting appointment")
    private OffsetDateTime startTime;

    @Schema(description = "End time of the conflicting appointment")
    private OffsetDateTime endTime;

    @Schema(description = "Names of consultations in the conflicting appointment", example = "[\"General Checkup\", \"Blood Test\"]")
    private List<String> consultationNames;

    @Schema(description = "Status of the conflicting appointment", example = "SCHEDULED")
    private SessionStatus status;
}
