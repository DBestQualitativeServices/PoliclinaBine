package com.example.policlicabine.service;

import com.example.policlicabine.builder.UserTestBuilder;
import com.example.policlicabine.entity.User;
import com.example.policlicabine.entity.enums.UserRole;
import com.example.policlicabine.repository.UserRepository;
import com.example.policlicabine.security.CustomUserDetailsService;
import com.example.policlicabine.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link CustomUserDetailsService}.
 * <p>
 * Tests Spring Security UserDetailsService implementation:
 * - Loading user by username from database
 * - Converting User entity to Spring Security UserDetails
 * - Role mapping (DOCTOR → ROLE_DOCTOR)
 * - Account status flags (enabled, locked, expired, credentials expired)
 * - Special case: users with null/empty password (migration support)
 * <p>
 * Uses Mockito mocks for UserRepository.
 */
@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    private CustomUserDetailsService userDetailsService;

    @BeforeEach
    void setUp() {
        userDetailsService = new CustomUserDetailsService(userRepository);
    }

    // ===== LOAD USER BY USERNAME TESTS =====

    @Test
    void loadUserByUsername_WithExistingUser_ShouldReturnUserPrincipal() {
        User testUser = UserTestBuilder.aDoctor()
                .withUsername("drsmith")
                .withPassword("$2a$10$hashedPassword")
                .build();
        testUser.setEnabled(true);
        testUser.setAccountNonLocked(true);

        when(userRepository.findWithRolesPermissionsAndProfiles("drsmith")).thenReturn(Optional.of(testUser));

        UserDetails userDetails = userDetailsService.loadUserByUsername("drsmith");

        assertThat(userDetails).isNotNull();
        assertThat(userDetails).isInstanceOf(UserPrincipal.class);
        assertThat(userDetails.getUsername()).isEqualTo("drsmith");
        assertThat(userDetails.getPassword()).isEqualTo("$2a$10$hashedPassword");
        assertThat(userDetails.isEnabled()).isTrue();
        assertThat(userDetails.isAccountNonLocked()).isTrue();
        assertThat(userDetails.isAccountNonExpired()).isTrue();
        assertThat(userDetails.isCredentialsNonExpired()).isTrue();

        UserPrincipal principal = (UserPrincipal) userDetails;
        assertThat(principal.getUserId()).isEqualTo(testUser.getUserId());

        assertThat(userDetails.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .contains("ROLE_DOCTOR");

        verify(userRepository).findWithRolesPermissionsAndProfiles("drsmith");
    }

    @Test
    void loadUserByUsername_WithNonExistentUser_ShouldThrowException() {
        // Given
        when(userRepository.findWithRolesPermissionsAndProfiles("nonexistent")).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("nonexistent"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("User not found");

        verify(userRepository).findWithRolesPermissionsAndProfiles("nonexistent");
    }

    // ===== PASSWORD VALIDATION TESTS =====

    @Test
    void loadUserByUsername_WithNullPassword_ShouldThrowException() {
        // Given - User with null password (migration scenario)
        User userWithoutPassword = UserTestBuilder.aDoctor()
                .withUsername("olduser")
                .build();
        userWithoutPassword.setPassword(null);

        when(userRepository.findWithRolesPermissionsAndProfiles("olduser")).thenReturn(Optional.of(userWithoutPassword));

        // When/Then
        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("olduser"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("User has no password set")
                .hasMessageContaining("contact administrator");
    }

    @Test
    void loadUserByUsername_WithEmptyPassword_ShouldThrowException() {
        // Given - User with empty password
        User userWithEmptyPassword = UserTestBuilder.aDoctor()
                .withUsername("emptypass")
                .withPassword("")
                .build();

        when(userRepository.findWithRolesPermissionsAndProfiles("emptypass")).thenReturn(Optional.of(userWithEmptyPassword));

        // When/Then
        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("emptypass"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("User has no password set");
    }

    @Test
    void loadUserByUsername_WithBlankPassword_ShouldThrowException() {
        // Given - User with blank password (spaces only)
        User userWithBlankPassword = UserTestBuilder.aDoctor()
                .withUsername("blankpass")
                .withPassword("   ")
                .build();

        when(userRepository.findWithRolesPermissionsAndProfiles("blankpass")).thenReturn(Optional.of(userWithBlankPassword));

        // When/Then
        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("blankpass"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("User has no password set");
    }

    // ===== ROLE MAPPING TESTS =====

    @Test
    void loadUserByUsername_WithDoctorRole_ShouldMapToRoleDoctor() {
        User doctor = UserTestBuilder.aDoctor()
                .withUsername("doctor")
                .withPassword("password")
                .build();

        when(userRepository.findWithRolesPermissionsAndProfiles("doctor")).thenReturn(Optional.of(doctor));

        UserDetails userDetails = userDetailsService.loadUserByUsername("doctor");

        assertThat(userDetails.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .contains("ROLE_DOCTOR");
    }

    @Test
    void loadUserByUsername_WithAdminRole_ShouldMapToRoleAdmin() {
        User admin = UserTestBuilder.anAdmin()
                .withUsername("admin")
                .withPassword("password")
                .build();

        when(userRepository.findWithRolesPermissionsAndProfiles("admin")).thenReturn(Optional.of(admin));

        UserDetails userDetails = userDetailsService.loadUserByUsername("admin");

        assertThat(userDetails.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .contains("ROLE_ADMIN");
    }

    @Test
    void loadUserByUsername_WithManagerRole_ShouldMapToRoleManager() {
        User manager = UserTestBuilder.aUser()
                .withUsername("manager")
                .withPassword("password")
                .withRoles(Set.of(UserRole.MANAGER))
                .build();

        when(userRepository.findWithRolesPermissionsAndProfiles("manager")).thenReturn(Optional.of(manager));

        UserDetails userDetails = userDetailsService.loadUserByUsername("manager");

        assertThat(userDetails.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .contains("ROLE_MANAGER");
    }

    // ===== ACCOUNT STATUS TESTS =====

    @Test
    void loadUserByUsername_WithEnabledUser_ShouldReturnEnabledUserDetails() {
        // Given
        User enabledUser = UserTestBuilder.aDoctor()
                .withUsername("enabled")
                .withPassword("password")
                .build();
        enabledUser.setEnabled(true);

        when(userRepository.findWithRolesPermissionsAndProfiles("enabled")).thenReturn(Optional.of(enabledUser));

        // When
        UserDetails userDetails = userDetailsService.loadUserByUsername("enabled");

        // Then
        assertThat(userDetails.isEnabled()).isTrue();
    }

    @Test
    void loadUserByUsername_WithDisabledUser_ShouldReturnDisabledUserDetails() {
        // Given
        User disabledUser = UserTestBuilder.aDoctor()
                .withUsername("disabled")
                .withPassword("password")
                .build();
        disabledUser.setEnabled(false);

        when(userRepository.findWithRolesPermissionsAndProfiles("disabled")).thenReturn(Optional.of(disabledUser));

        // When
        UserDetails userDetails = userDetailsService.loadUserByUsername("disabled");

        // Then
        assertThat(userDetails.isEnabled()).isFalse();
    }

    @Test
    void loadUserByUsername_WithLockedAccount_ShouldReturnLockedUserDetails() {
        // Given
        User lockedUser = UserTestBuilder.aDoctor()
                .withUsername("locked")
                .withPassword("password")
                .build();
        lockedUser.setAccountNonLocked(false);

        when(userRepository.findWithRolesPermissionsAndProfiles("locked")).thenReturn(Optional.of(lockedUser));

        // When
        UserDetails userDetails = userDetailsService.loadUserByUsername("locked");

        // Then
        assertThat(userDetails.isAccountNonLocked()).isFalse();
    }

    @Test
    void loadUserByUsername_WithNonLockedAccount_ShouldReturnNonLockedUserDetails() {
        // Given
        User nonLockedUser = UserTestBuilder.aDoctor()
                .withUsername("nonlocked")
                .withPassword("password")
                .build();
        nonLockedUser.setAccountNonLocked(true);

        when(userRepository.findWithRolesPermissionsAndProfiles("nonlocked")).thenReturn(Optional.of(nonLockedUser));

        // When
        UserDetails userDetails = userDetailsService.loadUserByUsername("nonlocked");

        // Then
        assertThat(userDetails.isAccountNonLocked()).isTrue();
    }

    @Test
    void loadUserByUsername_ShouldAlwaysSetAccountNonExpiredToTrue() {
        // Given - Account expiration not implemented in User entity
        User user = UserTestBuilder.aDoctor()
                .withUsername("user")
                .withPassword("password")
                .build();

        when(userRepository.findWithRolesPermissionsAndProfiles("user")).thenReturn(Optional.of(user));

        // When
        UserDetails userDetails = userDetailsService.loadUserByUsername("user");

        // Then
        assertThat(userDetails.isAccountNonExpired()).isTrue();
    }

    @Test
    void loadUserByUsername_ShouldAlwaysSetCredentialsNonExpiredToTrue() {
        // Given - Credentials expiration not implemented in User entity
        User user = UserTestBuilder.aDoctor()
                .withUsername("user")
                .withPassword("password")
                .build();

        when(userRepository.findWithRolesPermissionsAndProfiles("user")).thenReturn(Optional.of(user));

        // When
        UserDetails userDetails = userDetailsService.loadUserByUsername("user");

        // Then
        assertThat(userDetails.isCredentialsNonExpired()).isTrue();
    }

    // ===== EDGE CASES =====

    @Test
    void loadUserByUsername_WithCaseSensitiveUsername_ShouldMatchExactly() {
        // Given
        User user = UserTestBuilder.aDoctor()
                .withUsername("TestUser")
                .withPassword("password")
                .build();

        when(userRepository.findWithRolesPermissionsAndProfiles("TestUser")).thenReturn(Optional.of(user));
        when(userRepository.findWithRolesPermissionsAndProfiles("testuser")).thenReturn(Optional.empty());

        // When/Then - Exact match should work
        UserDetails userDetails = userDetailsService.loadUserByUsername("TestUser");
        assertThat(userDetails.getUsername()).isEqualTo("TestUser");

        // Different case should fail
        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("testuser"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void loadUserByUsername_ShouldNotTrimUsername() {
        // Given - Username with spaces
        User user = UserTestBuilder.aDoctor()
                .withUsername("user with spaces")
                .withPassword("password")
                .build();

        when(userRepository.findWithRolesPermissionsAndProfiles("user with spaces")).thenReturn(Optional.of(user));

        // When
        UserDetails userDetails = userDetailsService.loadUserByUsername("user with spaces");

        // Then
        assertThat(userDetails.getUsername()).isEqualTo("user with spaces");
    }
}
