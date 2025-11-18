package com.example.policlicabine.controller;

import com.example.policlicabine.common.Result;
import com.example.policlicabine.dto.*;
import com.example.policlicabine.service.AuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "User authentication and authorization endpoints")
public class AuthController {

    private final AuthenticationService authenticationService;

    @PostMapping("/register")
    @Operation(summary = "Register a new user", description = "Creates a new user account and returns JWT tokens")
    public ResponseEntity<?> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest
    ) {
        log.info("Registration request for username: {}", request.getUsername());

        Result<AuthResponse> result = authenticationService.register(request);

        if (result.isSuccess()) {
            return ResponseEntity.status(HttpStatus.CREATED).body(result.getValue());
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ErrorResponse.of(HttpStatus.BAD_REQUEST.value(), result.getErrorMessage(), httpRequest.getRequestURI()));
        }
    }

    @PostMapping("/login")
    @Operation(summary = "Login", description = "Authenticate user and return JWT tokens")
    public ResponseEntity<?> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest
    ) {
        log.info("Login request for username: {}", request.getUsername());

        String ipAddress = getClientIp(httpRequest);
        Result<AuthResponse> result = authenticationService.authenticate(request, ipAddress);

        if (result.isSuccess()) {
            return ResponseEntity.ok(result.getValue());
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ErrorResponse.of(HttpStatus.UNAUTHORIZED.value(), result.getErrorMessage(), httpRequest.getRequestURI()));
        }
    }

    @PostMapping("/refresh-token")
    @Operation(summary = "Refresh access token", description = "Generate a new access token using refresh token")
    public ResponseEntity<?> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request,
            HttpServletRequest httpRequest
    ) {
        log.info("Token refresh request");

        Result<AuthResponse> result = authenticationService.refreshToken(request);

        if (result.isSuccess()) {
            return ResponseEntity.ok(result.getValue());
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ErrorResponse.of(HttpStatus.UNAUTHORIZED.value(), result.getErrorMessage(), httpRequest.getRequestURI()));
        }
    }

    @PostMapping("/change-password")
    @SecurityRequirement(name = "bearer-jwt")
    @Operation(summary = "Change password", description = "Change password for authenticated user")
    public ResponseEntity<?> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            HttpServletRequest httpRequest
    ) {
        log.info("Password change request");

        Result<Void> result = authenticationService.changePassword(request);

        if (result.isSuccess()) {
            return ResponseEntity.ok(new MessageResponse("Password changed successfully"));
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ErrorResponse.of(HttpStatus.BAD_REQUEST.value(), result.getErrorMessage(), httpRequest.getRequestURI()));
        }
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Initiate password reset", description = "Request a password reset token")
    public ResponseEntity<?> forgotPassword(
            @Valid @RequestBody InitiatePasswordResetRequest request,
            HttpServletRequest httpRequest
    ) {
        log.info("Password reset request for username: {}", request.getUsername());

        Result<String> result = authenticationService.initiatePasswordReset(request);

        if (result.isSuccess()) {
            return ResponseEntity.ok(new MessageResponse(result.getValue()));
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ErrorResponse.of(HttpStatus.BAD_REQUEST.value(), result.getErrorMessage(), httpRequest.getRequestURI()));
        }
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password", description = "Reset password using reset token")
    public ResponseEntity<?> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request,
            HttpServletRequest httpRequest
    ) {
        log.info("Password reset completion request");

        Result<Void> result = authenticationService.resetPassword(request);

        if (result.isSuccess()) {
            return ResponseEntity.ok(new MessageResponse("Password reset successfully"));
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ErrorResponse.of(HttpStatus.BAD_REQUEST.value(), result.getErrorMessage(), httpRequest.getRequestURI()));
        }
    }

    /**
     * Extract client IP address from request
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * Simple response DTOs
     */
    private record MessageResponse(String message) {}
}
