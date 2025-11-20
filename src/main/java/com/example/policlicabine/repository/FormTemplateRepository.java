package com.example.policlicabine.repository;

import com.example.policlicabine.entity.FormTemplate;
import com.example.policlicabine.entity.enums.FormPurpose;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FormTemplateRepository extends JpaRepository<FormTemplate, UUID> {

    Optional<FormTemplate> findByCode(String code);

    List<FormTemplate> findByPurposeAndActiveTrueAndIsDeletedFalse(FormPurpose purpose);

    List<FormTemplate> findByActiveTrueAndIsDeletedFalse();

    @Query(value = """
        SELECT * FROM form_templates
        WHERE purpose = :purpose
        AND active = true
        AND is_deleted = false
        ORDER BY version DESC
        LIMIT 1
        """, nativeQuery = true)
    Optional<FormTemplate> findLatestByPurpose(@Param("purpose") String purpose);

    @Query(value = """
        SELECT * FROM form_templates
        WHERE structure ->> 'formId' = :formId
        AND is_deleted = false
        """, nativeQuery = true)
    Optional<FormTemplate> findByStructureFormId(@Param("formId") String formId);
}
