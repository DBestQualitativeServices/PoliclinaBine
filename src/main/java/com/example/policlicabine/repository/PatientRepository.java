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
    Optional<Patient> findWithConsentFileByPatientId(UUID id);

    @EntityGraph(attributePaths = {"files", "files.uploadedBy"})
    Optional<Patient> findWithFilesByPatientId(UUID id);


    @EntityGraph(attributePaths = {
        "consentFile", "consentFile.uploadedBy",
        "files", "files.uploadedBy"
    })
    Optional<Patient> findWithAllFilesByPatientId(UUID patientId);

    @EntityGraph(attributePaths = {"user"})
    Optional<Patient> findWithUserByPatientId(UUID id);

    boolean existsByUserUserId(UUID userId);
}