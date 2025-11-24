package com.example.policlicabine.service;

import com.example.policlicabine.base.BaseServiceTest;
import com.example.policlicabine.builder.RoleTestBuilder;
import com.example.policlicabine.builder.UserTestBuilder;
import com.example.policlicabine.common.Result;
import com.example.policlicabine.dto.*;
import com.example.policlicabine.entity.PasswordResetToken;
import com.example.policlicabine.entity.Role;
import com.example.policlicabine.entity.User;
import com.example.policlicabine.entity.enums.UserRole;
import com.example.policlicabine.event.*;
import com.example.policlicabine.repository.RoleRepository;
import com.example.policlicabine.repository.UserRepository;
import com.example.policlicabine.security.JwtService;
import com.example.policlicabine.util.CredentialGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static com.example.policlicabine.util.ResultAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link AuthenticationService}.
 * <p>
 * Tests all authentication operations including:
 * - User registration with username uniqueness validation
 * - User authentication with credentials
 * - Token refresh functionality
 * - Password change with current password verification
 * - Password reset initiation (secure - always returns success)
 * - Password reset completion with token validation
 * <p>
 * Uses Mockito mocks for all dependencies and verifies domain event publishing.
 */
@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest extends BaseServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordResetTokenService passwordResetTokenService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private PatientService patientService;

    @Mock
    private DoctorService doctorService;

    @Mock
    private ManagerService managerService;

    @Mock
    private CredentialGenerator credentialGenerator;

    private AuthenticationService authenticationService;

    private User testUser;
    private UserDetails testUserDetails;

    @BeforeEach
    void setUp() {
        eventPublisher = createEventPublisher();

        authenticationService = new AuthenticationService(
                userRepository,
                roleRepository,
                passwordResetTokenService,
                passwordEncoder,
                jwtService,
                authenticationManager,
                userDetailsService,
                eventPublisher,
                patientService,
                doctorService,
                managerService,
                credentialGenerator
        );

        // Setup common test data
        testUser = UserTestBuilder.aDoctor()
                .withUsername("testuser")
                .withPassword("$2a$10$hashedPassword")
                .build();

        testUserDetails = org.springframework.security.core.userdetails.User.builder()
                .username("testuser")
                .password("$2a$10$hashedPassword")
                .authorities(new SimpleGrantedAuthority("ROLE_DOCTOR"))
                .build();
    }

    // ===== REGISTER TESTS =====

    @Test
    void register_WithValidRequest_ShouldReturnSuccessAndPublishEvent() {
        // Given
        RegisterRequest request = RegisterRequest.builder()
                .username("newuser")
                .password("password123")
                .roles(Set.of(UserRole.DOCTOR))
                .build();

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("$2a$10$encodedPassword");

        // Mock role repository to return Role entities
        Role doctorRole = RoleTestBuilder.aRole(UserRole.DOCTOR).build();
        when(roleRepository.findByNameIn(Set.of(UserRole.DOCTOR))).thenReturn(Set.of(doctorRole));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setUserId(UUID.randomUUID());
            return user;
        });
        // Stub userDetailsService to return UserDetails with authorities for JWT generation
        when(userDetailsService.loadUserByUsername(anyString())).thenAnswer(invocation -> {
            String username = invocation.getArgument(0);
            return org.springframework.security.core.userdetails.User.builder()
                    .username(username)
                    .password("password")
                    .authorities(new SimpleGrantedAuthority("ROLE_DOCTOR"))
                    .build();
        });
        when(jwtService.generateToken(any(UserDetails.class), anyString())).thenReturn("access-token");
        when(jwtService.generateRefreshToken(any(UserDetails.class))).thenReturn("refresh-token");

        // When
        Result<AuthResponse> result = authenticationService.register(request);

        // Then
        assertThat(result)
                .isSuccess()
                .hasValue()
                .hasValueSatisfying(response -> {
                    assertThat(response.getUsername()).isEqualTo("newuser");
                    assertThat(response.getRoles()).contains(UserRole.DOCTOR);
                    assertThat(response.getAccessToken()).isEqualTo("access-token");
                    assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
                    assertThat(response.getTokenType()).isEqualTo("Bearer");
                });

        // Verify user saved
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getUsername()).isEqualTo("newuser");
        assertThat(savedUser.getPassword()).isEqualTo("$2a$10$encodedPassword");
        assertThat(savedUser.getRoles()).isNotEmpty();
        assertThat(savedUser.isEnabled()).isTrue();
        assertThat(savedUser.isAccountNonLocked()).isTrue();

        // Verify event published
        ArgumentCaptor<UserRegistered> eventCaptor = ArgumentCaptor.forClass(UserRegistered.class);
        verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());
        UserRegistered event = eventCaptor.getValue();
        assertThat(event.username()).isEqualTo("newuser");
        assertThat(event.roles()).contains(UserRole.DOCTOR);
    }

    @Test
    void register_WithDuplicateUsername_ShouldReturnFailure() {
        // Given
        RegisterRequest request = RegisterRequest.builder()
                .username("existinguser")
                .password("password123")
                .roles(Set.of(UserRole.DOCTOR))
                .build();

        when(userRepository.existsByUsername("existinguser")).thenReturn(true);

        // When
        Result<AuthResponse> result = authenticationService.register(request);

        // Then
        assertThat(result)
                .isFailure()
                .hasErrorMessageContaining("Username already exists");

        // Verify no save or event
        verify(userRepository, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }

    // ===== AUTHENTICATE TESTS =====

    @Test
    void authenticate_WithValidCredentials_ShouldReturnSuccessAndPublishEvent() {
        // Given
        LoginRequest request = LoginRequest.builder()
                .username("testuser")
                .password("password123")
                .build();

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                testUserDetails, "password123", testUserDetails.getAuthorities()
        );

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(userDetailsService.loadUserByUsername("testuser")).thenReturn(testUserDetails);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(jwtService.generateToken(eq(testUserDetails), anyString())).thenReturn("access-token");
        when(jwtService.generateRefreshToken(testUserDetails)).thenReturn("refresh-token");

        // When
        Result<AuthResponse> result = authenticationService.authenticate(request, "127.0.0.1");

        // Then
        assertThat(result)
                .isSuccess()
                .hasValue()
                .hasValueSatisfying(response -> {
                    assertThat(response.getUsername()).isEqualTo("testuser");
                    assertThat(response.getAccessToken()).isEqualTo("access-token");
                    assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
                });

        // Verify lastLogin updated
        verify(userRepository).save(any(User.class));

        // Verify event published
        ArgumentCaptor<UserAuthenticated> eventCaptor = ArgumentCaptor.forClass(UserAuthenticated.class);
        verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());
        UserAuthenticated event = eventCaptor.getValue();
        assertThat(event.username()).isEqualTo("testuser");
        assertThat(event.ipAddress()).isEqualTo("127.0.0.1");
        assertThat(event.loginTime()).isNotNull();
    }

    @Test
    void authenticate_WithInvalidCredentials_ShouldReturnFailure() {
        // Given
        LoginRequest request = LoginRequest.builder()
                .username("testuser")
                .password("wrongpassword")
                .build();

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        // When
        Result<AuthResponse> result = authenticationService.authenticate(request, "127.0.0.1");

        // Then
        assertThat(result)
                .isFailure()
                .hasErrorMessageContaining("Invalid credentials");

        // Verify no event published
        verifyNoInteractions(eventPublisher);
    }

    // ===== REFRESH TOKEN TESTS =====

    @Test
    void refreshToken_WithValidToken_ShouldReturnNewAccessToken() {
        // Given
        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken("valid-refresh-token")
                .build();

        when(jwtService.extractUsername("valid-refresh-token")).thenReturn("testuser");
        when(userDetailsService.loadUserByUsername("testuser")).thenReturn(testUserDetails);
        when(jwtService.isTokenValid("valid-refresh-token", testUserDetails)).thenReturn(true);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(jwtService.generateToken(any(UserDetails.class), anyString())).thenReturn("new-access-token");

        // When
        Result<AuthResponse> result = authenticationService.refreshToken(request);

        // Then
        assertThat(result)
                .isSuccess()
                .hasValue()
                .hasValueSatisfying(response -> {
                    assertThat(response.getAccessToken()).isEqualTo("new-access-token");
                    assertThat(response.getRefreshToken()).isEqualTo("valid-refresh-token");
                    assertThat(response.getUsername()).isEqualTo("testuser");
                });
    }

    @Test
    void refreshToken_WithInvalidToken_ShouldReturnFailure() {
        // Given
        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken("invalid-refresh-token")
                .build();

        when(jwtService.extractUsername("invalid-refresh-token")).thenReturn("testuser");
        when(userDetailsService.loadUserByUsername("testuser")).thenReturn(testUserDetails);
        when(jwtService.isTokenValid("invalid-refresh-token", testUserDetails)).thenReturn(false);

        // When
        Result<AuthResponse> result = authenticationService.refreshToken(request);

        // Then
        assertThat(result)
                .isFailure()
                .hasErrorMessageContaining("Invalid or expired refresh token");
    }

    // ===== CHANGE PASSWORD TESTS =====

    @Test
    void changePassword_WithCorrectCurrentPassword_ShouldReturnSuccessAndPublishEvent() {
        // Given
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("oldpassword")
                .newPassword("newpassword123")
                .build();

        // Mock SecurityContext
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                testUserDetails, null, testUserDetails.getAuthorities()
        );
        org.springframework.security.core.context.SecurityContextHolder.getContext()
                .setAuthentication(authentication);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("oldpassword", testUser.getPassword())).thenReturn(true);
        when(passwordEncoder.encode("newpassword123")).thenReturn("$2a$10$newEncodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        Result<Void> result = authenticationService.changePassword(request);

        // Then
        assertThat(result).isSuccess();

        // Verify password updated
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getPassword()).isEqualTo("$2a$10$newEncodedPassword");

        // Verify event published
        ArgumentCaptor<PasswordChanged> eventCaptor = ArgumentCaptor.forClass(PasswordChanged.class);
        verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());
        PasswordChanged event = eventCaptor.getValue();
        assertThat(event.username()).isEqualTo("testuser");
        assertThat(event.changedAt()).isNotNull();

        // Cleanup
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    @Test
    void changePassword_WithIncorrectCurrentPassword_ShouldReturnFailure() {
        // Given
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("wrongpassword")
                .newPassword("newpassword123")
                .build();

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                testUserDetails, null, testUserDetails.getAuthorities()
        );
        org.springframework.security.core.context.SecurityContextHolder.getContext()
                .setAuthentication(authentication);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongpassword", testUser.getPassword())).thenReturn(false);

        // When
        Result<Void> result = authenticationService.changePassword(request);

        // Then
        assertThat(result)
                .isFailure()
                .hasErrorMessageContaining("Current password is incorrect");

        // Verify no save or event
        verify(userRepository, never()).save(any());
        verifyNoInteractions(eventPublisher);

        // Cleanup
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    // ===== INITIATE PASSWORD RESET TESTS =====

    @Test
    void initiatePasswordReset_WithExistingUser_ShouldReturnSuccessAndPublishEvent() {
        // Given
        InitiatePasswordResetRequest request = InitiatePasswordResetRequest.builder()
                .username("testuser")
                .build();

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .tokenId(UUID.randomUUID())
                .token("reset-token-123")
                .user(testUser)
                .expiryDate(OffsetDateTime.now(ZoneOffset.UTC).plusHours(1))
                .used(false)
                .build();

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordResetTokenService.createResetToken(testUser.getUserId()))
                .thenReturn(Result.success(resetToken));

        // When
        Result<String> result = authenticationService.initiatePasswordReset(request);

        // Then
        assertThat(result)
                .isSuccess()
                .hasValue("Password reset token: reset-token-123");

        // Verify event published
        ArgumentCaptor<PasswordResetInitiated> eventCaptor = ArgumentCaptor.forClass(PasswordResetInitiated.class);
        verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());
        PasswordResetInitiated event = eventCaptor.getValue();
        assertThat(event.username()).isEqualTo("testuser");
        assertThat(event.resetToken()).isEqualTo("reset-token-123");
    }

    @Test
    void initiatePasswordReset_WithNonExistentUser_ShouldReturnSuccessWithoutEvent() {
        // Given - Security feature: always return success to prevent user enumeration
        InitiatePasswordResetRequest request = InitiatePasswordResetRequest.builder()
                .username("nonexistent")
                .build();

        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // When
        Result<String> result = authenticationService.initiatePasswordReset(request);

        // Then - Still returns success for security
        assertThat(result).isSuccess();

        // Verify no token created and no event published
        verifyNoInteractions(passwordResetTokenService);
        verifyNoInteractions(eventPublisher);
    }

    // ===== RESET PASSWORD TESTS =====

    @Test
    void resetPassword_WithValidToken_ShouldReturnSuccessAndPublishEvent() {
        // Given
        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .token("valid-token")
                .newPassword("newpassword123")
                .build();

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .tokenId(UUID.randomUUID())
                .token("valid-token")
                .user(testUser)
                .expiryDate(OffsetDateTime.now(ZoneOffset.UTC).plusHours(1))
                .used(false)
                .build();

        when(passwordResetTokenService.validateResetToken("valid-token"))
                .thenReturn(Result.success(resetToken));
        when(passwordEncoder.encode("newpassword123")).thenReturn("$2a$10$newEncodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(passwordResetTokenService.markTokenAsUsed("valid-token"))
                .thenReturn(Result.success());

        // When
        Result<Void> result = authenticationService.resetPassword(request);

        // Then
        assertThat(result).isSuccess();

        // Verify password updated
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getPassword()).isEqualTo("$2a$10$newEncodedPassword");

        // Verify token marked as used
        verify(passwordResetTokenService).markTokenAsUsed("valid-token");

        // Verify event published
        ArgumentCaptor<PasswordReset> eventCaptor = ArgumentCaptor.forClass(PasswordReset.class);
        verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());
        PasswordReset event = eventCaptor.getValue();
        assertThat(event.username()).isEqualTo("testuser");
        assertThat(event.resetAt()).isNotNull();
    }

    @Test
    void resetPassword_WithInvalidToken_ShouldReturnFailure() {
        // Given
        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .token("invalid-token")
                .newPassword("newpassword123")
                .build();

        when(passwordResetTokenService.validateResetToken("invalid-token"))
                .thenReturn(Result.failure("Invalid or expired reset token"));

        // When
        Result<Void> result = authenticationService.resetPassword(request);

        // Then
        assertThat(result)
                .isFailure()
                .hasErrorMessageContaining("Invalid or expired reset token");

        // Verify no save or event
        verify(userRepository, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void resetPassword_WithExpiredToken_ShouldReturnFailure() {
        // Given
        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .token("expired-token")
                .newPassword("newpassword123")
                .build();

        when(passwordResetTokenService.validateResetToken("expired-token"))
                .thenReturn(Result.failure("Reset token has expired"));

        // When
        Result<Void> result = authenticationService.resetPassword(request);

        // Then
        assertThat(result)
                .isFailure()
                .hasErrorMessageContaining("expired");

        // Verify no save or event
        verify(userRepository, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }
}
