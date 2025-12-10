package com.example.policlicabine.repository;

import com.example.policlicabine.entity.FormSignature;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FormSignatureRepository extends JpaRepository<FormSignature, UUID> {

    List<FormSignature> findByFormSubmissionId(UUID formSubmissionId);

    List<FormSignature> findBySignedByUserId(UUID userId);

    Optional<FormSignature> findByFormSubmissionIdAndSignatureFieldId(UUID formSubmissionId, String signatureFieldId);

    boolean existsByFormSubmissionIdAndSignatureFieldId(UUID formSubmissionId, String signatureFieldId);

    @EntityGraph(attributePaths = {"signedBy"})
    List<FormSignature> findWithSignerByFormSubmissionId(UUID formSubmissionId);
}
