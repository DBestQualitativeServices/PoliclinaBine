package com.example.policlicabine.service;

import com.example.policlicabine.dto.DiagnosisDto;
import com.example.policlicabine.entity.Diagnosis;
import com.example.policlicabine.exception.BusinessException;
import com.example.policlicabine.exception.ResourceNotFoundException;
import com.example.policlicabine.mapper.DiagnosisMapper;
import com.example.policlicabine.repository.DiagnosisRepository;
import com.example.policlicabine.service.base.BaseServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service for managing Diagnosis entities (ICD-10 codes).
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
        if (dto.getIcd10Code() != null && !dto.getIcd10Code().trim().isEmpty()) {
            entity.setIcd10Code(dto.getIcd10Code().trim());
        }
        if (dto.getIcd10Description() != null && !dto.getIcd10Description().trim().isEmpty()) {
            entity.setIcd10Description(dto.getIcd10Description().trim());
        }
    }

    /**
     * Creates a new diagnosis (ICD-10 code).
     */
    public DiagnosisDto createDiagnosis(String icd10Code, String description) {
        if (icd10Code == null || icd10Code.trim().isEmpty()) {
            throw new BusinessException("ICD-10 code is required");
        }

        Diagnosis diagnosis = Diagnosis.builder()
            .icd10Code(icd10Code.trim())
            .icd10Description(description != null ? description.trim() : null)
            .build();

        Diagnosis savedDiagnosis = diagnosisRepository.save(diagnosis);

        log.info("Diagnosis created: {} - {}", savedDiagnosis.getDiagnosisId(), icd10Code);

        return diagnosisMapper.toDto(savedDiagnosis);
    }

    /**
     * Retrieves all diagnoses.
     */
    @Transactional(readOnly = true)
    public List<DiagnosisDto> getAllDiagnoses() {
        return findAll();
    }

    /**
     * Finds a diagnosis by ICD-10 code.
     */
    @Transactional(readOnly = true)
    public DiagnosisDto findByIcd10Code(String icd10Code) {
        if (icd10Code == null || icd10Code.trim().isEmpty()) {
            throw new BusinessException("ICD-10 code is required");
        }

        Diagnosis diagnosis = diagnosisRepository
            .findByIcd10Code(icd10Code.trim())
            .orElseThrow(() -> new ResourceNotFoundException("Diagnosis", "ICD-10 code: " + icd10Code));

        return diagnosisMapper.toDto(diagnosis);
    }
}
