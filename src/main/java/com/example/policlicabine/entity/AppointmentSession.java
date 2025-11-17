package com.example.policlicabine.entity;

import com.example.policlicabine.entity.enums.SessionStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Entity
@Table(name = "appointment_sessions")
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
        name = "session_consultations",
        joinColumns = @JoinColumn(name = "session_id"),
        inverseJoinColumns = @JoinColumn(name = "consultation_id")
    )
    @BatchSize(size = 10)
    private List<Consultation> consultations;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "session_diagnoses",
        joinColumns = @JoinColumn(name = "session_id"),
        inverseJoinColumns = @JoinColumn(name = "diagnosis_id")
    )
    @BatchSize(size = 10)
    private List<Diagnosis> diagnoses;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @BatchSize(size = 20)
    private List<Answer> answers;

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
    }

    public boolean requiresSurgeryRoom() {
        return consultations != null && consultations.stream()
            .anyMatch(Consultation::getRequiresSurgeryRoom);
    }

    public boolean isCompleted() {
        return status == SessionStatus.COMPLETED;
    }

    public boolean isCancelled() {
        return status == SessionStatus.CANCELLED || status == SessionStatus.NO_SHOW;
    }

    public boolean hasBeenRescheduled() {
        return rescheduleCount > 0;
    }

    public boolean hasMedicalData() {
        return (diagnoses != null && !diagnoses.isEmpty()) ||
               freeTextDiagnosis != null ||
               treatmentInstructions != null ||
               freeTextObservations != null;
    }

    public BigDecimal getSubtotalAmount() {
        if (consultations == null || consultations.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return consultations.stream()
            .map(consultation -> consultation.getPrice() != null ? consultation.getPrice() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public List<Question> getAllQuestions() {
        if (consultations == null || consultations.isEmpty()) {
            return new ArrayList<>();
        }
        return consultations.stream()
            .flatMap(consultation -> consultation.getQuestions().stream())
            .collect(Collectors.toList());
    }

    public List<Answer> getAnswersForConsultation(Consultation consultation) {
        if (answers == null || answers.isEmpty()) {
            return new ArrayList<>();
        }
        return answers.stream()
            .filter(answer -> consultation.equals(answer.getConsultation()))
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
