package com.example.policlicabine.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Stores individual signatures for form submissions.
 * Each signature links to a specific signature field in the form template.
 */
@Entity
@Table(name = "form_signatures", indexes = {
        @Index(name = "idx_signature_submission", columnList = "form_submission_id"),
        @Index(name = "idx_signature_field", columnList = "form_submission_id, signature_field_id"),
        @Index(name = "idx_signature_user", columnList = "signed_by_user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FormSignature {

    @Id
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "UUID")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "form_submission_id", nullable = false)
    private FormSubmission formSubmission;

    /**
     * Which signature field in the template this signature is for.
     * Matches FormField.name where type="signature".
     */
    @Column(name = "signature_field_id", nullable = false)
    private String signatureFieldId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "signed_by_user_id", nullable = false)
    private User signedBy;

    /**
     * Binary PNG data of the drawn signature from canvas.
     * Stored as BYTEA in PostgreSQL for optimal storage efficiency.
     */
    @Column(name = "signature_data", columnDefinition = "BYTEA", nullable = false)
    private byte[] signatureData;

    @Column(name = "signed_at", nullable = false)
    private LocalDateTime signedAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) id = UUID.randomUUID();
        if (signedAt == null) signedAt = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FormSignature)) return false;
        FormSignature that = (FormSignature) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "FormSignature{" +
                "id=" + id +
                ", signatureFieldId='" + signatureFieldId + '\'' +
                ", signedAt=" + signedAt +
                '}';
    }
}
