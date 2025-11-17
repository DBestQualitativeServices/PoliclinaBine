package com.example.policlicabine.builder;

import com.example.policlicabine.entity.User;
import com.example.policlicabine.entity.enums.UserRole;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * Test data builder for User entity using the Builder pattern.
 * <p>
 * Provides sensible defaults for test data while allowing customization.
 * </p>
 * <p>Example usage:</p>
 * <pre>
 * User doctor = UserTestBuilder.aDoctor()
 *     .withFullName("Dr. Jane Smith")
 *     .withUsername("jsmith")
 *     .build();
 *
 * User admin = UserTestBuilder.aUser()
 *     .withRole(UserRole.ADMIN)
 *     .build();
 * </pre>
 */
public class UserTestBuilder {

    private UUID userId = UUID.randomUUID();
    private String username = "testuser_" + UUID.randomUUID().toString().substring(0, 8);
    private String fullName = "Test User";
    private UserRole role = UserRole.RECEPTIONIST;
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
                .withRole(UserRole.DOCTOR)
                .withFullName("Dr. Test Doctor");
    }

    public static UserTestBuilder anAdmin() {
        return new UserTestBuilder()
                .withRole(UserRole.ADMIN)
                .withFullName("Admin User");
    }

    public static UserTestBuilder aReceptionist() {
        return new UserTestBuilder()
                .withRole(UserRole.RECEPTIONIST)
                .withFullName("Receptionist User");
    }

    public UserTestBuilder withUserId(UUID userId) {
        this.userId = userId;
        return this;
    }

    public UserTestBuilder withUsername(String username) {
        this.username = username;
        return this;
    }

    public UserTestBuilder withFullName(String fullName) {
        this.fullName = fullName;
        return this;
    }

    public UserTestBuilder withRole(UserRole role) {
        this.role = role;
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
        return User.builder()
                .userId(userId)
                .username(username)
                .fullName(fullName)
                .role(role)
                .password(password)
                .enabled(enabled)
                .accountNonLocked(accountNonLocked)
                .createdAt(createdAt)
                .lastLogin(lastLogin)
                .build();
    }
}
