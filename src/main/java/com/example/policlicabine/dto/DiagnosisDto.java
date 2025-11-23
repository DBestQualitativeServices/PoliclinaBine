package com.example.policlicabine.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "ICD-10 diagnosis code and description")
public class DiagnosisDto {

    private UUID diagnosisId;
    private String icd10Code;
    private String icd10Description;
}
