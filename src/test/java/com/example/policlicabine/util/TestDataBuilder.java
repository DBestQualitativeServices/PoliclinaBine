package com.example.policlicabine.util;

import com.example.policlicabine.entity.*;
import com.example.policlicabine.entity.enums.SessionStatus;
import com.example.policlicabine.entity.enums.Specialty;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Test Data Builder Pattern for creating test entities.
 * Provides fluent API with sensible defaults and easy customization.
 *
 * Usage:
 * <pre>
 * Patient patient = patient().withName("Ion", "Popescu").build();
 * Doctor doctor = doctor().build();
 * AppointmentSession session = appointment()
 *     .withPatient(patient)
 *     .withDoctor(doctor)
 *     .at(OffsetDateTime.parse("2026-02-20T10:00:00Z"))
 *     .withConsultations(consultation().build())
 *     .build();
 * </pre>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TestDataBuilder {

    // ===================================================================
    // PATIENT BUILDER
    // ===================================================================

    public static class PatientBuilder {
        private UUID patientId = UUID.randomUUID();
        private String firstName = "Maria";
        private String lastName = "Popescu";
        private String phone = "+40712345678";
        private String email = "maria.popescu@test.com";
        private String cnp = "2950101123456";
        private String sex = "F";

        public PatientBuilder withId(UUID id) {
            this.patientId = id;
            return this;
        }

        public PatientBuilder withName(String first, String last) {
            this.firstName = first;
            this.lastName = last;
            return this;
        }

        public PatientBuilder withPhone(String phone) {
            this.phone = phone;
            return this;
        }

        public PatientBuilder withEmail(String email) {
            this.email = email;
            return this;
        }

        public PatientBuilder withCnp(String cnp) {
            this.cnp = cnp;
            return this;
        }

        public Patient build() {
            Patient patient = Patient.builder()
                .patientId(patientId)
                .firstName(firstName)
                .lastName(lastName)
                .phone(phone)
                .email(email)
                .cnp(cnp)
                .sex(sex)
                .build();
            return patient;
        }
    }

    // ===================================================================
    // USER BUILDER
    // ===================================================================

    public static class UserBuilder {
        private UUID userId = UUID.randomUUID();
        private String username = "test.user";
        private String password = "encrypted_password";

        public UserBuilder withId(UUID id) {
            this.userId = id;
            return this;
        }

        public UserBuilder withUsername(String username) {
            this.username = username;
            return this;
        }

        public User build() {
            User user = User.builder()
                .userId(userId)
                .username(username)
                .password(password)
                .build();
            return user;
        }
    }

    // ===================================================================
    // DOCTOR BUILDER
    // ===================================================================

    public static class DoctorBuilder {
        private UUID doctorId = UUID.randomUUID();
        private User user;
        private String fullName = "Dr. Ion Ionescu";
        private Set<Specialty> specialties = new HashSet<>(Set.of(Specialty.GENERAL_DERMATOLOGY));

        public DoctorBuilder withId(UUID id) {
            this.doctorId = id;
            return this;
        }

        public DoctorBuilder withUser(User user) {
            this.user = user;
            return this;
        }

        public DoctorBuilder withFullName(String fullName) {
            this.fullName = fullName;
            return this;
        }

        public DoctorBuilder withSpecialties(Specialty... specialties) {
            this.specialties = new HashSet<>(Arrays.asList(specialties));
            return this;
        }

        public Doctor build() {
            if (user == null) {
                user = new UserBuilder()
                    .withUsername("dr." + fullName.toLowerCase().replace(" ", "."))
                    .build();
            }
            Doctor doctor = Doctor.builder()
                .doctorId(doctorId)
                .user(user)
                .fullName(fullName)
                .specialties(specialties)
                .build();
            return doctor;
        }
    }

    // ===================================================================
    // CONSULTATION TYPE BUILDER
    // ===================================================================

    public static class ConsultationTypeBuilder {
        private UUID consultationId = UUID.randomUUID();
        private String name = "Control dermatologic";
        private Integer durationMinutes = 30;
        private BigDecimal price = new BigDecimal("150.00");
        private Boolean isActive = true;
        private Specialty specialty = Specialty.GENERAL_DERMATOLOGY;

        public ConsultationTypeBuilder withId(UUID id) {
            this.consultationId = id;
            return this;
        }

        public ConsultationTypeBuilder withDuration(int minutes) {
            this.durationMinutes = minutes;
            return this;
        }

        public ConsultationTypeBuilder withName(String name) {
            this.name = name;
            return this;
        }

        public ConsultationTypeBuilder withPrice(String price) {
            this.price = new BigDecimal(price);
            return this;
        }

        public ConsultationTypeBuilder withSpecialty(Specialty specialty) {
            this.specialty = specialty;
            return this;
        }

        public ConsultationTypeBuilder inactive() {
            this.isActive = false;
            return this;
        }

        public ConsultationType build() {
            ConsultationType consultation = ConsultationType.builder()
                .consultationId(consultationId)
                .name(name)
                .durationMinutes(durationMinutes)
                .price(price)
                .isActive(isActive)
                .specialty(specialty)
                .build();
            return consultation;
        }
    }

    // ===================================================================
    // APPOINTMENT SESSION BUILDER
    // ===================================================================

    public static class AppointmentSessionBuilder {
        private UUID sessionId = UUID.randomUUID();
        private Patient patient;
        private Doctor doctor;
        private OffsetDateTime scheduledDateTime = OffsetDateTime.now().plusDays(1);
        private Set<ConsultationType> consultationTypes = new HashSet<>();
        private SessionStatus status = SessionStatus.SCHEDULED;
        private Boolean isEmergency = false;
        private Integer totalDurationMinutes;

        public AppointmentSessionBuilder withId(UUID id) {
            this.sessionId = id;
            return this;
        }

        public AppointmentSessionBuilder withPatient(Patient patient) {
            this.patient = patient;
            return this;
        }

        public AppointmentSessionBuilder withDoctor(Doctor doctor) {
            this.doctor = doctor;
            return this;
        }

        public AppointmentSessionBuilder at(OffsetDateTime dateTime) {
            this.scheduledDateTime = dateTime;
            return this;
        }

        public AppointmentSessionBuilder withConsultations(ConsultationType... types) {
            this.consultationTypes = new HashSet<>(Arrays.asList(types));
            // Recalculate duration
            this.totalDurationMinutes = Arrays.stream(types)
                .mapToInt(c -> c.getDurationMinutes() != null ? c.getDurationMinutes() : 0)
                .sum();
            return this;
        }

        public AppointmentSessionBuilder withStatus(SessionStatus status) {
            this.status = status;
            return this;
        }

        public AppointmentSessionBuilder asEmergency() {
            this.isEmergency = true;
            return this;
        }

        public AppointmentSessionBuilder withDuration(int minutes) {
            this.totalDurationMinutes = minutes;
            return this;
        }

        public AppointmentSession build() {
            if (patient == null) {
                patient = new PatientBuilder().build();
            }
            if (doctor == null) {
                doctor = new DoctorBuilder().build();
            }
            if (consultationTypes.isEmpty()) {
                consultationTypes.add(new ConsultationTypeBuilder().build());
            }
            if (totalDurationMinutes == null) {
                totalDurationMinutes = consultationTypes.stream()
                    .mapToInt(c -> c.getDurationMinutes() != null ? c.getDurationMinutes() : 0)
                    .sum();
            }

            AppointmentSession session = AppointmentSession.builder()
                .sessionId(sessionId)
                .patient(patient)
                .doctor(doctor)
                .scheduledDateTime(scheduledDateTime)
                .consultationTypes(consultationTypes)
                .status(status)
                .isEmergency(isEmergency)
                .totalDurationMinutes(totalDurationMinutes)
                .build();

            return session;
        }
    }

    // ===================================================================
    // STATIC FACTORY METHODS
    // ===================================================================

    public static PatientBuilder patient() {
        return new PatientBuilder();
    }

    public static UserBuilder user() {
        return new UserBuilder();
    }

    public static DoctorBuilder doctor() {
        return new DoctorBuilder();
    }

    public static ConsultationTypeBuilder consultation() {
        return new ConsultationTypeBuilder();
    }

    public static AppointmentSessionBuilder appointment() {
        return new AppointmentSessionBuilder();
    }

    // ===================================================================
    // COMMON TEST FIXTURES
    // ===================================================================

    public static class Fixtures {
        public static final OffsetDateTime BASE_DATE = OffsetDateTime.parse("2026-02-20T10:00:00Z");
        public static final String CONSULTATION_CONTROL = "Control dermatologic";
        public static final String CONSULTATION_ECOGRAFIE = "Ecografie";
        public static final String CONSULTATION_LASER = "Tratament laser";
        public static final int DEFAULT_DURATION = 30;
    }
}
