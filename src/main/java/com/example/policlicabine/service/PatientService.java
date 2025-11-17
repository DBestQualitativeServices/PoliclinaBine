package com.example.policlicabine.service;

import com.example.policlicabine.common.Result;
import com.example.policlicabine.dto.PatientDto;
import com.example.policlicabine.dto.PatientFilterCriteria;
import com.example.policlicabine.entity.Patient;
import com.example.policlicabine.event.PatientConsentStatusChanged;
import com.example.policlicabine.event.PatientPersonalInfoUpdated;
import com.example.policlicabine.event.PatientRegistered;
import com.example.policlicabine.mapper.PatientMapper;
import com.example.policlicabine.repository.PatientRepository;
import com.example.policlicabine.service.base.BaseServiceImpl;
import com.example.policlicabine.service.base.ServiceHelper;
import com.example.policlicabine.specification.PatientSpecificationBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

/**
 * Service for managing Patient entities.
 *
 * Architecture:
 * - Extends BaseServiceImpl for common CRUD operations (findById, validateExists, getEntityById)
 * - Focuses on business-specific logic (registration, updates, consent management)
 * - Uses domain events for decoupled architecture
 * - Result pattern for consistent error handling
 *
 * Inherited Methods (from BaseServiceImpl):
 * - findById(UUID) → Result&lt;PatientDto&gt;
 * - validateExists(UUID) → Result&lt;Void&gt;
 * - getEntityById(UUID) → Patient
 * - findAll() → Result&lt;List&lt;PatientDto&gt;&gt;
 */
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
        // Update mutable fields only (NOT patientId or registrationDate)
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
        if (dto.getConsentFileUrl() != null) {
            entity.setConsentFileUrl(dto.getConsentFileUrl().trim());
        }
    }

    /**
     * Registers a new patient with validation.
     * @param firstName Required first name
     * @param lastName Required last name
     * @param phone Required phone number (must be unique)
     * @param email Optional email address
     * @param address Optional address
     * @return Result containing PatientDto or error message
     */
    public Result<PatientDto> registerNewPatient(String firstName, String lastName,
                                                String phone, String email, String address) {
        try {
            // Validate required fields
            if (firstName == null || firstName.trim().isEmpty()) {
                return Result.failure("First name is required");
            }
            if (lastName == null || lastName.trim().isEmpty()) {
                return Result.failure("Last name is required");
            }
            if (phone == null || phone.trim().isEmpty()) {
                return Result.failure("Phone number is required");
            }

            // Check for duplicate phone
            if (patientRepository.existsByPhone(phone.trim())) {
                return Result.failure("Patient with this phone number already exists");
            }

            // Check for duplicate email
            if (email != null && patientRepository.existsByEmail(email.trim())) {
                return Result.failure(ServiceHelper.alreadyExistsMessage("Patient", "email"));
            }

            // Build and save patient
            Patient patient = Patient.builder()
                .firstName(firstName.trim())
                .lastName(lastName.trim())
                .phone(phone.trim())
                .email(email != null ? email.trim() : null)
                .address(address != null ? address.trim() : null)
                .consentFileUrl(null)
                .build();

            Patient savedPatient = patientRepository.save(patient);

            // Publish domain event (asynchronous processing)
            PatientRegistered event = new PatientRegistered(
                savedPatient.getPatientId(), firstName, lastName, phone, email);
            eventPublisher.publishEvent(event);

            log.info("New patient registered: {} {} (ID: {})", firstName, lastName, savedPatient.getPatientId());

            return Result.success(patientMapper.toDto(savedPatient));

        } catch (Exception e) {
            log.error("Error registering new patient", e);
            return Result.failure("Failed to register patient: " + e.getMessage());
        }
    }

    /**
     * Updates patient personal information (phone, email, address).
     * @param patientId Patient identifier
     * @param phone New phone number
     * @param email New email address
     * @param address New address
     * @return Result containing updated PatientDto or error message
     */
    public Result<PatientDto> updatePatientPersonalInfo(UUID patientId, String phone, String email, String address) {
        try {
            if (patientId == null) {
                return Result.failure("Patient ID is required");
            }

            Patient patient = patientRepository.findById(patientId)
                .orElse(null);
            if (patient == null) {
                return Result.failure("Patient not found");
            }

            // Store old values for event comparison
            String oldPhone = patient.getPhone();
            String oldEmail = patient.getEmail();
            String oldAddress = patient.getAddress();

            // Update fields if provided
            if (phone != null) {
                patient.setPhone(phone.trim());
            }
            if (email != null) {
                patient.setEmail(email.trim());
            }
            if (address != null) {
                patient.setAddress(address.trim());
            }

            Patient savedPatient = patientRepository.save(patient);

            // Publish event if something changed
            if (!Objects.equals(oldPhone, patient.getPhone()) ||
                !Objects.equals(oldEmail, patient.getEmail()) ||
                !Objects.equals(oldAddress, patient.getAddress())) {

                eventPublisher.publishEvent(new PatientPersonalInfoUpdated(
                    patientId, patient.getPhone(), patient.getEmail(), patient.getAddress()));
            }

            log.info("Patient personal info updated: {}", patientId);

            return Result.success(patientMapper.toDto(savedPatient));

        } catch (Exception e) {
            log.error("Error updating patient info", e);
            return Result.failure("Failed to update patient info: " + e.getMessage());
        }
    }

    /**
     * Updates the consent file URL for a patient.
     * @param patientId Patient identifier
     * @param consentFileUrl URL to consent file
     * @return Result containing updated PatientDto or error message
     */
    public Result<PatientDto> updateConsentFile(UUID patientId, String consentFileUrl) {
        try {
            if (patientId == null) {
                return Result.failure("Patient ID is required");
            }

            Patient patient = patientRepository.findById(patientId)
                .orElse(null);
            if (patient == null) {
                return Result.failure("Patient not found");
            }

            patient.setConsentFileUrl(consentFileUrl);
            Patient savedPatient = patientRepository.save(patient);

            // Publish consent status change event
            eventPublisher.publishEvent(new PatientConsentStatusChanged(
                patientId, savedPatient.hasConsentSigned()));

            log.info("Patient consent file updated: {}", patientId);

            return Result.success(patientMapper.toDto(savedPatient));

        } catch (Exception e) {
            log.error("Error updating consent file", e);
            return Result.failure("Failed to update consent file: " + e.getMessage());
        }
    }

    /**
     * Searches patients with dynamic filtering, pagination, and sorting.
     * <p>
     * This method uses Spring Data JPA Specifications for building dynamic queries
     * based on the provided filter criteria. All filters are optional and combined
     * with AND logic.
     * </p>
     * <p>
     * Supported filters:
     * <ul>
     *   <li>firstName, lastName, phone, email - partial match, case-insensitive</li>
     *   <li>registeredAfter, registeredBefore - date range filtering</li>
     *   <li>hasConsent - boolean filter for consent file presence</li>
     * </ul>
     * </p>
     *
     * @param criteria filter criteria (all fields optional)
     * @param pageable pagination and sorting parameters
     * @return Page of PatientDto with pagination metadata
     */
    @Transactional(readOnly = true)
    public Page<PatientDto> search(PatientFilterCriteria criteria, Pageable pageable) {
        log.debug("Searching patients with criteria: {} and pageable: {}", criteria, pageable);

        try {
            // Build dynamic specification from filter criteria
            Specification<Patient> spec = specificationBuilder.build(criteria);

            // Execute query with pagination
            Page<Patient> entityPage = patientRepository.findAll(spec, pageable);

            // Map entities to DTOs using Spring's Page.map()
            Page<PatientDto> dtoPage = entityPage.map(this::toDto);

            log.info("Patient search returned {} results (page {}/{})",
                    dtoPage.getNumberOfElements(),
                    dtoPage.getNumber() + 1,
                    dtoPage.getTotalPages());

            return dtoPage;

        } catch (Exception e) {
            log.error("Error searching patients with criteria: {}", criteria, e);
            throw new RuntimeException("Failed to search patients: " + e.getMessage(), e);
        }
    }

    /**
     * Finds a patient by their unique identifier.
     * Delegates to inherited findById() method from BaseServiceImpl.
     *
     * @param patientId Patient identifier
     * @return Result containing PatientDto or error message
     */
    @Transactional(readOnly = true)
    public Result<PatientDto> findPatientById(UUID patientId) {
        return findById(patientId);
    }

    /**
     * Finds a patient by phone number.
     * @param phone Phone number
     * @return Result containing PatientDto or error message
     */
    @Transactional(readOnly = true)
    public Result<PatientDto> findPatientByPhone(String phone) {
        try {
            if (phone == null || phone.trim().isEmpty()) {
                return Result.failure("Phone number is required");
            }

            Patient patient = patientRepository.findByPhone(phone.trim())
                .orElse(null);
            if (patient == null) {
                return Result.failure("Patient not found with phone: " + phone);
            }

            return Result.success(patientMapper.toDto(patient));

        } catch (Exception e) {
            log.error("Error finding patient by phone", e);
            return Result.failure("Failed to find patient: " + e.getMessage());
        }
    }

    // ============= INTERNAL METHODS FOR SERVICE-TO-SERVICE COMMUNICATION =============
    // Note: validateExists(UUID) and getEntityById(UUID) are inherited from BaseServiceImpl

    /**
     * INTERNAL: Validates that a patient exists.
     * Delegates to inherited validateExists() method from BaseServiceImpl.
     * Used by other services (e.g., AppointmentSessionService) to validate patient references.
     *
     * @param patientId Patient identifier
     * @return Result success if patient exists, failure with message otherwise
     */
    @Transactional(readOnly = true)
    public Result<Void> validatePatientExists(UUID patientId) {
        return validateExists(patientId);
    }
}
