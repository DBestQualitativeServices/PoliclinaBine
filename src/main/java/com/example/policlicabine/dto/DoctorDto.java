package com.example.policlicabine.dto;

import com.example.policlicabine.entity.enums.Specialty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Doctor profile with specialties and availability")
public class DoctorDto {

    private UUID doctorId;
    private UUID userId;
    private String fullName;
    private List<Specialty> specialties;
    private List<WeeklyAvailabilityDto> weeklyAvailability;
}
