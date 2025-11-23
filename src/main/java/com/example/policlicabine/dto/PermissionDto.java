package com.example.policlicabine.dto;

import com.example.policlicabine.entity.enums.PermissionEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(
        name = "Permission",
        description = "System permission that can be assigned to roles"
)
public class PermissionDto {

    @Schema(
            description = "Unique permission identifier (UUID)",
            example = "123e4567-e89b-12d3-a456-426614174000",
            accessMode = Schema.AccessMode.READ_ONLY
    )
    private UUID permissionId;

    @Schema(
            description = "Permission name (enum)",
            example = "ALL",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private PermissionEnum name;

    @Schema(
            description = "Human-readable permission description",
            example = "Full system access - all permissions granted",
            accessMode = Schema.AccessMode.READ_ONLY
    )
    private String description;
}
