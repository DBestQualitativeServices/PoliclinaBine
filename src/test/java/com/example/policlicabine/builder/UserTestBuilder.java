package com.example.policlicabine.builder;

import com.example.policlicabine.entity.Role;
import com.example.policlicabine.entity.User;
import com.example.policlicabine.entity.enums.UserRole;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class UserTestBuilder {

    private UUID userId = UUID.randomUUID();
    private String username = "testuser_" + UUID.randomUUID().toString().substring(0, 8);
    private Set<UserRole> roles = Set.of(UserRole.RECEPTIONIST);
    private String password = "$2a$10$dummyHashedPassword";
    private boolean enabled = true;
    private boolean accountNonLocked = true;
    private OffsetDateTime createdAt = OffsetDateTime.now(ZoneOffset.UTC);
    private OffsetDateTime lastLogin = null;

    public static UserTestBuilder aUser() {
        return new UserTestBuilder();
    }

    public static UserTestBuilder aDoctor() {
        return new UserTestBuilder()
                .withRoles(Set.of(UserRole.DOCTOR));
    }

    public static UserTestBuilder anAdmin() {
        return new UserTestBuilder()
                .withRoles(Set.of(UserRole.ADMIN));
    }

    public static UserTestBuilder aReceptionist() {
        return new UserTestBuilder()
                .withRoles(Set.of(UserRole.RECEPTIONIST));
    }

    public UserTestBuilder withUserId(UUID userId) {
        this.userId = userId;
        return this;
    }

    public UserTestBuilder withUsername(String username) {
        this.username = username;
        return this;
    }

    public UserTestBuilder withRoles(Set<UserRole> roles) {
        this.roles = roles;
        return this;
    }

    public UserTestBuilder withPassword(String password) {
        this.password = password;
        return this;
    }

    public UserTestBuilder withEnabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public UserTestBuilder withAccountNonLocked(boolean accountNonLocked) {
        this.accountNonLocked = accountNonLocked;
        return this;
    }

    public UserTestBuilder withCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public UserTestBuilder withLastLogin(OffsetDateTime lastLogin) {
        this.lastLogin = lastLogin;
        return this;
    }

    public User build() {
        User user = User.builder()
                .userId(userId)
                .username(username)
                .password(password)
                .enabled(enabled)
                .accountNonLocked(accountNonLocked)
                .createdAt(createdAt)
                .lastLogin(lastLogin)
                .build();

        Set<Role> roleEntities = roles.stream()
                .map(userRole -> RoleTestBuilder.aRole(userRole).build())
                .collect(Collectors.toSet());
        user.setRoles(roleEntities);

        return user;
    }
}
