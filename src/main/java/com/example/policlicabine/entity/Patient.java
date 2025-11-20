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

//    /**
//     * All forms associated with this patient (consent forms, medical history forms, etc.)
//     *
//     * <p>Replaces the old consentFile OneToOne relationship with a more flexible
//     * OneToMany relationship to Form entities. Patient can have multiple forms:
//     * <ul>
//     *   <li>General consent form (with 1-year validity)</li>
//     *   <li>Anesthesia consent forms (per procedure)</li>
//     *   <li>Surgery authorization forms (per procedure)</li>
//     *   <li>Treatment plan agreements</li>
//     * </ul>
//     */
//    @OneToMany(mappedBy = "patient", fetch = FetchType.LAZY)
//    @BatchSize(size = 20)
//    @Builder.Default
//    private List<Form> forms = new ArrayList<>();
//
//    /**
//     * Patient medical dossier - all files associated with this patient
//     *
//     * <p>Note: Files can be associated with patient in two ways:
//     * <ul>
//     *   <li><strong>Via Form:</strong> File belongs to a Form (e.g., signed consent PDF)</li>
//     *   <li><strong>Direct:</strong> Standalone file (e.g., patient-uploaded medical history from another hospital)</li>
//     * </ul>
//     */
//    @OneToMany(mappedBy = "patient", fetch = FetchType.LAZY)
//    @BatchSize(size = 20)
//    @Builder.Default
//    private List<File> files = new ArrayList<>();

    @OneToMany(mappedBy = "patient", fetch = FetchType.LAZY)
    @BatchSize(size = 20)
    @Builder.Default
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

//    /**
//     * Checks if patient has a valid signed consent form.
//     *
//     * <p>A valid consent form must meet ALL criteria:
//     * <ul>
//     *   <li>Form type is a consent form (CONSENT, ANESTHESIA_CONSENT, or SURGERY_CONSENT)</li>
//     *   <li>Form is signed (status = SIGNED, patientSignedAt is set)</li>
//     *   <li>Form has not expired (validUntil is null or in the future)</li>
//     *   <li>Form is not soft-deleted</li>
//     * </ul>
//     *
//     * @return true if patient has at least one valid signed consent form
//     */
//    public boolean hasConsentSigned() {
//        return forms.stream()
//                .anyMatch(form -> form.isConsentForm()
//                        && form.isSigned()
//                        && !form.isExpired()
//                        && !form.getIsDeleted());
//    }

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
