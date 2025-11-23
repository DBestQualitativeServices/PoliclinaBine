package com.example.policlicabine.builder;

import com.example.policlicabine.entity.Permission;
import com.example.policlicabine.entity.enums.PermissionEnum;

import java.util.UUID;

public class PermissionTestBuilder {

    private UUID permissionId = UUID.randomUUID();
    private PermissionEnum name;
    private String description;

    public static PermissionTestBuilder aPermission(PermissionEnum name) {
        return new PermissionTestBuilder().withName(name);
    }

    public static PermissionTestBuilder allPermission() {
        return new PermissionTestBuilder()
                .withName(PermissionEnum.ALL)
                .withDescription("Full system access - all permissions granted");
    }

    public PermissionTestBuilder withPermissionId(UUID permissionId) {
        this.permissionId = permissionId;
        return this;
    }

    public PermissionTestBuilder withName(PermissionEnum name) {
        this.name = name;
        return this;
    }

    public PermissionTestBuilder withDescription(String description) {
        this.description = description;
        return this;
    }

    public Permission build() {
        return Permission.builder()
                .permissionId(permissionId)
                .name(name)
                .description(description)
                .build();
    }
}
