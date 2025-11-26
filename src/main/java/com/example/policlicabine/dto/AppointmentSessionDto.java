package com.example.policlicabine.dto;

import com.example.policlicabine.entity.enums.SessionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Medical appointment session with patient, doctor, consultations, and diagnoses")
public class AppointmentSessionDto {

    private UUID sessionId;

    // Nested DTOs (going DOWN the hierarchy)
    private PatientDto patient;
    private DoctorDto doctor;
    private List<ConsultationTypeDto> consultationTypes;
    private List<DiagnosisDto> diagnoses;

    // Session details
    private OffsetDateTime scheduledDateTime;
    private Boolean isEmergency;
    private SessionStatus status;
    private String freeTextDiagnosis;
    private String treatmentInstructions;
    private String freeTextObservations;
    private String cancellationReason;
    private Integer contactAttempts;
    private Integer rescheduleCount;
    private OffsetDateTime createdAt;
    private OffsetDateTime completedAt;
    private OffsetDateTime cancelledAt;
    private BigDecimal subtotalAmount;

    // Form completion status
    @Schema(description = "Total forms required for all consultation types in this appointment")
    private Integer requiredFormsCount;

    @Schema(description = "Forms completed AND valid at appointment date (not expired)")
    private Integer completedFormsCount;

    @Schema(description = "True if all required forms are completed and valid at appointment date")
    private Boolean allFormsComplete;
}
