package com.example.policlicabine.service;

import com.example.policlicabine.common.Result;
import com.example.policlicabine.dto.*;
import com.example.policlicabine.entity.PasswordResetToken;
import com.example.policlicabine.entity.User;
import com.example.policlicabine.event.*;
import com.example.policlicabine.repository.UserRepository;
import com.example.policlicabine.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordResetTokenService resetTokenService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Register a new user
     */
    @Transactional
    public Result<AuthResponse> register(RegisterRequest request) {
        try {
            // Check if username already exists
            if (userRepository.existsByUsername(request.getUsername())) {
                log.warn("Registration failed: Username already exists: {}", request.getUsername());
                return Result.failure("Username already exists");
            }

            // Create user
            User user = User.builder()
                    .username(request.getUsername())
                    .password(passwordEncoder.encode(request.getPassword()))
                    .fullName(request.getFullName())
                    .role(request.getRole())
                    .enabled(true)
                    .accountNonLocked(true)
                    .build();

            User saved = userRepository.save(user);
            log.info("User registered successfully: {}", saved.getUsername());

            // Publish event
            eventPublisher.publishEvent(new UserRegistered(
                    saved.getUserId(),
                    saved.getUsername(),
                    saved.getFullName(),
                    saved.getRole()
            ));

            // Generate tokens
            UserDetails userDetails = userDetailsService.loadUserByUsername(saved.getUsername());
            String accessToken = jwtService.generateToken(userDetails);
            String refreshToken = jwtService.generateRefreshToken(userDetails);

            return Result.success(AuthResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .userId(saved.getUserId())
                    .username(saved.getUsername())
                    .fullName(saved.getFullName())
                    .role(saved.getRole())
                    .build());

        } catch (Exception e) {
            log.error("Registration error for user {}: {}", request.getUsername(), e.getMessage(), e);
            return Result.failure("Registration failed: " + e.getMessage());
        }
    }

    /**
     * Authenticate user and generate tokens
     */
    public Result<AuthResponse> authenticate(LoginRequest request, String ipAddress) {
        try {
            // Authenticate with Spring Security
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()
                    )
            );

            // Load user
            User user = userRepository.findByUsername(request.getUsername())
                    .orElse(null);

            if (user == null) {
                log.warn("Authentication failed: User not found: {}", request.getUsername());
                return Result.failure("Invalid credentials");
            }

            // Update last login
            user.setLastLogin(OffsetDateTime.now(ZoneOffset.UTC));
            userRepository.save(user);

            // Generate tokens
            UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
            String accessToken = jwtService.generateToken(userDetails);
            String refreshToken = jwtService.generateRefreshToken(userDetails);

            log.info("User authenticated successfully: {}", user.getUsername());

            // Publish event
            eventPublisher.publishEvent(new UserAuthenticated(
                    user.getUserId(),
                    user.getUsername(),
                    OffsetDateTime.now(ZoneOffset.UTC),
                    ipAddress != null ? ipAddress : "unknown"
            ));

            return Result.success(AuthResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .userId(user.getUserId())
                    .username(user.getUsername())
                    .fullName(user.getFullName())
                    .role(user.getRole())
                    .build());

        } catch (BadCredentialsException e) {
            log.warn("Authentication failed: Invalid credentials for user: {}", request.getUsername());
            return Result.failure("Invalid credentials");
        } catch (Exception e) {
            log.error("Authentication error for user {}: {}", request.getUsername(), e.getMessage(), e);
            return Result.failure("Authentication failed: " + e.getMessage());
        }
    }

    /**
     * Refresh access token using refresh token
     */
    @Transactional(readOnly = true)
    public Result<AuthResponse> refreshToken(RefreshTokenRequest request) {
        try {
            String refreshToken = request.getRefreshToken();

            // Extract username from refresh token
            String username = jwtService.extractUsername(refreshToken);

            if (username == null) {
                log.warn("Refresh token invalid: Cannot extract username");
                return Result.failure("Invalid refresh token");
            }

            // Load user details
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            // Validate refresh token
            if (!jwtService.isTokenValid(refreshToken, userDetails)) {
                log.warn("Refresh token invalid or expired for user: {}", username);
                return Result.failure("Invalid or expired refresh token");
            }

            // Load user
            User user = userRepository.findByUsername(username).orElse(null);
            if (user == null) {
                return Result.failure("User not found");
            }

            // Generate new access token
            String newAccessToken = jwtService.generateToken(userDetails);

            log.info("Access token refreshed for user: {}", username);

            return Result.success(AuthResponse.builder()
                    .accessToken(newAccessToken)
                    .refreshToken(refreshToken) // Return same refresh token
                    .tokenType("Bearer")
                    .userId(user.getUserId())
                    .username(user.getUsername())
                    .fullName(user.getFullName())
                    .role(user.getRole())
                    .build());

        } catch (Exception e) {
            log.error("Token refresh error: {}", e.getMessage(), e);
            return Result.failure("Failed to refresh token");
        }
    }

    /**
     * Change password for authenticated user
     */
    @Transactional
    public Result<Void> changePassword(ChangePasswordRequest request) {
        try {
            // Get current authenticated user
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return Result.failure("User not authenticated");
            }

            String username = authentication.getName();
            User user = userRepository.findByUsername(username).orElse(null);

            if (user == null) {
                return Result.failure("User not found");
            }

            // Verify current password
            if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
                log.warn("Password change failed: Invalid current password for user: {}", username);
                return Result.failure("Current password is incorrect");
            }

            // Update password
            user.setPassword(passwordEncoder.encode(request.getNewPassword()));
            userRepository.save(user);

            log.info("Password changed successfully for user: {}", username);

            // Publish event
            eventPublisher.publishEvent(new PasswordChanged(
                    user.getUserId(),
                    user.getUsername(),
                    OffsetDateTime.now(ZoneOffset.UTC)
            ));

            return Result.success(null);

        } catch (Exception e) {
            log.error("Password change error: {}", e.getMessage(), e);
            return Result.failure("Failed to change password");
        }
    }

    /**
     * Initiate password reset flow
     */
    @Transactional
    public Result<String> initiatePasswordReset(InitiatePasswordResetRequest request) {
        try {
            User user = userRepository.findByUsername(request.getUsername()).orElse(null);

            // Always return success even if user doesn't exist (security best practice)
            if (user == null) {
                log.warn("Password reset requested for non-existent user: {}", request.getUsername());
                return Result.success("If the username exists, a password reset link will be sent");
            }

            // Create reset token
            Result<PasswordResetToken> tokenResult = resetTokenService.createResetToken(user.getUserId());

            if (tokenResult.isFailure()) {
                log.error("Failed to create reset token for user: {}", user.getUsername());
                return Result.success("If the username exists, a password reset link will be sent");
            }

            PasswordResetToken resetToken = tokenResult.getValue();

            log.info("Password reset initiated for user: {}", user.getUsername());

            // Publish event
            eventPublisher.publishEvent(new PasswordResetInitiated(
                    user.getUserId(),
                    user.getUsername(),
                    resetToken.getToken(),
                    resetToken.getExpiryDate()
            ));

            // In production, this should be sent via email
            // For now, return the token (REMOVE THIS IN PRODUCTION!)
            return Result.success("Password reset token: " + resetToken.getToken());

        } catch (Exception e) {
            log.error("Password reset initiation error: {}", e.getMessage(), e);
            return Result.success("If the username exists, a password reset link will be sent");
        }
    }

    /**
     * Reset password using reset token
     */
    @Transactional
    public Result<Void> resetPassword(ResetPasswordRequest request) {
        try {
            // Validate token
            Result<PasswordResetToken> tokenResult = resetTokenService.validateResetToken(request.getToken());

            if (tokenResult.isFailure()) {
                return Result.failure(tokenResult.getErrorMessage());
            }

            PasswordResetToken resetToken = tokenResult.getValue();
            User user = resetToken.getUser();

            // Update password
            user.setPassword(passwordEncoder.encode(request.getNewPassword()));
            userRepository.save(user);

            // Mark token as used
            resetTokenService.markTokenAsUsed(request.getToken());

            log.info("Password reset successfully for user: {}", user.getUsername());

            // Publish event
            eventPublisher.publishEvent(new PasswordReset(
                    user.getUserId(),
                    user.getUsername(),
                    OffsetDateTime.now(ZoneOffset.UTC)
            ));

            return Result.success(null);

        } catch (Exception e) {
            log.error("Password reset error: {}", e.getMessage(), e);
            return Result.failure("Failed to reset password");
        }
    }
}
