package com.example.policlicabine.dto;

import com.example.policlicabine.entity.enums.FileCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Data Transfer Object for File entity.
 *
 * <p>Used for API responses and contains all file metadata
 * including computed fields like downloadUrl and isExpired.
 *
 * @author PoliclicaBine System
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "File metadata and information")
public class FileDto {

    @Schema(description = "Unique file identifier (UUID v7 - time-sorted)",
            example = "018e1234-5678-7abc-def0-123456789abc")
    private UUID id;

    @Schema(description = "Original filename as provided by user",
            example = "patient-consent-form.pdf")
    @NotNull
    private String originalFilename;

    @Schema(description = "File size in bytes",
            example = "1048576")
    private Long fileSize;

    @Schema(description = "MIME type of the file",
            example = "image/jpeg")
    private String mimeType;

    @Schema(description = "File category classification")
    private FileCategory fileCategory;

    @Schema(description = "Timestamp when file was uploaded")
    private LocalDateTime uploadedAt;

    @Schema(description = "Start date of file validity (optional)")
    private LocalDate validFrom;

    @Schema(description = "End date of file validity (e.g., consent expires after 1 year)")
    private LocalDate validUntil;

    @Schema(description = "User who uploaded the file")
    private UserDto uploadedBy;

    @Schema(description = "Patient who owns this file (optional - not all files are patient-specific)",
            example = "018e1234-5678-7abc-def0-123456789abc")
    private UUID patientId;

    @Schema(description = "Version number of the file", example = "1")
    private Integer version;

    @Schema(description = "UUID of the previous version (if this is an updated file)")
    private UUID previousVersionId;

    @Schema(description = "Download URL for the file",
            example = "/api/files/018e1234-5678-7abc-def0-123456789abc/download",
            accessMode = Schema.AccessMode.READ_ONLY)
    private String downloadUrl;

    @Schema(description = "Whether the file has expired based on validUntil date",
            example = "false",
            accessMode = Schema.AccessMode.READ_ONLY)
    private Boolean isExpired;

    @Schema(description = "Whether the file is still active (not deleted and not expired)",
            example = "true",
            accessMode = Schema.AccessMode.READ_ONLY)
    private Boolean isActive;

    @Schema(description = "Human-readable file size",
            example = "1.5 MB",
            accessMode = Schema.AccessMode.READ_ONLY)
    private String fileSizeFormatted;
}
