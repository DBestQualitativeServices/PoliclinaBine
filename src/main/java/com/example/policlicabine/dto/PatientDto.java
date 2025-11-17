package com.example.policlicabine.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Patient Data Transfer Object.
 *
 * Contains patient registration and profile information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(
        name = "Patient",
        description = "Patient registration and personal information"
)
public class PatientDto {

    @Schema(
            description = "Unique patient identifier (UUID)",
            example = "123e4567-e89b-12d3-a456-426614174000",
            accessMode = Schema.AccessMode.READ_ONLY
    )
    private UUID patientId;

    @NotBlank
    @Size(min = 2, max = 100)
    @Schema(
            description = "Patient's first name",
            example = "Maria",
            requiredMode = Schema.RequiredMode.REQUIRED,
            minLength = 2,
            maxLength = 100
    )
    private String firstName;

    @NotBlank
    @Size(min = 2, max = 100)
    @Schema(
            description = "Patient's last name",
            example = "Popescu",
            requiredMode = Schema.RequiredMode.REQUIRED,
            minLength = 2,
            maxLength = 100
    )
    private String lastName;

    @NotBlank
    @Pattern(regexp = "^\\+?[0-9]{10,20}$", message = "Phone must be 10-20 digits, optional + prefix")
    @Size(max = 20)
    @Schema(
            description = "Patient's phone number (10-20 digits, optional + prefix)",
            example = "+40712345678",
            requiredMode = Schema.RequiredMode.REQUIRED,
            pattern = "^\\+?[0-9]{10,20}$",
            maxLength = 20
    )
    private String phone;

    @Email
    @Size(max = 100)
    @Schema(
            description = "Patient's email address",
            example = "maria.popescu@email.com",
            format = "email",
            maxLength = 100
    )
    private String email;

    @Size(max = 1000)
    @Schema(
            description = "Patient's residential address",
            example = "Str. Mihai Eminescu nr. 15, Bucharest, Romania",
            maxLength = 1000
    )
    private String address;

    @Size(max = 500)
    @Schema(
            description = "URL to patient's consent file (if signed)",
            example = "https://storage.policlicabine.com/consent/patient-123.pdf",
            maxLength = 500,
            accessMode = Schema.AccessMode.READ_ONLY
    )
    private String consentFileUrl;

    @Schema(
            description = "Timestamp when patient was registered in the system",
            example = "2025-01-15T10:30:00Z",
            accessMode = Schema.AccessMode.READ_ONLY
    )
    private OffsetDateTime registrationDate;
}
