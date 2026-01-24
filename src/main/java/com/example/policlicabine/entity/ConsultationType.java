package com.example.policlicabine.entity;

import com.example.policlicabine.entity.enums.ConsultCategory;
import com.example.policlicabine.entity.enums.Specialty;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.HashSet;
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

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private ConsultCategory consultCategory = ConsultCategory.PREVENTIV;

    @Column(precision = 10, scale = 2)
    private BigDecimal price;

    @Column(length = 3)
    private String priceCurrency = "RON";

    private Integer durationMinutes;

    private Boolean requiresSurgeryRoom = false;

    private Boolean isActive = true;

    @Column(name = "workflow_step")
    private Integer workflowStep;

    @Column(name = "category_level_1", length = 100)
    private String categoryLevel1;

    @Column(name = "category_level_2", length = 200)
    private String categoryLevel2;

    @Column(name = "subcategory_level_1", length = 200)
    private String subcategoryLevel1;

    @Column(name = "subcategory_level_2", length = 200)
    private String subcategoryLevel2;

    @Column(name = "category_path", length = 500)
    private String categoryPath;

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
