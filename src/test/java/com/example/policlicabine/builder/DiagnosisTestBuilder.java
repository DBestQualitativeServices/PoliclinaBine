package com.example.policlicabine.builder;

import com.example.policlicabine.entity.Diagnosis;

import java.util.ArrayList;
import java.util.UUID;

/**
 * Test data builder for Diagnosis entity using the Builder pattern.
 * <p>
 * Provides common ICD-10 diagnosis codes for testing.
 * </p>
 * <p>Example usage:</p>
 * <pre>
 * Diagnosis hypertension = DiagnosisTestBuilder.hypertension().build();
 * Diagnosis custom = DiagnosisTestBuilder.aDiagnosis()
 *     .withIcd10Code("J00")
 *     .withDescription("Acute nasopharyngitis (common cold)")
 *     .build();
 * </pre>
 */
public class DiagnosisTestBuilder {

    private UUID diagnosisId = UUID.randomUUID();
    private String icd10Code = "Z00.0";
    private String description = "General medical examination";

    public static DiagnosisTestBuilder aDiagnosis() {
        return new DiagnosisTestBuilder();
    }

    public static DiagnosisTestBuilder hypertension() {
        return new DiagnosisTestBuilder()
                .withIcd10Code("I10")
                .withDescription("Essential (primary) hypertension");
    }

    public static DiagnosisTestBuilder diabetes() {
        return new DiagnosisTestBuilder()
                .withIcd10Code("E11")
                .withDescription("Type 2 diabetes mellitus");
    }

    public static DiagnosisTestBuilder acuteUpperRespiratoryInfection() {
        return new DiagnosisTestBuilder()
                .withIcd10Code("J06.9")
                .withDescription("Acute upper respiratory infection, unspecified");
    }

    public static DiagnosisTestBuilder headache() {
        return new DiagnosisTestBuilder()
                .withIcd10Code("R51")
                .withDescription("Headache");
    }

    public DiagnosisTestBuilder withDiagnosisId(UUID diagnosisId) {
        this.diagnosisId = diagnosisId;
        return this;
    }

    public DiagnosisTestBuilder withIcd10Code(String icd10Code) {
        this.icd10Code = icd10Code;
        return this;
    }

    public DiagnosisTestBuilder withDescription(String description) {
        this.description = description;
        return this;
    }

    public Diagnosis build() {
        return Diagnosis.builder()
                .diagnosisId(diagnosisId)
                .icd10Code(icd10Code)
                .icd10Description(description)
                .sessions(new ArrayList<>())
                .build();
    }
}
