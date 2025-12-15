package com.example.policlicabine.repository;

import com.example.policlicabine.common.repository.FilterableRepository;
import com.example.policlicabine.entity.Patient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PatientRepository extends FilterableRepository<Patient, UUID> {

    Optional<Patient> findByEmail(String email);

    Optional<Patient> findByPhone(String phone);

    @EntityGraph(attributePaths = {"user"})
    Optional<Patient> findWithUserByPatientId(UUID id);

    // Override inherited method from JpaSpecificationExecutor to add EntityGraph
    // Loads User with all necessary relationships to prevent N+1 queries
    @EntityGraph(attributePaths = {
        "user",                  // Load User entity
        "user.roles",            // Load User's roles (for UserMapper)
        "user.doctorProfile",    // Load Doctor profile (prevent N+1 check)
        "user.managerProfile"    // Load Manager profile (prevent N+1 check)
    })
    Page<Patient> findAll(Specification<Patient> spec, Pageable pageable);

    boolean existsByUserUserId(UUID userId);
}