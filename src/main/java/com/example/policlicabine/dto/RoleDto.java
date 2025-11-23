package com.example.policlicabine.dto;

import com.example.policlicabine.entity.enums.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(
        name = "Role",
        description = "User role with associated permissions for RBAC"
)
public class RoleDto {

    @Schema(
            description = "Unique role identifier (UUID)",
            example = "123e4567-e89b-12d3-a456-426614174000",
            accessMode = Schema.AccessMode.READ_ONLY
    )
    private UUID roleId;

    @Schema(
            description = "Role name (enum: DOCTOR, PATIENT, MANAGER)",
            example = "DOCTOR",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private UserRole name;

    @Schema(
            description = "Permissions granted by this role",
            accessMode = Schema.AccessMode.READ_ONLY
    )
    @Builder.Default
    private Set<PermissionDto> permissions = new HashSet<>();
}
