package com.example.policlicabine.repository;

import com.example.policlicabine.entity.Doctor;
import com.example.policlicabine.entity.enums.Specialty;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DoctorRepository extends JpaRepository<Doctor, UUID> {

    boolean existsByUserUserId(UUID userId);

    Optional<Doctor> findByUserUserId(UUID userId);

    @Query("SELECT d FROM Doctor d JOIN d.specialties s WHERE s IN :specialties")
    List<Doctor> findBySpecialtiesIn(@Param("specialties") List<Specialty> specialties);

    // ============= EntityGraph Query Methods =============

    /**
     * Finds doctor with user and weekly availability loaded.
     * Use for DTO mapping with nested relationships.
     */
    @EntityGraph(attributePaths = {"user", "weeklyAvailability"})
    Optional<Doctor> findWithUserAndAvailabilityByDoctorId(UUID doctorId);
}
