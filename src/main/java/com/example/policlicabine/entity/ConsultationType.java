package com.example.policlicabine.entity;

import com.example.policlicabine.entity.enums.Specialty;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "consultation_types", indexes = {
    @Index(name = "idx_consultation_type_specialty", columnList = "specialty"),
    @Index(name = "idx_consultation_type_active", columnList = "isActive")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsultationType {

    @Id
    @Column(columnDefinition = "UUID")
    private UUID consultationId;

    @Column(nullable = false, length = 200)
    private String name;

    @Enumerated(EnumType.STRING)
    private Specialty specialty;

    @Column(precision = 10, scale = 2)
    private BigDecimal price;

    @Column(length = 3)
    private String priceCurrency = "RON";

    private Integer durationMinutes;

    private Boolean requiresSurgeryRoom = false;

    private Boolean isActive = true;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "consultation_type_required_form_templates",
        joinColumns = @JoinColumn(name = "consultation_type_id"),
        inverseJoinColumns = @JoinColumn(name = "form_template_id")
    )
    @BatchSize(size = 20)
    @Builder.Default
    private Set<FormTemplate> requiredFormTemplates = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consultation_form_template_id")
    private FormTemplate consultationFormTemplate;

    @ManyToMany(mappedBy = "consultationTypes", fetch = FetchType.LAZY)
    @BatchSize(size = 10)
    private List<AppointmentSession> sessions;

    @CreationTimestamp
    @Column(updatable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime createdAt;

    @PrePersist
    void generateId() {
        if (consultationId == null) {
            consultationId = UUID.randomUUID();
        }
    }

    public Boolean getRequiresSurgeryRoom() {
        return requiresSurgeryRoom != null && requiresSurgeryRoom;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ConsultationType)) return false;
        ConsultationType that = (ConsultationType) o;
        return consultationId != null && Objects.equals(consultationId, that.consultationId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "ConsultationType{" +
                "consultationId=" + consultationId +
                ", name='" + name + '\'' +
                ", specialty=" + specialty +
                ", price=" + price +
                '}';
    }
}
