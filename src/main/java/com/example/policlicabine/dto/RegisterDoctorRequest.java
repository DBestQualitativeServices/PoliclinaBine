package com.example.policlicabine.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class RegisterDoctorRequest {

    // Credentials
    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    // Doctor profile data
    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotEmpty(message = "At least one specialty is required")
    private List<String> specialties;

    private String licenseNumber;

    @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Invalid phone format")
    private String phone;
}
