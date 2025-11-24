package com.example.policlicabine.repository;

import com.example.policlicabine.common.repository.FilterableRepository;
import com.example.policlicabine.entity.Patient;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PatientRepository extends FilterableRepository<Patient, UUID> {

    Optional<Patient> findByEmail(String email);

    Optional<Patient> findByPhone(String phone);

    @EntityGraph(attributePaths = {"consentFile", "consentFile.uploadedBy"})
    @Query("SELECT p FROM Patient p WHERE p.patientId = :id")
    Optional<Patient> findWithConsentFileById(UUID id);

    @EntityGraph(attributePaths = {"files", "files.uploadedBy"})
    @Query("SELECT p FROM Patient p WHERE p.patientId = :id")
    Optional<Patient> findWithFilesById(UUID id);


    @EntityGraph(attributePaths = {
        "consentFile", "consentFile.uploadedBy",
        "files", "files.uploadedBy"
    })
    @Query("SELECT p FROM Patient p WHERE p.patientId = :id")
    Optional<Patient> findWithAllFilesById(UUID id);

    @EntityGraph(attributePaths = {"user"})
    Optional<Patient> findWithUserById(UUID id);

    boolean existsByUserUserId(UUID userId);
}