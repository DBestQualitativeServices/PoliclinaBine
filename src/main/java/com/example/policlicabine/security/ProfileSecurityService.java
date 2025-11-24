package com.example.policlicabine.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component("profileSecurity")
@Slf4j
public class ProfileSecurityService {

    private UserPrincipal getCurrentPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            throw new AccessDeniedException("Not authenticated");
        }

        if (!(auth.getPrincipal() instanceof UserPrincipal)) {
            throw new AccessDeniedException("Invalid principal type");
        }

        return (UserPrincipal) auth.getPrincipal();
    }

    public boolean isOwnPatientData(UUID patientId) {
        try {
            UserPrincipal principal = getCurrentPrincipal();
            return principal.getProfileId("PATIENT")
                .map(id -> id.equals(patientId))
                .orElse(false);
        } catch (Exception e) {
            log.warn("Failed to check patient ownership", e);
            return false;
        }
    }

    public boolean isDoctorProfile(UUID doctorId) {
        try {
            UserPrincipal principal = getCurrentPrincipal();
            return principal.getProfileId("DOCTOR")
                .map(id -> id.equals(doctorId))
                .orElse(false);
        } catch (Exception e) {
            log.warn("Failed to check doctor profile", e);
            return false;
        }
    }

    public boolean canAccessPatientData(UUID patientId) {
        try {
            UserPrincipal principal = getCurrentPrincipal();

            if (isOwnPatientData(patientId)) {
                return true;
            }

            return principal.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));

        } catch (Exception e) {
            log.warn("Failed to check patient data access", e);
            return false;
        }
    }

    public UUID getCurrentUserId() {
        return getCurrentPrincipal().getUserId();
    }

    public Optional<UUID> getCurrentDoctorId() {
        try {
            return getCurrentPrincipal().getProfileId("DOCTOR");
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public Optional<UUID> getCurrentPatientId() {
        try {
            return getCurrentPrincipal().getProfileId("PATIENT");
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public Optional<UUID> getCurrentManagerId() {
        try {
            return getCurrentPrincipal().getProfileId("MANAGER");
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
