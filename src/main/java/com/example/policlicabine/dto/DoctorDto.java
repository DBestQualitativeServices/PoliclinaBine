package com.example.policlicabine.dto;

import com.example.policlicabine.entity.enums.Specialty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Doctor profile with specialties and availability")
public class DoctorDto {

    private UUID doctorId;
    
    // Note: userId is used for INPUT (creating doctors) but NOT mapped from entity
    // to avoid N+1 queries from User's bidirectional OneToOne relationships
    private UUID userId;
    private String fullName;
    
    @Pattern(regexp = "^[0-9]{13}$", message = "CNP must be 13 digits")
    @Schema(
            description = "Personal Numeric Code (CNP) - Romanian national identification number (13 digits)",
            example = "1920515123456",
            pattern = "^[0-9]{13}$",
            maxLength = 13
    )
    private String cnp;

    @Schema(
            description = "Doctor's birth date (auto-calculated from CNP if available)",
            example = "1985-03-20",
            format = "date"
    )
    private LocalDate dataNastere;

    private List<Specialty> specialties;
    private List<WeeklyAvailabilityDto> weeklyAvailability;
}
