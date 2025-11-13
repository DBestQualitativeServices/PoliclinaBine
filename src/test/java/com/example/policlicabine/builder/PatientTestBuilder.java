package com.example.policlicabine.builder;

import com.example.policlicabine.entity.Patient;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Test data builder for Patient entity using the Builder pattern.
 * <p>
 * Provides realistic default test data with unique identifiers.
 * </p>
 * <p>Example usage:</p>
 * <pre>
 * Patient patient = PatientTestBuilder.aPatient()
 *     .withFirstName("John")
 *     .withLastName("Doe")
 *     .withEmail("john.doe@example.com")
 *     .build();
 * </pre>
 */
public class PatientTestBuilder {

    private static final AtomicInteger counter = new AtomicInteger(0);

    private UUID patientId = UUID.randomUUID();
    private String firstName = "John";
    private String lastName = "Doe";
    private String phone = "0700" + String.format("%06d", counter.incrementAndGet());
    private String email = "patient" + counter.get() + "@test.com";
    private String address = "123 Test Street, Test City";
    private String consentFileUrl = null;
    private LocalDateTime registrationDate = LocalDateTime.now();

    public static PatientTestBuilder aPatient() {
        return new PatientTestBuilder();
    }

    public static PatientTestBuilder aPatientWithConsent() {
        return new PatientTestBuilder()
                .withConsentFileUrl("https://storage.example.com/consents/test-consent.pdf");
    }

    public PatientTestBuilder withPatientId(UUID patientId) {
        this.patientId = patientId;
        return this;
    }

    public PatientTestBuilder withFirstName(String firstName) {
        this.firstName = firstName;
        return this;
    }

    public PatientTestBuilder withLastName(String lastName) {
        this.lastName = lastName;
        return this;
    }

    public PatientTestBuilder withPhone(String phone) {
        this.phone = phone;
        return this;
    }

    public PatientTestBuilder withEmail(String email) {
        this.email = email;
        return this;
    }

    public PatientTestBuilder withAddress(String address) {
        this.address = address;
        return this;
    }

    public PatientTestBuilder withConsentFileUrl(String consentFileUrl) {
        this.consentFileUrl = consentFileUrl;
        return this;
    }

    public PatientTestBuilder withRegistrationDate(LocalDateTime registrationDate) {
        this.registrationDate = registrationDate;
        return this;
    }

    public Patient build() {
        return Patient.builder()
                .patientId(patientId)
                .firstName(firstName)
                .lastName(lastName)
                .phone(phone)
                .email(email)
                .address(address)
                .consentFileUrl(consentFileUrl)
                .appointments(new ArrayList<>())
                .registrationDate(registrationDate)
                .build();
    }
}
