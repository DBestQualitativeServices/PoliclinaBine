package com.example.policlicabine.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "patients", indexes = {
    @Index(name = "idx_patient_phone", columnList = "phone"),
    @Index(name = "idx_patient_email", columnList = "email")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Patient {

    @Id
    @Column(columnDefinition = "UUID")
    private UUID patientId;

    @Column(nullable = false, length = 100)
    private String firstName;

    @Column(nullable = false, length = 100)
    private String lastName;

    @Column(length = 20)
    private String phone;

    @Column(length = 100, unique = true)
    private String email;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(length = 500)
    private String consentFileUrl;

    @OneToMany(mappedBy = "patient", fetch = FetchType.LAZY)
    @BatchSize(size = 20)
    private List<AppointmentSession> appointments = new ArrayList<>();

    @CreationTimestamp
    @Column(updatable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime registrationDate;

    @PrePersist
    void generateId() {
        if (patientId == null) {
            patientId = UUID.randomUUID();
        }
    }

    /**
     * Checks if patient has signed consent file.
     */
    public boolean hasConsentSigned() {
        return consentFileUrl != null && !consentFileUrl.trim().isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Patient)) return false;
        Patient patient = (Patient) o;
        return patientId != null && Objects.equals(patientId, patient.patientId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Patient{" +
                "patientId=" + patientId +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", phone='" + phone + '\'' +
                '}';
    }
}
