package com.example.policlicabine.service;

import com.example.policlicabine.common.Result;
import com.example.policlicabine.dto.DiagnosisDto;
import com.example.policlicabine.entity.Diagnosis;
import com.example.policlicabine.mapper.DiagnosisMapper;
import com.example.policlicabine.repository.DiagnosisRepository;
import com.example.policlicabine.service.base.BaseServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing Diagnosis entities (ICD-10 codes).
 *
 * Architecture:
 * - Extends BaseServiceImpl for common CRUD operations (findById, validateExists, getEntityById)
 * - Focuses on business-specific logic (ICD-10 code management)
 *
 * Inherited Methods (from BaseServiceImpl):
 * - findById(UUID) → Result&lt;DiagnosisDto&gt;
 * - validateExists(UUID) → Result&lt;Void&gt;
 * - getEntityById(UUID) → Diagnosis
 * - getEntitiesByIds(List&lt;UUID&gt;) → List&lt;Diagnosis&gt;
 * - findAll() → Result&lt;List&lt;DiagnosisDto&gt;&gt;
 */
@Service
@Slf4j
@Transactional
public class DiagnosisService extends BaseServiceImpl<Diagnosis, DiagnosisDto, UUID> {

    private final DiagnosisRepository diagnosisRepository;
    private final DiagnosisMapper diagnosisMapper;

    public DiagnosisService(DiagnosisRepository diagnosisRepository, DiagnosisMapper diagnosisMapper) {
        super(diagnosisRepository, diagnosisMapper);
        this.diagnosisRepository = diagnosisRepository;
        this.diagnosisMapper = diagnosisMapper;
    }

    @Override
    protected DiagnosisDto toDto(Diagnosis entity) {
        return diagnosisMapper.toDto(entity);
    }

    @Override
    protected String getEntityName() {
        return "Diagnosis";
    }

    @Override
    protected void updateEntityFromDto(Diagnosis entity, DiagnosisDto dto) {
        // Update mutable fields (NOT diagnosisId)
        if (dto.getIcd10Code() != null && !dto.getIcd10Code().trim().isEmpty()) {
            entity.setIcd10Code(dto.getIcd10Code().trim());
        }
        if (dto.getIcd10Description() != null && !dto.getIcd10Description().trim().isEmpty()) {
            entity.setIcd10Description(dto.getIcd10Description().trim());
        }
    }

    /**
     * Creates a new diagnosis (ICD-10 code).
     *
     * @param icd10Code ICD-10 code
     * @param description ICD-10 description
     * @return Result containing DiagnosisDto or error message
     */
    public Result<DiagnosisDto> createDiagnosis(String icd10Code, String description) {
        try {
            if (icd10Code == null || icd10Code.trim().isEmpty()) {
                return Result.failure("ICD-10 code is required");
            }

            Diagnosis diagnosis = Diagnosis.builder()
                .icd10Code(icd10Code.trim())
                .icd10Description(description != null ? description.trim() : null)
                .build();

            Diagnosis savedDiagnosis = diagnosisRepository.save(diagnosis);

            log.info("Diagnosis created: {} - {}", savedDiagnosis.getDiagnosisId(), icd10Code);

            return Result.success(diagnosisMapper.toDto(savedDiagnosis));

        } catch (Exception e) {
            log.error("Error creating diagnosis", e);
            return Result.failure("Failed to create diagnosis: " + e.getMessage());
        }
    }

    /**
     * Retrieves all diagnoses.
     * Delegates to inherited findAll() method from BaseServiceImpl.
     *
     * @return Result containing list of DiagnosisDto or error message
     */
    @Transactional(readOnly = true)
    public Result<List<DiagnosisDto>> getAllDiagnoses() {
        return findAll();
    }

    // ============= INTERNAL METHODS FOR SERVICE-TO-SERVICE COMMUNICATION =============
    // Note: The following methods are inherited from BaseServiceImpl:
    // - validateExists(UUID) → Result<Void>
    // - getEntityById(UUID) → Diagnosis
    // - getEntitiesByIds(List<UUID>) → List<Diagnosis>

    /**
     * Finds a diagnosis by ICD-10 code.
     *
     * @param icd10Code ICD-10 code
     * @return Result containing DiagnosisDto or error message
     */
    @Transactional(readOnly = true)
    public Result<DiagnosisDto> findByIcd10Code(String icd10Code) {
        try {
            if (icd10Code == null || icd10Code.trim().isEmpty()) {
                return Result.failure("ICD-10 code is required");
            }

            Diagnosis diagnosis = diagnosisRepository
                .findByIcd10Code(icd10Code.trim())
                .orElse(null);
            if (diagnosis == null) {
                return Result.failure("Diagnosis not found with ICD-10 code: " + icd10Code);
            }

            return Result.success(diagnosisMapper.toDto(diagnosis));

        } catch (Exception e) {
            log.error("Error finding diagnosis by ICD-10 code", e);
            return Result.failure("Failed to find diagnosis: " + e.getMessage());
        }
    }
}
