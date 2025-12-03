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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

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
    }

    /**
     * Registers a new patient with validation.
     */
    public PatientDto registerNewPatient(String firstName, String lastName,
                                         String phone, String email, String address,
                                         String domiciliu, String ciSerie, String ciNumber,
                                         String ciEliberatDe, LocalDate ciDataEliberare) {
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
            .build();

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
                                            String ciEliberatDe, LocalDate ciDataEliberare) {
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
                .build();

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
}
