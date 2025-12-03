package com.example.policlicabine.repository;

import com.example.policlicabine.common.repository.FilterableRepository;
import com.example.policlicabine.entity.FormTemplate;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FormTemplateRepository extends FilterableRepository<FormTemplate, UUID> {

    Optional<FormTemplate> findByNameAndIsDeletedFalse(String name);

    /**
     * Find by name regardless of deleted status - used for hard delete before recreate
     */
    Optional<FormTemplate> findByName(String name);

    List<FormTemplate> findByActiveTrueAndIsDeletedFalse();

    @Query(value = """
        SELECT * FROM form_templates
        WHERE structure ->> 'formId' = :formId
        AND is_deleted = false
        """, nativeQuery = true)
    Optional<FormTemplate> findByStructureFormId(@Param("formId") String formId);

    // ===== ENTITYGRAPH QUERIES =====

    @EntityGraph(attributePaths = {"createdBy"})
    Optional<FormTemplate> findWithCreatedById(UUID id);
}
