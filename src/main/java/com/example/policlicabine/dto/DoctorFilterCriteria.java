package com.example.policlicabine.dto;

import com.example.policlicabine.entity.enums.Specialty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoctorFilterCriteria {

    private String fullName;

    private Specialty specialty;

    private Boolean enabled;

    private OffsetDateTime createdAfter;

    private OffsetDateTime createdBefore;
}
