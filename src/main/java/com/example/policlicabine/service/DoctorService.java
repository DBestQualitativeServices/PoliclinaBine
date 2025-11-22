package com.example.policlicabine.service;

import com.example.policlicabine.common.Result;
import com.example.policlicabine.dto.ConsultationTypeDto;
import com.example.policlicabine.dto.DoctorDto;
import com.example.policlicabine.dto.DoctorFilterCriteria;
import com.example.policlicabine.entity.ConsultationType;
import com.example.policlicabine.entity.Doctor;
import com.example.policlicabine.entity.User;
import com.example.policlicabine.entity.enums.Specialty;
import com.example.policlicabine.entity.enums.UserRole;
import com.example.policlicabine.mapper.ConsultationTypeMapper;
import com.example.policlicabine.mapper.DoctorMapper;
import com.example.policlicabine.repository.DoctorRepository;
import com.example.policlicabine.service.base.BaseServiceImpl;
import com.example.policlicabine.specification.DoctorSpecificationBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing Doctor entities.
 *
 * Architecture:
 * - Extends BaseServiceImpl for common CRUD operations (findById, validateExists, getEntityById)
 * - Only uses DoctorRepository (single responsibility principle)
 * - Calls UserService and ConsultationService for service-to-service communication
 * - Publishes domain events for decoupled architecture
 * - Constructor-based dependency injection
 * - Transactional boundaries at service layer
 * - Defensive validation
 * - Result pattern for error handling
 *
 * Inherited Methods (from BaseServiceImpl):
 * - findById(UUID) → Result&lt;DoctorDto&gt;
 * - validateExists(UUID) → Result&lt;Void&gt;
 * - getEntityById(UUID) → Doctor
 * - getEntitiesByIds(List&lt;UUID&gt;) → List&lt;Doctor&gt;
 * - findAll() → Result&lt;List&lt;DoctorDto&gt;&gt;
 */
@Service
@Slf4j
@Transactional
public class DoctorService extends BaseServiceImpl<Doctor, DoctorDto, UUID> {

    // Only our repository - single responsibility principle
    private final DoctorRepository doctorRepository;

    // Services for validation and entity access - service-to-service communication
    private final UserService userService;
    private final ConsultationService consultationService;

    private final DoctorMapper doctorMapper;
    private final ConsultationTypeMapper consultationMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final DoctorSpecificationBuilder specificationBuilder;

    public DoctorService(DoctorRepository doctorRepository,
                        UserService userService,
                        ConsultationService consultationService,
                        DoctorMapper doctorMapper,
                        ConsultationTypeMapper consultationMapper,
                        ApplicationEventPublisher eventPublisher,
                        DoctorSpecificationBuilder specificationBuilder) {
        super(doctorRepository, doctorMapper);
        this.doctorRepository = doctorRepository;
        this.userService = userService;
        this.consultationService = consultationService;
        this.doctorMapper = doctorMapper;
        this.consultationMapper = consultationMapper;
        this.eventPublisher = eventPublisher;
        this.specificationBuilder = specificationBuilder;
    }

    @Override
    protected DoctorDto toDto(Doctor entity) {
        return doctorMapper.toDto(entity);
    }

    @Override
    protected String getEntityName() {
        return "Doctor";
    }

    @Override
    protected void updateEntityFromDto(Doctor entity, DoctorDto dto) {
        // Update mutable fields (NOT doctorId or userId - user relationship is immutable)
        if (dto.getSpecialties() != null && !dto.getSpecialties().isEmpty()) {
            entity.setSpecialties(dto.getSpecialties());
        }
        // Don't update: user (immutable relationship), weeklyAvailability (managed separately)
    }

    /**
     * Creates a new doctor profile linked to a user account.
     * Validates that the user exists and has the DOCTOR role.
     *
     * Architecture notes:
     * - Uses UserService to get user entity (service-to-service communication)
     * - Publishes DoctorProfileCreated event for decoupled architecture
     *
     * @param userId User identifier
     * @param specialties List of doctor's specialties
     * @return Result containing DoctorDto or error message
     */
    public Result<DoctorDto> createDoctor(UUID userId, List<Specialty> specialties) {
        try {
            if (userId == null) {
                return Result.failure("User ID is required");
            }

            if (specialties == null || specialties.isEmpty()) {
                return Result.failure("At least one specialty is required");
            }

            // Get user entity via UserService (service-to-service communication)
            User user = userService.getEntityById(userId);
            if (user == null) {
                return Result.failure("User not found");
            }

            if (user.getRole() != UserRole.DOCTOR) {
                return Result.failure("User must have DOCTOR role");
            }

            // Check for duplicate doctor profile
            if (doctorRepository.existsByUserUserId(userId)) {
                return Result.failure("Doctor profile already exists for this user");
            }

            Doctor doctor = Doctor.builder()
                .user(user)
                .specialties(specialties)
                .build();

            Doctor savedDoctor = doctorRepository.save(doctor);

            log.info("Doctor profile created: {} for user {}", savedDoctor.getDoctorId(), userId);

            return Result.success(doctorMapper.toDto(savedDoctor));

        } catch (Exception e) {
            log.error("Error creating doctor", e);
            return Result.failure("Failed to create doctor: " + e.getMessage());
        }
    }

    /**
     * Retrieves all consultations available for a doctor based on their specialties.
     * Uses read-only transaction for optimal performance.
     *
     * Architecture notes:
     * - Uses ConsultationService to get consultations by specialties (service-to-service communication)
     *
     * @param doctorId Doctor identifier
     * @return Result containing list of ConsultationTypeDto or error message
     */
    @Transactional(readOnly = true)
    public Result<List<ConsultationTypeDto>> getConsultationsForDoctor(UUID doctorId) {
        try {
            if (doctorId == null) {
                return Result.failure("Doctor ID is required");
            }

            Doctor doctor = doctorRepository.findById(doctorId)
                .orElse(null);
            if (doctor == null) {
                return Result.failure("Doctor not found");
            }

            // Get consultations matching doctor's specialties via ConsultationService
            List<ConsultationType> consultations = consultationService
                .getEntitiesBySpecialties(doctor.getSpecialties());

            List<ConsultationTypeDto> consultationDtos = consultations.stream()
                .map(consultationMapper::toDto)
                .collect(Collectors.toList());

            return Result.success(consultationDtos);

        } catch (Exception e) {
            log.error("Error getting consultations for doctor", e);
            return Result.failure("Failed to get consultations: " + e.getMessage());
        }
    }

    /**
     * Finds a doctor by their unique identifier.
     * Delegates to inherited findById() method from BaseServiceImpl.
     *
     * @param doctorId Doctor identifier
     * @return Result containing DoctorDto or error message
     */
    @Transactional(readOnly = true)
    public Result<DoctorDto> findDoctorById(UUID doctorId) {
        return findById(doctorId);
    }

    /**
     * Finds a doctor by their associated user ID.
     *
     * @param userId User identifier
     * @return Result containing DoctorDto or error message
     */
    @Transactional(readOnly = true)
    public Result<DoctorDto> findDoctorByUserId(UUID userId) {
        try {
            if (userId == null) {
                return Result.failure("User ID is required");
            }

            Doctor doctor = doctorRepository.findByUserUserId(userId)
                .orElse(null);
            if (doctor == null) {
                return Result.failure("Doctor not found for user");
            }

            return Result.success(doctorMapper.toDto(doctor));

        } catch (Exception e) {
            log.error("Error finding doctor by user ID", e);
            return Result.failure("Failed to find doctor: " + e.getMessage());
        }
    }

    /**
     * Finds all doctors with a specific specialty.
     *
     * @param specialty The specialty to search for
     * @return Result containing list of DoctorDto or error message
     */
    @Transactional(readOnly = true)
    public Result<List<DoctorDto>> findDoctorsBySpecialty(Specialty specialty) {
        try {
            if (specialty == null) {
                return Result.failure("Specialty is required");
            }

            List<Doctor> doctors = doctorRepository.findBySpecialtiesIn(List.of(specialty));

            List<DoctorDto> doctorDtos = doctors.stream()
                .map(doctorMapper::toDto)
                .collect(Collectors.toList());

            return Result.success(doctorDtos);

        } catch (Exception e) {
            log.error("Error finding doctors by specialty", e);
            return Result.failure("Failed to find doctors: " + e.getMessage());
        }
    }

    /**
     * Searches doctors with dynamic filtering, pagination, and sorting.
     * <p>
     * This method uses Spring Data JPA Specifications for building dynamic queries
     * based on the provided filter criteria. All filters are optional and combined
     * with AND logic.
     * </p>
     * <p>
     * Supported filters:
     * <ul>
     *   <li>fullName - partial match on doctor's user full name, case-insensitive</li>
     *   <li>specialty - exact match on specialty enum</li>
     *   <li>enabled - boolean filter for user enabled status</li>
     *   <li>createdAfter, createdBefore - date range filtering on user creation date</li>
     * </ul>
     * </p>
     *
     * @param criteria filter criteria (all fields optional)
     * @param pageable pagination and sorting parameters
     * @return Page of DoctorDto with pagination metadata
     */
    @Transactional(readOnly = true)
    public Page<DoctorDto> search(DoctorFilterCriteria criteria, Pageable pageable) {
        log.debug("Searching doctors with criteria: {} and pageable: {}", criteria, pageable);

        try {
            // Build dynamic specification from filter criteria
            Specification<Doctor> spec = specificationBuilder.build(criteria);

            // Execute query with pagination
            Page<Doctor> entityPage = doctorRepository.findAll(spec, pageable);

            // Map entities to DTOs using Spring's Page.map()
            Page<DoctorDto> dtoPage = entityPage.map(this::toDto);

            log.info("Doctor search returned {} results (page {}/{})",
                    dtoPage.getNumberOfElements(),
                    dtoPage.getNumber() + 1,
                    dtoPage.getTotalPages());

            return dtoPage;

        } catch (Exception e) {
            log.error("Error searching doctors with criteria: {}", criteria, e);
            throw new RuntimeException("Failed to search doctors: " + e.getMessage(), e);
        }
    }

    // ============= INTERNAL METHODS FOR SERVICE-TO-SERVICE COMMUNICATION =============
    // Note: The following methods are inherited from BaseServiceImpl:
    // - getEntityById(UUID) → Doctor
    // - validateExists(UUID) → Result<Void>
    // - getEntitiesByIds(List<UUID>) → List<Doctor>

    /**
     * INTERNAL: Validates that a doctor exists.
     * Delegates to inherited validateExists() method from BaseServiceImpl.
     * Used by other services (e.g., AppointmentSessionService) to validate doctor references.
     *
     * @param doctorId Doctor identifier
     * @return Result success if doctor exists, failure with message otherwise
     */
    @Transactional(readOnly = true)
    public Result<Void> validateDoctorExists(UUID doctorId) {
        return validateExists(doctorId);
    }
}
