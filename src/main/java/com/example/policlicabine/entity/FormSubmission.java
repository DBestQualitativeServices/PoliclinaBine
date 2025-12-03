package com.example.policlicabine.entity;

import com.example.policlicabine.model.FormStructure;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submitted_by_user_id")
    private User submittedBy;

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "patient_signed_at")
    private LocalDateTime patientSignedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_signed_by_user_id")
    private User patientSignedBy;

    @Column(name = "doctor_signed_at")
    private LocalDateTime doctorSignedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_signed_by_user_id")
    private User doctorSignedBy;

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

    public void signByPatient(User witnessedBy) {
        this.patientSignedAt = LocalDateTime.now();
        this.patientSignedBy = witnessedBy;
        this.updatedAt = LocalDateTime.now();
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
