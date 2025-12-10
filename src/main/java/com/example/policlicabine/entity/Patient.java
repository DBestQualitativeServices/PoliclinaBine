package com.example.policlicabine.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "patients", indexes = {
    @Index(name = "idx_patient_phone", columnList = "phone"),
    @Index(name = "idx_patient_email", columnList = "email"),
    @Index(name = "idx_patient_user", columnList = "user_id")
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

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true)
    private User user;

    @Column(nullable = false, length = 100)
    private String firstName;

    @Column(nullable = false, length = 100)
    private String lastName;

    @Column(length = 20)
    private String phone;

    @Column(length = 100)
    private String email;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(columnDefinition = "TEXT")
    private String domiciliu;

    @Column(length = 2)
    private String ciSerie;

    @Column(length = 7)
    private String ciNumber;

    @Column(length = 200)
    private String ciEliberatDe;

    @Column(columnDefinition = "DATE")
    private LocalDate ciDataEliberare;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "form_field_cache", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> formFieldCache = new HashMap<>();

    @OneToMany(mappedBy = "patient", fetch = FetchType.LAZY)
    @BatchSize(size = 20)
    @Builder.Default
    private List<AppointmentSession> appointments = new ArrayList<>();

    @OneToMany(mappedBy = "patient", fetch = FetchType.LAZY)
    @BatchSize(size = 20)
    @Builder.Default
    private List<FormSubmission> formSubmissions = new ArrayList<>();

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
     * Updates form field cache with new data (latest wins - overwrites existing keys).
     * @param newData the new field values to merge into cache
     */
    public void updateFormFieldCache(Map<String, Object> newData) {
        if (newData == null || newData.isEmpty()) {
            return;
        }
        if (this.formFieldCache == null) {
            this.formFieldCache = new HashMap<>();
        }
        // Convert keys to lowercase for case-insensitive matching
        for (Map.Entry<String, Object> entry : newData.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null) {
                this.formFieldCache.put(entry.getKey().toLowerCase(), entry.getValue());
            }
        }
    }

    /**
     * Syncs current entity fields to the form field cache.
     * Call this after creating or updating patient profile fields.
     */
    public void syncEntityFieldsToCache() {
        if (this.formFieldCache == null) {
            this.formFieldCache = new HashMap<>();
        }
        // Sync all entity fields to cache with lowercase keys
        if (firstName != null && !firstName.trim().isEmpty()) {
            formFieldCache.put("firstname", firstName.trim());
        }
        if (lastName != null && !lastName.trim().isEmpty()) {
            formFieldCache.put("lastname", lastName.trim());
        }
        if (phone != null && !phone.trim().isEmpty()) {
            formFieldCache.put("phone", phone.trim());
        }
        if (email != null && !email.trim().isEmpty()) {
            formFieldCache.put("email", email.trim());
        }
        if (address != null && !address.trim().isEmpty()) {
            formFieldCache.put("address", address.trim());
        }
        if (domiciliu != null && !domiciliu.trim().isEmpty()) {
            formFieldCache.put("domiciliu", domiciliu.trim());
        }
        if (ciSerie != null && !ciSerie.trim().isEmpty()) {
            formFieldCache.put("ciserie", ciSerie.trim());
        }
        if (ciNumber != null && !ciNumber.trim().isEmpty()) {
            formFieldCache.put("cinumber", ciNumber.trim());
        }
        if (ciEliberatDe != null && !ciEliberatDe.trim().isEmpty()) {
            formFieldCache.put("cieliberatde", ciEliberatDe.trim());
        }
        if (ciDataEliberare != null) {
            formFieldCache.put("cidataeliberare", ciDataEliberare.toString());
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Patient patient)) return false;
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
