package com.example.policlicabine.entity;

import com.example.policlicabine.entity.enums.Specialty;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "doctors", indexes = {
    @Index(name = "idx_doctor_user", columnList = "user_id")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Doctor {

    @Id
    @Column(columnDefinition = "UUID")
    private UUID doctorId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false, length = 200)
    private String fullName;

    @Column(length = 13)
    private String cnp;

    @Column(columnDefinition = "DATE")
    private LocalDate dataNastere;

    @ElementCollection(targetClass = Specialty.class, fetch = FetchType.LAZY)
    @CollectionTable(name = "doctor_specialties", joinColumns = @JoinColumn(name = "doctor_id"))
    @Enumerated(EnumType.STRING)
    private Set<Specialty> specialties = new HashSet<>();

    @OneToMany(mappedBy = "doctor", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY)
    @BatchSize(size = 10)
    private Set<WeeklyAvailability> weeklyAvailability = new HashSet<>();

    @OneToMany(mappedBy = "doctor", fetch = FetchType.LAZY)
    @BatchSize(size = 20)
    private List<AppointmentSession> appointments = new ArrayList<>();

    @PrePersist
    void generateId() {
        if (doctorId == null) {
            doctorId = UUID.randomUUID();
        }
    }

    public static LocalDate calculateBirthDateFromCnp(String cnp) {
        return Patient.calculateBirthDateFromCnp(cnp);  // Delegate to Patient
    }

    public void calculateBirthDateFromCnp() {
        if (this.cnp != null && !this.cnp.trim().isEmpty()) {
            LocalDate calculated = calculateBirthDateFromCnp(this.cnp);
            if (calculated != null) {
                this.dataNastere = calculated;
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Doctor)) return false;
        Doctor doctor = (Doctor) o;
        return doctorId != null && Objects.equals(doctorId, doctor.doctorId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Doctor{" +
                "doctorId=" + doctorId +
                ", fullName='" + fullName + '\'' +
                ", cnp='" + (cnp != null && cnp.length() >= 3 ? cnp.substring(0, 3) + "**********" : null) + '\'' +
                '}';
    }
}
