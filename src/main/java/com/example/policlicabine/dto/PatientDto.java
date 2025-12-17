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

import java.time.LocalDate;
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

    @Schema(
            description = "Patient user account (for portal login). Null if patient has no account yet.",
            accessMode = Schema.AccessMode.READ_ONLY
    )
    private UserDto user;

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

    @Size(max = 1000)
    @Schema(
            description = "Patient's legal residence address (domiciliu) as per identity card",
            example = "Str. Victoriei nr. 25, Cluj-Napoca, Romania",
            maxLength = 1000
    )
    private String domiciliu;

    @Size(min = 2, max = 2)
    @Pattern(regexp = "^[A-Z]{2}$", message = "ciSerie must be 2 uppercase letters")
    @Schema(
            description = "Identity card series (2 uppercase letters)",
            example = "CJ",
            pattern = "^[A-Z]{2}$",
            minLength = 2,
            maxLength = 2
    )
    private String ciSerie;

    @Pattern(regexp = "^[0-9]{6,7}$", message = "CI number must be 6-7 digits")
    @Schema(
            description = "Identity card number (6-7 digits)",
            example = "123456",
            pattern = "^[0-9]{6,7}$"
    )
    private String ciNumber;

    @Size(max = 200)
    @Schema(
            description = "Identity card issuing authority",
            example = "SPCLEP Cluj-Napoca",
            maxLength = 200
    )
    private String ciEliberatDe;

    @Schema(
            description = "Identity card issue date",
            example = "2020-05-15",
            format = "date"
    )
    private LocalDate ciDataEliberare;

    @Pattern(regexp = "^[0-9]{13}$", message = "CNP must be 13 digits")
    @Schema(
            description = "Personal Numeric Code (CNP) - Romanian national identification number (13 digits)",
            example = "1920515123456",
            pattern = "^[0-9]{13}$",
            maxLength = 13
    )
    private String cnp;

    @Schema(
            description = "Source of patient registration (e.g., 'online', 'clinic', 'referral')",
            example = "online",
            maxLength = 255
    )
    private String sursa;

    @Pattern(regexp = "^[MF]$", message = "Sex must be 'M' or 'F'")
    @Schema(
            description = "Patient's sex (M=Male, F=Female). Auto-calculated from CNP if available.",
            example = "F",
            allowableValues = {"M", "F"}
    )
    private String sex;

    @Schema(
            description = "Whether the patient is a minor. Auto-calculated from CNP if available.",
            example = "false"
    )
    private Boolean minor;

    @Schema(
            description = "Timestamp when patient was registered in the system",
            example = "2025-01-15T10:30:00Z",
            accessMode = Schema.AccessMode.READ_ONLY
    )
    private OffsetDateTime registrationDate;
}
