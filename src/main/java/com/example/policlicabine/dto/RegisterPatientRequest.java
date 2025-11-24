package com.example.policlicabine.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RegisterPatientRequest {

    // Credentials (OPTIONAL - auto-generated if not provided)
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String username;

    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    // Patient profile data
    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @NotBlank(message = "Phone is required")
    @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Invalid phone format")
    private String phone;

    @Email(message = "Invalid email format")
    private String email;

    @Pattern(regexp = "^[0-9]{13}$", message = "CNP must be 13 digits")
    private String cnp;

    private String address;
}
