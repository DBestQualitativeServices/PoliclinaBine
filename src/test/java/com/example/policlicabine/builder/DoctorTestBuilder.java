package com.example.policlicabine.builder;

import com.example.policlicabine.entity.Doctor;
import com.example.policlicabine.entity.User;
import com.example.policlicabine.entity.enums.Specialty;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Test data builder for Doctor entity using the Builder pattern.
 * <p>
 * Provides defaults with common specialties and linked User.
 * </p>
 * <p>Example usage:</p>
 * <pre>
 * User doctorUser = UserTestBuilder.aDoctor().build();
 * Doctor doctor = DoctorTestBuilder.aDoctor()
 *     .withUser(doctorUser)
 *     .withSpecialties(Specialty.CARDIOLOGY)
 *     .build();
 * </pre>
 */
public class DoctorTestBuilder {

    private UUID doctorId = UUID.randomUUID();
    private User user = UserTestBuilder.aDoctor().build();
    private String fullName = "Dr. Test Doctor";
    private List<Specialty> specialties = new ArrayList<>(Arrays.asList(Specialty.GENERAL_DERMATOLOGY));

    public static DoctorTestBuilder aDoctor() {
        return new DoctorTestBuilder();
    }

    public static DoctorTestBuilder aGeneralPractitioner() {
        return new DoctorTestBuilder()
                .withSpecialties(Specialty.GENERAL_DERMATOLOGY);
    }

    public static DoctorTestBuilder aCardiologist() {
        return new DoctorTestBuilder()
                .withSpecialties(Specialty.COSMETIC_DERMATOLOGY);
    }

    public static DoctorTestBuilder aDermatologist() {
        return new DoctorTestBuilder()
                .withSpecialties(Specialty.MEDICAL_DERMATOLOGY);
    }

    public static DoctorTestBuilder aSurgeon() {
        return new DoctorTestBuilder()
                .withSpecialties(Specialty.MOLES);
    }

    public DoctorTestBuilder withDoctorId(UUID doctorId) {
        this.doctorId = doctorId;
        return this;
    }

    public DoctorTestBuilder withUser(User user) {
        this.user = user;
        return this;
    }

    public DoctorTestBuilder withFullName(String fullName) {
        this.fullName = fullName;
        return this;
    }

    public DoctorTestBuilder withSpecialties(Specialty... specialties) {
        this.specialties = new ArrayList<>(Arrays.asList(specialties));
        return this;
    }

    public DoctorTestBuilder withSpecialties(List<Specialty> specialties) {
        this.specialties = new ArrayList<>(specialties);
        return this;
    }

    public DoctorTestBuilder addSpecialty(Specialty specialty) {
        this.specialties.add(specialty);
        return this;
    }

    public Doctor build() {
        return Doctor.builder()
                .doctorId(doctorId)
                .user(user)
                .fullName(fullName)
                .specialties(specialties)
                .weeklyAvailability(new ArrayList<>())
                .appointments(new ArrayList<>())
                .build();
    }
}
