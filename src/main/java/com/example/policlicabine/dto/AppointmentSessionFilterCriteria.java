package com.example.policlicabine.dto;

import com.example.policlicabine.entity.enums.SessionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentSessionFilterCriteria {

    private UUID sessionId;

    private UUID patientId;

    private String patientName;

    private UUID doctorId;

    private String doctorName;

    private OffsetDateTime scheduledAfter;

    private OffsetDateTime scheduledBefore;

    private OffsetDateTime completedAfter;

    private OffsetDateTime completedBefore;

    private SessionStatus status;

    private List<String> consultationNames;

    // CNP and birth date filtering
    @Schema(description = "Filter by patient CNP (partial match)", example = "192051512")
    private String patientCnp;

    @Schema(description = "Filter by birth date from (inclusive)", example = "1990-01-01", format = "date")
    private LocalDate birthDateFrom;

    @Schema(description = "Filter by birth date to (inclusive)", example = "2000-12-31", format = "date")
    private LocalDate birthDateTo;
}
