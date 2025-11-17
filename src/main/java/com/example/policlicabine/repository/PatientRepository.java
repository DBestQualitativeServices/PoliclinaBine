package com.example.policlicabine.repository;

import com.example.policlicabine.common.repository.FilterableRepository;
import com.example.policlicabine.entity.Patient;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PatientRepository extends FilterableRepository<Patient, UUID> {

    boolean existsByPhone(String phone);

    boolean existsByEmail(String email);

    Optional<Patient> findByEmail(String email);

    Optional<Patient> findByPhone(String phone);
}