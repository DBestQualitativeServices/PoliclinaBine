package com.example.policlicabine.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientFilterCriteria {

    private String firstName;

    private String lastName;

    private String fullName;

    private String phone;

    private String email;

    private OffsetDateTime registeredAfter;

    private OffsetDateTime registeredBefore;
}
