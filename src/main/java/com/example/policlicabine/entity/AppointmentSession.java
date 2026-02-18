package com.example.policlicabine.entity;

import com.example.policlicabine.entity.enums.SessionStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Entity
@Table(name = "appointment_sessions", indexes = {
    @Index(name = "idx_appointment_status", columnList = "status"),
    @Index(name = "idx_appointment_scheduled_date", columnList = "scheduledDateTime"),
    @Index(name = "idx_appointment_doctor_date", columnList = "doctor_id, scheduledDateTime"),
    @Index(name = "idx_appointment_patient_date", columnList = "patient_id, scheduledDateTime")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentSession {

    @Id
    private UUID sessionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @Column(nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime scheduledDateTime;

    @Builder.Default
    private Boolean isEmergency = false;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private SessionStatus status = SessionStatus.SCHEDULED;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "session_consultation_types",
        joinColumns = @JoinColumn(name = "session_id"),
        inverseJoinColumns = @JoinColumn(name = "consultation_type_id")
    )
    @BatchSize(size = 10)
    private Set<ConsultationType> consultationTypes = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "session_diagnoses",
        joinColumns = @JoinColumn(name = "session_id"),
        inverseJoinColumns = @JoinColumn(name = "diagnosis_id")
    )
    @BatchSize(size = 10)
    private Set<Diagnosis> diagnoses = new HashSet<>();

    @Column(columnDefinition = "TEXT")
    private String freeTextDiagnosis;

    @Column(columnDefinition = "TEXT")
    private String treatmentInstructions;

    @Column(columnDefinition = "TEXT")
    private String freeTextObservations;

    @Column(columnDefinition = "TEXT")
    private String cancellationReason;

    @Builder.Default
    private Integer contactAttempts = 0;

    @Builder.Default
    private Integer rescheduleCount = 0;

    /**
     * Cached total duration in minutes for all consultations in this session.
     * Calculated automatically on new appointments (@PrePersist) unless isCustomDuration is true.
     * Explicitly recalculated in service layer when consultations are added/removed.
     * When isCustomDuration is true, this field holds the customDurationMinutes value instead.
     * Used for efficient booking conflict detection queries.
     */
    @Builder.Default
    private Integer totalDurationMinutes = 0;

    /**
     * Whether a custom duration has been manually set for this session.
     * When true, totalDurationMinutes is NOT recalculated when consultations change.
     * Default false (auto-calculated from consultations).
     */
    @Builder.Default
    private Boolean isCustomDuration = false;

    /**
     * Custom duration in minutes, set manually by receptionist.
     * Only relevant when isCustomDuration = true.
     * Min value: 5 minutes.
     */
    private Integer customDurationMinutes;

    @CreationTimestamp
    @Column(updatable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime createdAt;

    @Column(columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime completedAt;

    @Column(columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime cancelledAt;

    @Column(columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime lastContactAttemptAt;

    @PrePersist
    void generateId() {
        if (sessionId == null) {
            sessionId = UUID.randomUUID();
        }
        // Only auto-calculate if not using custom duration
        if (!Boolean.TRUE.equals(isCustomDuration)) {
            calculateTotalDuration();
        }
    }

    /**
     * Calculates and sets totalDurationMinutes from consultationTypes.
     * Called by @PrePersist for new entities.
     * Service layer calls this explicitly when consultations change.
     */
    public void calculateTotalDuration() {
        if (consultationTypes == null || consultationTypes.isEmpty()) {
            this.totalDurationMinutes = 0;
            return;
        }
        this.totalDurationMinutes = consultationTypes.stream()
            .map(c -> c.getDurationMinutes() != null ? c.getDurationMinutes() : 0)
            .reduce(0, Integer::sum);
    }

    /**
     * Convenience method to get the end time of this appointment.
     * @return scheduledDateTime + totalDurationMinutes
     */
    public OffsetDateTime getEndTime() {
        if (scheduledDateTime == null) {
            return null;
        }
        return scheduledDateTime.plusMinutes(totalDurationMinutes != null ? totalDurationMinutes : 0);
    }

    public boolean isCompleted() {
        return status == SessionStatus.COMPLETED;
    }

    public boolean isCancelled() {
        return status == SessionStatus.CANCELLED || status == SessionStatus.NO_SHOW;
    }

    public BigDecimal getSubtotalAmount() {
        if (consultationTypes == null || consultationTypes.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return consultationTypes.stream()
            .map(consultationType -> consultationType.getPrice() != null ? consultationType.getPrice() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Returns list of consultation type names for this session.
     * Used to avoid duplicate code in services.
     * @return List of consultation type names
     */
    public List<String> getConsultationNames() {
        if (consultationTypes == null || consultationTypes.isEmpty()) {
            return new ArrayList<>();
        }
        return consultationTypes.stream()
            .map(ConsultationType::getName)
            .collect(Collectors.toList());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AppointmentSession)) return false;
        AppointmentSession that = (AppointmentSession) o;
        return sessionId != null && Objects.equals(sessionId, that.sessionId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "AppointmentSession{" +
                "sessionId=" + sessionId +
                ", scheduledDateTime=" + scheduledDateTime +
                ", status=" + status +
                ", isEmergency=" + isEmergency +
                '}';
    }
}
