package com.example.policlicabine.entity;

import com.example.policlicabine.model.FormField;
import com.example.policlicabine.model.FormStructure;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Entity
@Table(name = "form_submissions", indexes = {
        @Index(name = "idx_submission_patient", columnList = "patient_id"),
        @Index(name = "idx_submission_session", columnList = "appointment_session_id"),
        @Index(name = "idx_submission_template", columnList = "template_id"),
        @Index(name = "idx_submission_expires", columnList = "expires_at"),
        // Composite index for common query pattern (validity check)
        @Index(name = "idx_submission_patient_validity",
               columnList = "patient_id, is_deleted, expires_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FormSubmission {
    @Id
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "UUID")
    private UUID id;

    @PrePersist
    private void generateId() {
        if (this.id == null) this.id = UUID.randomUUID();
        if (this.submittedAt == null) this.submittedAt = LocalDateTime.now();
        if (this.isDeleted == null) this.isDeleted = false;
        if (this.data == null) this.data = new HashMap<>();
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private FormTemplate template;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appointment_session_id")
    private AppointmentSession appointmentSession;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consultation_type_id")
    private ConsultationType consultationType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private FormStructure templateSnapshot;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    @Builder.Default
    private Map<String, Object> data = new HashMap<>();

    @OneToMany(mappedBy = "formSubmission", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @BatchSize(size = 20)
    @Builder.Default
    private List<File> attachedFiles = new ArrayList<>();

    /**
     * Collection of signatures for this form submission.
     * Replaces the hardcoded patientSignedAt/doctorSignedAt fields.
     */
    @OneToMany(mappedBy = "formSubmission", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @BatchSize(size = 20)
    @Builder.Default
    private Set<FormSignature> signatures = new LinkedHashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submitted_by_user_id")
    private User submittedBy;

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PreUpdate
    private void setUpdatedAt() {
        this.updatedAt = LocalDateTime.now();
    }

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deleted_by_user_id")
    private User deletedBy;

    public void softDelete(User deletedBy) {
        this.isDeleted = true;
        this.deletedAt = LocalDateTime.now();
        this.deletedBy = deletedBy;
    }

    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isValid() {
        return !isExpired() && !isDeleted;
    }

    public void attachFile(File file) {
        if (file != null) {
            file.setFormSubmission(this);
            if (!this.attachedFiles.contains(file)) {
                this.attachedFiles.add(file);
            }
        }
    }

    public void removeFile(File file) {
        if (file != null) {
            this.attachedFiles.remove(file);
            file.setFormSubmission(null);
        }
    }

    /**
     * Adds a signature to this form submission.
     */
    public void addSignature(FormSignature signature) {
        if (signature != null) {
            signature.setFormSubmission(this);
            if (!this.signatures.contains(signature)) {
                this.signatures.add(signature);
            }
        }
    }

    /**
     * Removes a signature from this form submission.
     */
    public void removeSignature(FormSignature signature) {
        if (signature != null) {
            this.signatures.remove(signature);
            signature.setFormSubmission(null);
        }
    }

    /**
     * Checks if a specific signature field has been signed.
     */
    public boolean hasSignatureForField(String signatureFieldId) {
        return signatures.stream()
                .anyMatch(s -> s.getSignatureFieldId().equals(signatureFieldId));
    }

    /**
     * Gets the set of signature field IDs that have been signed.
     */
    public Set<String> getSignedFieldIds() {
        return signatures.stream()
                .map(FormSignature::getSignatureFieldId)
                .collect(Collectors.toSet());
    }

    /**
     * Checks if all required signature fields have been signed.
     * Requires parsing the templateSnapshot to determine required fields.
     */
    public boolean isFullySigned() {
        if (templateSnapshot == null || templateSnapshot.getSections() == null) {
            return true; // No template means no required signatures
        }
        
        List<String> requiredFields = templateSnapshot.getSections().stream()
                .filter(section -> section.getFields() != null)
                .flatMap(section -> section.getFields().stream())
                .filter(field -> "signature".equals(field.getType()))
                .filter(field -> Boolean.TRUE.equals(field.getRequired()))
                .map(field -> field.getName())
                .toList();
        
        Set<String> signedFields = getSignedFieldIds();
        return signedFields.containsAll(requiredFields);
    }

    /**
     * Checks if the owner has signed this form submission.
     * The owner is determined by the template's ownerType (PATIENT, DOCTOR, ADMIN).
     * A form is considered "owner signed" when a signature exists for a signature field
     * whose signatureType matches the template's ownerType.
     *
     * @return true if owner's signature is present, false otherwise
     */
    public boolean isOwnerSigned() {
        if (template == null || template.getOwnerType() == null) {
            return true; // No owner type defined means no owner signature required
        }
        if (templateSnapshot == null || templateSnapshot.getSections() == null) {
            return true; // No template structure means no signatures to check
        }

        String ownerType = template.getOwnerType().name();
        Set<String> signedFieldIds = getSignedFieldIds();

        // Find signature fields whose signatureType matches the owner type
        // and check if any of them has been signed
        return templateSnapshot.getSections().stream()
                .filter(section -> section.getFields() != null)
                .flatMap(section -> section.getFields().stream())
                .filter(field -> "signature".equals(field.getType()))
                .filter(field -> ownerType.equals(field.getSignatureType()))
                .anyMatch(field -> signedFieldIds.contains(field.getName()));
    }

    /**
     * Gets the list of owner signature field names that are missing.
     * Useful for understanding what signatures are needed to complete the form.
     *
     * @return list of signature field names that the owner needs to sign
     */
    public List<String> getMissingOwnerSignatureFields() {
        if (template == null || template.getOwnerType() == null) {
            return List.of();
        }
        if (templateSnapshot == null || templateSnapshot.getSections() == null) {
            return List.of();
        }

        String ownerType = template.getOwnerType().name();
        Set<String> signedFieldIds = getSignedFieldIds();

        return templateSnapshot.getSections().stream()
                .filter(section -> section.getFields() != null)
                .flatMap(section -> section.getFields().stream())
                .filter(field -> "signature".equals(field.getType()))
                .filter(field -> ownerType.equals(field.getSignatureType()))
                .map(FormField::getName)
                .filter(fieldName -> !signedFieldIds.contains(fieldName))
                .toList();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FormSubmission)) return false;
        FormSubmission that = (FormSubmission) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "FormSubmission{" +
                "id=" + id +
                ", valid=" + isValid() +
                '}';
    }
}
