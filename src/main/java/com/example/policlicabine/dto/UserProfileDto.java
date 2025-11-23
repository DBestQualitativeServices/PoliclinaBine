package com.example.policlicabine.dto;

import com.example.policlicabine.entity.enums.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;
import java.util.UUID;

/**
 * DTO for user profile information returned by /api/users/me endpoint.
 * Follows OAuth2/OIDC UserInfo pattern - separate from authentication response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "User profile with role-specific information")
public class UserProfileDto {

    private UUID userId;
    private String username;
    private Set<UserRole> roles;

    // Profile data - only one will be populated based on user type
    private DoctorDto doctorProfile;
    private PatientDto patientProfile;
    private ManagerDto managerProfile;

    // Helper method to get profile type
    public String getProfileType() {
        if (doctorProfile != null) return "DOCTOR";
        if (patientProfile != null) return "PATIENT";
        if (managerProfile != null) return "MANAGER";
        return "NONE";
    }

    // Helper method to get full name from any profile
    public String getFullName() {
        if (doctorProfile != null) return doctorProfile.getFullName();
        if (patientProfile != null) return patientProfile.getFirstName()+" " + patientProfile.getLastName();
        if (managerProfile != null) return managerProfile.getFullName();
        return username;
    }
}
