package com.example.policlicabine.controller;

import com.example.policlicabine.common.Result;
import com.example.policlicabine.common.StandardApiResponses;
import com.example.policlicabine.dto.*;
import com.example.policlicabine.exception.BusinessException;
import com.example.policlicabine.exception.UnauthorizedException;
import com.example.policlicabine.service.AuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "User authentication and authorization endpoints")
public class AuthController {

    private final AuthenticationService authenticationService;

    @PostMapping("/register")
    @StandardApiResponses
    @Operation(summary = "Register a new user", description = "DEPRECATED: Use persona-specific endpoints instead (/register-patient, /register-doctor, /register-manager)")
    @ResponseStatus(HttpStatus.CREATED)
    @Deprecated
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        log.info("Registration request for username: {}", request.getUsername());

        Result<AuthResponse> result = authenticationService.register(request);

        if (result.isFailure()) {
            throw new BusinessException(result.getErrorMessage());
        }

        return result.getValue();
    }

    @PostMapping("/register-patient")
    @StandardApiResponses
    @Operation(summary = "Register a new patient with user account")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponseWrapper<PatientDto> registerPatient(@Valid @RequestBody RegisterPatientRequest request) {
        log.info("Patient registration request for username: {}", request.getUsername());

        Result<AuthResponseWrapper<PatientDto>> result = authenticationService.registerPatient(request);

        if (result.isFailure()) {
            throw new BusinessException(result.getErrorMessage());
        }

        return result.getValue();
    }

    @PostMapping("/register-doctor")
    @StandardApiResponses
    @Operation(summary = "Register a new doctor with user account")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponseWrapper<DoctorDto> registerDoctor(@Valid @RequestBody RegisterDoctorRequest request) {
        log.info("Doctor registration request for username: {}", request.getUsername());

        Result<AuthResponseWrapper<DoctorDto>> result = authenticationService.registerDoctor(request);

        if (result.isFailure()) {
            throw new BusinessException(result.getErrorMessage());
        }

        return result.getValue();
    }

    @PostMapping("/register-manager")
    @StandardApiResponses
    @SecurityRequirement(name = "bearer-jwt")
    @Operation(summary = "Register a new manager with user account (admin only)")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponseWrapper<ManagerDto> registerManager(@Valid @RequestBody RegisterManagerRequest request) {
        log.info("Manager registration request for username: {}", request.getUsername());

        Result<AuthResponseWrapper<ManagerDto>> result = authenticationService.registerManager(request);

        if (result.isFailure()) {
            throw new BusinessException(result.getErrorMessage());
        }

        return result.getValue();
    }

    @PostMapping("/login")
    @StandardApiResponses
    @Operation(summary = "Login")
    public AuthResponse login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest
    ) {
        log.info("Login request for username: {}", request.getUsername());

        String ipAddress = getClientIp(httpRequest);
        Result<AuthResponse> result = authenticationService.authenticate(request, ipAddress);

        if (result.isFailure()) {
            throw new UnauthorizedException(result.getErrorMessage());
        }

        return result.getValue();
    }

    @PostMapping("/refresh-token")
    @StandardApiResponses
    @Operation(summary = "Refresh access token")
    public AuthResponse refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        log.info("Token refresh request");

        Result<AuthResponse> result = authenticationService.refreshToken(request);

        if (result.isFailure()) {
            throw new UnauthorizedException(result.getErrorMessage());
        }

        return result.getValue();
    }

    @PostMapping("/change-password")
    @StandardApiResponses
    @SecurityRequirement(name = "bearer-jwt")
    @Operation(summary = "Change password")
    public MessageResponse changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        log.info("Password change request");

        Result<Void> result = authenticationService.changePassword(request);

        if (result.isFailure()) {
            throw new BusinessException(result.getErrorMessage());
        }

        return new MessageResponse("Password changed successfully");
    }

    @PostMapping("/forgot-password")
    @StandardApiResponses
    @Operation(summary = "Initiate password reset")
    public MessageResponse forgotPassword(@Valid @RequestBody InitiatePasswordResetRequest request) {
        log.info("Password reset request for username: {}", request.getUsername());

        Result<String> result = authenticationService.initiatePasswordReset(request);

        if (result.isFailure()) {
            throw new BusinessException(result.getErrorMessage());
        }

        return new MessageResponse(result.getValue());
    }

    @PostMapping("/reset-password")
    @StandardApiResponses
    @Operation(summary = "Reset password")
    public MessageResponse resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        log.info("Password reset completion request");

        Result<Void> result = authenticationService.resetPassword(request);

        if (result.isFailure()) {
            throw new BusinessException(result.getErrorMessage());
        }

        return new MessageResponse("Password reset successfully");
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

    public record MessageResponse(String message) {}
}
