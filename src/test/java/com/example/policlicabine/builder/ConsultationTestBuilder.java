package com.example.policlicabine.builder;

import com.example.policlicabine.entity.Consultation;
import com.example.policlicabine.entity.enums.Specialty;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.UUID;

/**
 * Test data builder for Consultation entity using the Builder pattern.
 * <p>
 * Provides default consultation types for common medical services.
 * </p>
 * <p>Example usage:</p>
 * <pre>
 * Consultation generalConsult = ConsultationTestBuilder.generalConsultation().build();
 * Consultation cardiology = ConsultationTestBuilder.aConsultation()
 *     .withSpecialty(Specialty.CARDIOLOGY)
 *     .withPrice(new BigDecimal("300.00"))
 *     .build();
 * </pre>
 */
public class ConsultationTestBuilder {

    private UUID consultationId = UUID.randomUUID();
    private String name = "General Consultation";
    private Specialty specialty = Specialty.GENERAL_DERMATOLOGY;
    private BigDecimal price = new BigDecimal("150.00");
    private String priceCurrency = "RON";
    private Integer durationMinutes = 30;
    private Boolean requiresSurgeryRoom = false;
    private Boolean isActive = true;
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public static ConsultationTestBuilder aConsultation() {
        return new ConsultationTestBuilder();
    }

    public static ConsultationTestBuilder generalConsultation() {
        return new ConsultationTestBuilder()
                .withName("General Consultation")
                .withSpecialty(Specialty.GENERAL_DERMATOLOGY)
                .withPrice(new BigDecimal("150.00"))
                .withDurationMinutes(30);
    }

    public static ConsultationTestBuilder cardiologyConsultation() {
        return new ConsultationTestBuilder()
                .withName("Cosmetic Dermatology Consultation")
                .withSpecialty(Specialty.COSMETIC_DERMATOLOGY)
                .withPrice(new BigDecimal("250.00"))
                .withDurationMinutes(45);
    }

    public static ConsultationTestBuilder dermatologyConsultation() {
        return new ConsultationTestBuilder()
                .withName("Medical Dermatology Consultation")
                .withSpecialty(Specialty.MEDICAL_DERMATOLOGY)
                .withPrice(new BigDecimal("200.00"))
                .withDurationMinutes(30);
    }

    public static ConsultationTestBuilder surgeryConsultation() {
        return new ConsultationTestBuilder()
                .withName("Mole Removal Consultation")
                .withSpecialty(Specialty.MOLES)
                .withPrice(new BigDecimal("400.00"))
                .withDurationMinutes(60)
                .withRequiresSurgeryRoom(true);
    }

    public ConsultationTestBuilder withConsultationId(UUID consultationId) {
        this.consultationId = consultationId;
        return this;
    }

    public ConsultationTestBuilder withName(String name) {
        this.name = name;
        return this;
    }

    public ConsultationTestBuilder withSpecialty(Specialty specialty) {
        this.specialty = specialty;
        return this;
    }

    public ConsultationTestBuilder withPrice(BigDecimal price) {
        this.price = price;
        return this;
    }

    public ConsultationTestBuilder withPriceCurrency(String priceCurrency) {
        this.priceCurrency = priceCurrency;
        return this;
    }

    public ConsultationTestBuilder withDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
        return this;
    }

    public ConsultationTestBuilder withRequiresSurgeryRoom(Boolean requiresSurgeryRoom) {
        this.requiresSurgeryRoom = requiresSurgeryRoom;
        return this;
    }

    public ConsultationTestBuilder withIsActive(Boolean isActive) {
        this.isActive = isActive;
        return this;
    }

    public ConsultationTestBuilder withCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public ConsultationTestBuilder asInactive() {
        this.isActive = false;
        return this;
    }

    public Consultation build() {
        return Consultation.builder()
                .consultationId(consultationId)
                .name(name)
                .specialty(specialty)
                .price(price)
                .priceCurrency(priceCurrency)
                .durationMinutes(durationMinutes)
                .requiresSurgeryRoom(requiresSurgeryRoom)
                .isActive(isActive)
                .questions(new ArrayList<>())
                .sessions(new ArrayList<>())
                .answers(new ArrayList<>())
                .createdAt(createdAt)
                .build();
    }
}
