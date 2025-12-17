package com.example.policlicabine.service;

import com.example.policlicabine.dto.PatientDto;
import com.example.policlicabine.dto.PatientFilterCriteria;
import com.example.policlicabine.entity.Patient;
import com.example.policlicabine.entity.User;
import com.example.policlicabine.event.NewPatientRegisteredEvent;
import com.example.policlicabine.exception.BusinessException;
import com.example.policlicabine.exception.ResourceNotFoundException;
import com.example.policlicabine.mapper.PatientMapper;
import com.example.policlicabine.repository.PatientRepository;
import com.example.policlicabine.service.base.BaseServiceImpl;
import com.example.policlicabine.specification.PatientSpecificationBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.temporal.Temporal;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional
public class PatientService extends BaseServiceImpl<Patient, PatientDto, UUID> {

    private final PatientRepository patientRepository;
    private final PatientMapper patientMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final PatientSpecificationBuilder specificationBuilder;

    public PatientService(PatientRepository patientRepository, PatientMapper patientMapper,
                         ApplicationEventPublisher eventPublisher,
                         PatientSpecificationBuilder specificationBuilder) {
        super(patientRepository, patientMapper);
        this.patientRepository = patientRepository;
        this.patientMapper = patientMapper;
        this.eventPublisher = eventPublisher;
        this.specificationBuilder = specificationBuilder;
    }

    @Override
    protected PatientDto toDto(Patient entity) {
        return patientMapper.toDto(entity);
    }

    @Override
    protected String getEntityName() {
        return "Patient";
    }

    @Override
    protected void updateEntityFromDto(Patient entity, PatientDto dto) {
        if (dto.getFirstName() != null && !dto.getFirstName().trim().isEmpty()) {
            entity.setFirstName(dto.getFirstName().trim());
        }
        if (dto.getLastName() != null && !dto.getLastName().trim().isEmpty()) {
            entity.setLastName(dto.getLastName().trim());
        }
        if (dto.getPhone() != null) {
            entity.setPhone(dto.getPhone().trim());
        }
        if (dto.getEmail() != null) {
            entity.setEmail(dto.getEmail().trim());
        }
        if (dto.getAddress() != null) {
            entity.setAddress(dto.getAddress().trim());
        }
        if (dto.getDomiciliu() != null) {
            entity.setDomiciliu(dto.getDomiciliu().trim());
        }
        if (dto.getCiSerie() != null) {
            entity.setCiSerie(dto.getCiSerie().trim().toUpperCase());
        }
        if (dto.getCiNumber() != null) {
            entity.setCiNumber(dto.getCiNumber().trim());
        }
        if (dto.getCiEliberatDe() != null) {
            entity.setCiEliberatDe(dto.getCiEliberatDe().trim());
        }
        if (dto.getCiDataEliberare() != null) {
            entity.setCiDataEliberare(dto.getCiDataEliberare());
        }
        if (dto.getCnp() != null) {
            entity.setCnp(dto.getCnp().trim());
            entity.calculateFieldsFromCnp(); // Auto-calculate sex/minor
        }
        if (dto.getSursa() != null) {
            entity.setSursa(dto.getSursa().trim());
        }
        // Note: sex and minor are auto-calculated from CNP, but can be manually overridden
        if (dto.getSex() != null) {
            entity.setSex(dto.getSex().trim());
        }
        if (dto.getMinor() != null) {
            entity.setMinor(dto.getMinor());
        }

        // Sync entity fields to form field cache after any update
        entity.syncEntityFieldsToCache();
    }

    /**
     * Registers a new patient with validation.
     */
    public PatientDto registerNewPatient(String firstName, String lastName,
                                         String phone, String email, String address,
                                         String domiciliu, String ciSerie, String ciNumber,
                                         String ciEliberatDe, LocalDate ciDataEliberare,
                                         String cnp, String sursa) {
        if (firstName == null || firstName.trim().isEmpty()) {
            throw new BusinessException("First name is required");
        }
        if (lastName == null || lastName.trim().isEmpty()) {
            throw new BusinessException("Last name is required");
        }
        if (phone == null || phone.trim().isEmpty()) {
            throw new BusinessException("Phone number is required");
        }

        Patient patient = Patient.builder()
            .firstName(firstName.trim())
            .lastName(lastName.trim())
            .phone(phone.trim())
            .email(email != null ? email.trim() : null)
            .address(address != null ? address.trim() : null)
            .domiciliu(domiciliu != null ? domiciliu.trim() : null)
            .ciSerie(ciSerie != null ? ciSerie.trim().toUpperCase() : null)
            .ciNumber(ciNumber != null ? ciNumber.trim() : null)
            .ciEliberatDe(ciEliberatDe != null ? ciEliberatDe.trim() : null)
            .ciDataEliberare(ciDataEliberare)
            .cnp(cnp != null ? cnp.trim() : null)
            .sursa(sursa != null ? sursa.trim() : null)
            .build();

        // Calculate sex and minor from CNP if available
        patient.calculateFieldsFromCnp();

        // Initialize form field cache with entity fields
        patient.syncEntityFieldsToCache();

        Patient savedPatient = patientRepository.save(patient);

        log.info("New patient registered: {} {} (ID: {})", firstName, lastName, savedPatient.getPatientId());

        eventPublisher.publishEvent(new NewPatientRegisteredEvent(
            savedPatient.getPatientId(),
            savedPatient.getFirstName(),
            savedPatient.getLastName(),
            savedPatient.getEmail()
        ));

        return patientMapper.toDto(savedPatient);
    }

    /**
     * Updates patient personal information (phone, email, address).
     */
    public PatientDto updatePatientPersonalInfo(UUID patientId, String phone, String email, String address) {
        if (patientId == null) {
            throw new BusinessException("Patient ID is required");
        }

        Patient patient = patientRepository.findById(patientId)
            .orElseThrow(() -> new ResourceNotFoundException("Patient", patientId));

        if (phone != null) {
            patient.setPhone(phone.trim());
        }
        if (email != null) {
            patient.setEmail(email.trim());
        }
        if (address != null) {
            patient.setAddress(address.trim());
        }

        // Sync entity fields to form field cache
        patient.syncEntityFieldsToCache();

        Patient savedPatient = patientRepository.save(patient);

        log.info("Patient personal info updated: {}", patientId);

        return patientMapper.toDto(savedPatient);
    }

    /**
     * Searches patients with dynamic filtering, pagination, and sorting.
     */
    @Transactional(readOnly = true)
    public Page<PatientDto> search(PatientFilterCriteria criteria, Pageable pageable) {
        log.debug("Searching patients with criteria: {} and pageable: {}", criteria, pageable);

        Specification<Patient> spec = specificationBuilder.build(criteria);
        Page<Patient> entityPage = patientRepository.findAll(spec, pageable);
        Page<PatientDto> dtoPage = entityPage.map(this::toDto);

        log.info("Patient search returned {} results (page {}/{})",
                dtoPage.getNumberOfElements(),
                dtoPage.getNumber() + 1,
                dtoPage.getTotalPages());

        return dtoPage;
    }

    /**
     * Finds a patient by their unique identifier.
     */
    @Transactional(readOnly = true)
    public PatientDto findPatientById(UUID patientId) {
        return findById(patientId);
    }

    /**
     * Finds a patient by phone number.
     */
    @Transactional(readOnly = true)
    public PatientDto findPatientByPhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            throw new BusinessException("Phone number is required");
        }

        Patient patient = patientRepository.findByPhone(phone.trim())
            .orElseThrow(() -> new ResourceNotFoundException("Patient", "phone: " + phone));

        return patientMapper.toDto(patient);
    }

    /**
     * INTERNAL: Validates that a patient exists.
     */
    @Transactional(readOnly = true)
    public void validatePatientExists(UUID patientId) {
        validateExists(patientId);
    }

    /**
     * INTERNAL: Creates a patient profile linked to an existing user.
     */
    @Transactional
    public PatientDto createPatientWithUser(User user, String firstName, String lastName,
                                            String phone, String email, String address,
                                            String domiciliu, String ciSerie, String ciNumber,
                                            String ciEliberatDe, LocalDate ciDataEliberare,
                                            String cnp, String sursa) {
        if (user == null) {
            throw new BusinessException("User is required");
        }
        if (firstName == null || firstName.trim().isEmpty()) {
            throw new BusinessException("First name is required");
        }
        if (lastName == null || lastName.trim().isEmpty()) {
            throw new BusinessException("Last name is required");
        }
        if (phone == null || phone.trim().isEmpty()) {
            throw new BusinessException("Phone number is required");
        }

        if (patientRepository.existsByUserUserId(user.getUserId())) {
            throw new BusinessException("Patient profile already exists for this user");
        }

        Patient patient = Patient.builder()
                .user(user)
                .firstName(firstName.trim())
                .lastName(lastName.trim())
                .phone(phone.trim())
                .email(email != null ? email.trim() : null)
                .address(address != null ? address.trim() : null)
                .domiciliu(domiciliu != null ? domiciliu.trim() : null)
                .ciSerie(ciSerie != null ? ciSerie.trim().toUpperCase() : null)
                .ciNumber(ciNumber != null ? ciNumber.trim() : null)
                .ciEliberatDe(ciEliberatDe != null ? ciEliberatDe.trim() : null)
                .ciDataEliberare(ciDataEliberare)
                .cnp(cnp != null ? cnp.trim() : null)
                .sursa(sursa != null ? sursa.trim() : null)
                .build();

        // Calculate sex and minor from CNP if available
        patient.calculateFieldsFromCnp();

        // Initialize form field cache with entity fields
        patient.syncEntityFieldsToCache();

        Patient savedPatient = patientRepository.save(patient);

        log.info("Patient profile created: {} for user {}", savedPatient.getPatientId(), user.getUserId());

        eventPublisher.publishEvent(new NewPatientRegisteredEvent(
                savedPatient.getPatientId(),
                savedPatient.getFirstName(),
                savedPatient.getLastName(),
                savedPatient.getEmail()
        ));

        return patientMapper.toDto(savedPatient);
    }

    // ==================== Form Prefilling Methods ====================

    /**
     * Returns patient's cached form field data for form prefilling.
     * The cache includes both entity fields and custom form fields from previous submissions.
     * Returns empty map if patient not found (lenient behavior).
     *
     * @param patientId the patient ID
     * @return Map with lowercase keys and string values, empty map if patient not found
     */
    @Transactional(readOnly = true)
    public Map<String, String> getPatientFieldMapForPrefilling(UUID patientId) {
        if (patientId == null) {
            return Collections.emptyMap();
        }

        Patient patient = getEntityById(patientId);
        if (patient == null) {
            log.warn("Patient not found for form prefilling: {}", patientId);
            return Collections.emptyMap();
        }

        return extractCachedFieldMap(patient);
    }

    /**
     * Updates patient's form field cache with new data from form submission.
     * Uses "latest wins" semantics - new values overwrite existing ones.
     *
     * @param patientId the patient ID
     * @param formData the submitted form data to merge into cache
     */
    @Transactional
    public void updateFormFieldCache(UUID patientId, Map<String, Object> formData) {
        if (patientId == null || formData == null || formData.isEmpty()) {
            return;
        }

        Patient patient = patientRepository.findById(patientId).orElse(null);
        if (patient == null) {
            log.warn("Cannot update form field cache: patient {} not found", patientId);
            return;
        }

        patient.updateFormFieldCache(formData);
        patientRepository.save(patient);

        log.debug("Updated form field cache for patient {}: {} fields", patientId, formData.size());
    }

    /**
     * Extracts cached form fields as lowercase-keyed string map.
     */
    private Map<String, String> extractCachedFieldMap(Patient patient) {
        Map<String, String> result = new HashMap<>();

        if (patient.getFormFieldCache() == null || patient.getFormFieldCache().isEmpty()) {
            return result;
        }

        for (Map.Entry<String, Object> entry : patient.getFormFieldCache().entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null) {
                String key = entry.getKey().toLowerCase();
                String value = convertToString(entry.getValue());
                if (value != null && !value.isEmpty()) {
                    result.put(key, value);
                }
            }
        }

        return result;
    }

    /**
     * Converts various form field value types to string for prefilling.
     * Handles: String, Number, Boolean, List, LocalDate, LocalDateTime, OffsetDateTime.
     */
    private String convertToString(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String s) {
            return s.trim().isEmpty() ? null : s.trim();
        }
        if (value instanceof Number || value instanceof Boolean) {
            return value.toString();
        }
        // Handle date types
        if (value instanceof LocalDate || value instanceof LocalDateTime || value instanceof OffsetDateTime || value instanceof Temporal) {
            return value.toString();
        }
        // Multi-select fields: join with comma
        if (value instanceof List<?> list) {
            return list.stream()
                .filter(Objects::nonNull)
                .map(Object::toString)
                .collect(Collectors.joining(", "));
        }
        return value.toString();
    }

    // ==================== Scheduled Tasks ====================

    /**
     * Scheduled task to recalculate sex and minor fields from CNP for all patients.
     * Runs daily at 2:00 AM.
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void recalculateSexAndMinorFromCnp() {
        log.info("Starting scheduled CNP recalculation for all patients");

        List<Patient> patients = patientRepository.findAll();
        int updatedCount = 0;
        int skippedCount = 0;

        for (Patient patient : patients) {
            if (patient.getCnp() != null && !patient.getCnp().trim().isEmpty()) {
                String oldSex = patient.getSex();
                Boolean oldMinor = patient.getMinor();

                patient.calculateFieldsFromCnp();

                // Only count as updated if values actually changed
                if (!Objects.equals(oldSex, patient.getSex()) ||
                    !Objects.equals(oldMinor, patient.getMinor())) {
                    updatedCount++;
                    log.debug("Updated patient {}: sex={}->{}, minor={}->{}",
                        patient.getPatientId(), oldSex, patient.getSex(),
                        oldMinor, patient.getMinor());
                }
            } else {
                skippedCount++;
            }
        }

        if (updatedCount > 0) {
            patientRepository.saveAll(patients);
        }

        log.info("CNP recalculation completed: {} patients updated, {} skipped (no CNP)",
            updatedCount, skippedCount);
    }
}
