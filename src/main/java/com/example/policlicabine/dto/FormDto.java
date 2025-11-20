package com.example.policlicabine.dto;

import com.example.policlicabine.entity.enums.FormStatus;
import com.example.policlicabine.entity.enums.FormType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Data Transfer Object for Form entity.
 *
 * <p>Represents a medical form in the clinic workflow (consent form, anesthesia form, etc.).
 * Forms are business entities with lifecycle states, while Files are physical PDF/image representations.
 *
 * @author PoliclicaBine System
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(
        name = "Form",
        description = "Medical form with signature workflow and file attachments"
)
public class FormDto {

    // ==================== IDENTITY ====================

    @Schema(
            description = "Unique form identifier (UUID)",
            example = "018e1234-5678-7abc-def0-123456789abc",
            accessMode = Schema.AccessMode.READ_ONLY
    )
    private UUID formId;

    @Schema(
            description = "Type of form",
            example = "CONSENT",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private FormType formType;

    @Schema(
            description = "Current status in form lifecycle",
            example = "SIGNED",
            accessMode = Schema.AccessMode.READ_ONLY
    )
    private FormStatus status;

    // ==================== OWNERSHIP ====================

    @Schema(
            description = "Patient ID who owns this form",
            example = "123e4567-e89b-12d3-a456-426614174000",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private UUID patientId;

    @Schema(
            description = "Patient's full name (convenience field)",
            example = "Maria Popescu",
            accessMode = Schema.AccessMode.READ_ONLY
    )
    private String patientFullName;

    @Schema(
            description = "Appointment session ID (optional - for session-specific forms)",
            example = "018e5678-1234-7abc-def0-abcdef123456"
    )
    private UUID appointmentSessionId;

    // ==================== FILES ====================

    @Schema(
            description = "Primary signed PDF document",
            accessMode = Schema.AccessMode.READ_ONLY
    )
    private FileDto primaryFile;

    @Schema(
            description = "Supporting documents (ID scans, photos, etc.)",
            accessMode = Schema.AccessMode.READ_ONLY
    )
    private List<FileDto> attachments;

    @Schema(
            description = "Total number of files (primary + attachments)",
            example = "3",
            accessMode = Schema.AccessMode.READ_ONLY
    )
    private Integer totalFileCount;

    // ==================== PATIENT SIGNING ====================

    @Schema(
            description = "Timestamp when patient signed this form",
            example = "2025-01-15T14:30:00Z",
            accessMode = Schema.AccessMode.READ_ONLY
    )
    private LocalDateTime patientSignedAt;

    @Schema(
            description = "Username of user who witnessed patient signature (receptionist/doctor)",
            example = "dr.smith",
            accessMode = Schema.AccessMode.READ_ONLY
    )
    private String patientSignedByUsername;

    // ==================== DOCTOR SIGNING (FUTURE) ====================

    @Schema(
            description = "Timestamp when doctor signed this form (future use)",
            example = "2025-01-15T15:00:00Z",
            accessMode = Schema.AccessMode.READ_ONLY
    )
    private LocalDateTime doctorSignedAt;

    @Schema(
            description = "Username of doctor who signed (future use for multi-party signatures)",
            example = "dr.anesthesiologist",
            accessMode = Schema.AccessMode.READ_ONLY
    )
    private String doctorSignedByUsername;

    // ==================== VALIDITY PERIOD ====================

    @Schema(
            description = "Start date of form validity (for time-limited consent)",
            example = "2025-01-01"
    )
    private LocalDate validFrom;

    @Schema(
            description = "End date of form validity (e.g., consent valid for 1 year)",
            example = "2026-01-01"
    )
    private LocalDate validUntil;

    @Schema(
            description = "Whether the form has expired (current date > validUntil)",
            example = "false",
            accessMode = Schema.AccessMode.READ_ONLY
    )
    private Boolean isExpired;

    @Schema(
            description = "Whether the form is signed by patient",
            example = "true",
            accessMode = Schema.AccessMode.READ_ONLY
    )
    private Boolean isSigned;

    @Schema(
            description = "Whether the form is currently valid (signed, not expired, not deleted)",
            example = "true",
            accessMode = Schema.AccessMode.READ_ONLY
    )
    private Boolean isValid;

    // ==================== AUDIT METADATA ====================

    @Schema(
            description = "Timestamp when form record was created",
            example = "2025-01-15T10:00:00Z",
            accessMode = Schema.AccessMode.READ_ONLY
    )
    private LocalDateTime createdAt;

    @Schema(
            description = "Timestamp when form was last updated",
            example = "2025-01-15T14:30:00Z",
            accessMode = Schema.AccessMode.READ_ONLY
    )
    private LocalDateTime updatedAt;

    @Schema(
            description = "Username of user who created this form record",
            example = "receptionist.ana",
            accessMode = Schema.AccessMode.READ_ONLY
    )
    private String createdByUsername;

    @Schema(
            description = "Whether the form is soft-deleted",
            example = "false",
            accessMode = Schema.AccessMode.READ_ONLY
    )
    private Boolean isDeleted;

    // ==================== CONVENIENCE COMPUTED FIELDS ====================

    /**
     * Checks if this is a consent form type.
     */
    public boolean isConsentForm() {
        return formType != null && formType.isConsentForm();
    }

    /**
     * Gets human-readable form type name.
     */
    public String getFormTypeName() {
        return formType != null ? formType.getDisplayName() : null;
    }

    /**
     * Gets human-readable status name.
     */
    public String getStatusName() {
        return status != null ? status.getDisplayName() : null;
    }

    /**
     * Checks if form requires action (not yet signed or rejected).
     */
    public boolean requiresAction() {
        return status != null && status.requiresAction();
    }
}
