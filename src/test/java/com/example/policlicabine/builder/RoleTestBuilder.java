package com.example.policlicabine.builder;

import com.example.policlicabine.entity.Permission;
import com.example.policlicabine.entity.Role;
import com.example.policlicabine.entity.enums.UserRole;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class RoleTestBuilder {

    private UUID roleId = UUID.randomUUID();
    private UserRole name;
    private Set<Permission> permissions = new HashSet<>();

    public static RoleTestBuilder aRole(UserRole roleName) {
        return new RoleTestBuilder().withName(roleName);
    }

    public static RoleTestBuilder aDoctorRole() {
        return new RoleTestBuilder().withName(UserRole.DOCTOR);
    }

    public static RoleTestBuilder anAdminRole() {
        return new RoleTestBuilder().withName(UserRole.ADMIN);
    }

    public static RoleTestBuilder aReceptionistRole() {
        return new RoleTestBuilder().withName(UserRole.RECEPTIONIST);
    }

    public static RoleTestBuilder aManagerRole() {
        return new RoleTestBuilder().withName(UserRole.MANAGER);
    }

    public RoleTestBuilder withRoleId(UUID roleId) {
        this.roleId = roleId;
        return this;
    }

    public RoleTestBuilder withName(UserRole name) {
        this.name = name;
        return this;
    }

    public RoleTestBuilder withPermissions(Permission... perms) {
        this.permissions = Set.of(perms);
        return this;
    }

    public RoleTestBuilder withPermissions(Set<Permission> permissions) {
        this.permissions = new HashSet<>(permissions);
        return this;
    }

    public Role build() {
        Role role = Role.builder()
                .roleId(roleId)
                .name(name)
                .build();

        role.setPermissions(new HashSet<>(permissions));
        return role;
    }
}
