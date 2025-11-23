package com.example.policlicabine.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(
        name = "Manager",
        description = "Clinic manager profile with department and hire date information"
)
public class ManagerDto {

    @Schema(
            description = "Unique manager identifier (UUID)",
            example = "123e4567-e89b-12d3-a456-426614174000",
            accessMode = Schema.AccessMode.READ_ONLY
    )
    private UUID managerId;

    @Schema(
            description = "Associated user account (must have MANAGER role)",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private UserDto user;

    @Schema(
            description = "Manager's full name",
            example = "John Doe",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String fullName;

    @Size(max = 100)
    @Schema(
            description = "Manager's department (e.g., Billing, HR, Operations, Clinical Services)",
            example = "Clinical Services",
            maxLength = 100
    )
    private String department;

    @Schema(
            description = "Date when manager was hired",
            example = "2024-01-15T00:00:00Z"
    )
    private OffsetDateTime hireDate;
}
