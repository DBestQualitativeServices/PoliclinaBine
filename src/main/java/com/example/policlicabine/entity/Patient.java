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
    @Index(name = "idx_patient_user", columnList = "user_id"),
    @Index(name = "idx_patient_tutor", columnList = "tutor_patient_id")
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

    @Column(length = 2)
    private String ciSerie;

    @Column(length = 7)
    private String ciNumber;

    @Column(length = 200)
    private String ciEliberatDe;

    @Column
    private String sursa;

    @Column
    private Boolean minor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tutor_patient_id")
    private Patient tutor;

    @Column
    private String sex;

    @Column(length = 13)
    private String cnp;

    @Column(columnDefinition = "DATE")
    private LocalDate ciDataEliberare;

    @Column(columnDefinition = "DATE")
    private LocalDate dataNastere;

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
        if (sursa != null && !sursa.trim().isEmpty()) {
            formFieldCache.put("sursa", sursa.trim());
        }
        if (minor != null) {
            formFieldCache.put("minor", minor.toString());
        }
        if (sex != null && !sex.trim().isEmpty()) {
            formFieldCache.put("sex", sex.trim());
        }
        if (cnp != null && !cnp.trim().isEmpty()) {
            formFieldCache.put("cnp", cnp.trim());
        }
        if (dataNastere != null) {
            formFieldCache.put("datanastere", dataNastere.toString());
        }
    }

    /**
     * Calculates sex from CNP first digit.
     * @param cnp Romanian CNP (13 digits)
     * @return "M" for male, "F" for female, null if invalid
     */
    public static String calculateSexFromCnp(String cnp) {
        if (cnp == null || cnp.length() != 13 || !cnp.matches("^[0-9]{13}$")) {
            return null;
        }
        char firstDigit = cnp.charAt(0);
        if (firstDigit == '1' || firstDigit == '5') {
            return "M";
        } else if (firstDigit == '2' || firstDigit == '6') {
            return "F";
        }
        return null;
    }

    /**
     * Calculates minor status from CNP.
     * Logic: 1/2 = adult (born 1900-1999), 5/6 = check year (born 2000+)
     * @param cnp Romanian CNP (13 digits)
     * @return true if minor, false if adult, null if invalid
     */
    public static Boolean calculateMinorFromCnp(String cnp) {
        if (cnp == null || cnp.length() != 13 || !cnp.matches("^[0-9]{13}$")) {
            return null;
        }
        char firstDigit = cnp.charAt(0);

        // 1 or 2 = born 1900-1999, definitely adult
        if (firstDigit == '1' || firstDigit == '2') {
            return false;
        }

        // 5 or 6 = born 2000+, check year
        if (firstDigit == '5' || firstDigit == '6') {
            String yearStr = cnp.substring(1, 3);
            int year = Integer.parseInt(yearStr);
            return year > 7; // 00-09 (2000-2009) = minor, 10+ = adult
        }

        return null;
    }

    /**
     * Calculates birth date from CNP.
     * CNP format: SAALLZZJJNNNC where:
     *   S = sex indicator (1-9)
     *   AA = year (last 2 digits)
     *   LL = month (01-12)
     *   ZZ = day (01-31)
     *
     * Century determination:
     *   1/2 = 1900-1999
     *   3/4 = 1800-1899
     *   5/6 = 2000-2099
     *   7/8 = 2000-2099 (foreign residents)
     *   9 = foreign residents (smart detection: year > 24 → 1900s, else 2000s)
     */
    public static LocalDate calculateBirthDateFromCnp(String cnp) {
        if (cnp == null || cnp.length() != 13 || !cnp.matches("^[0-9]{13}$")) {
            return null;
        }

        try {
            char firstDigit = cnp.charAt(0);
            int year = Integer.parseInt(cnp.substring(1, 3));
            int month = Integer.parseInt(cnp.substring(3, 5));
            int day = Integer.parseInt(cnp.substring(5, 7));

            if (month < 1 || month > 12 || day < 1 || day > 31) {
                return null;
            }

            int fullYear;
            switch (firstDigit) {
                case '1', '2' -> fullYear = 1900 + year;
                case '3', '4' -> fullYear = 1800 + year;
                case '5', '6', '7', '8' -> fullYear = 2000 + year;
                case '9' -> fullYear = (year > 24) ? (1900 + year) : (2000 + year);
                default -> { return null; }
            }

            return LocalDate.of(fullYear, month, day);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Updates sex, minor, and birth date fields based on CNP.
     * Call this after setting/updating CNP.
     */
    public void calculateFieldsFromCnp() {
        if (this.cnp != null && !this.cnp.trim().isEmpty()) {
            String calculatedSex = calculateSexFromCnp(this.cnp);
            Boolean calculatedMinor = calculateMinorFromCnp(this.cnp);
            LocalDate calculatedBirthDate = calculateBirthDateFromCnp(this.cnp);

            if (calculatedSex != null) {
                this.sex = calculatedSex;
            }
            if (calculatedMinor != null) {
                this.minor = calculatedMinor;
            }
            if (calculatedBirthDate != null) {
                this.dataNastere = calculatedBirthDate;
            }

            // Sync to cache after calculation
            syncEntityFieldsToCache();
        }
    }

    public boolean requiresTutor() {
        return Boolean.TRUE.equals(this.minor);
    }

    public boolean hasTutor() {
        return this.tutor != null;
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
                ", sex='" + sex + '\'' +
                ", minor=" + minor +
                ", dataNastere=" + dataNastere +
                ", cnp='" + (cnp != null && cnp.length() >= 3 ? cnp.substring(0, 3) + "**********" : null) + '\'' +
                '}';
    }

}
