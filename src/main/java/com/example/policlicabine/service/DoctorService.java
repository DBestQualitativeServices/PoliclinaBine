package com.example.policlicabine.service;

import com.example.policlicabine.common.Result;
import com.example.policlicabine.dto.ConsultationTypeDto;
import com.example.policlicabine.dto.DoctorDto;
import com.example.policlicabine.dto.DoctorFilterCriteria;
import com.example.policlicabine.entity.ConsultationType;
import com.example.policlicabine.entity.Doctor;
import com.example.policlicabine.entity.User;
import com.example.policlicabine.entity.enums.Specialty;
import com.example.policlicabine.event.DoctorProfileCreated;
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

@Service
@Slf4j
@Transactional
public class DoctorService extends BaseServiceImpl<Doctor, DoctorDto, UUID> {

    private final DoctorRepository doctorRepository;

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
        if (dto.getFullName() != null && !dto.getFullName().trim().isEmpty()) {
            entity.setFullName(dto.getFullName().trim());
        }
        if (dto.getSpecialties() != null && !dto.getSpecialties().isEmpty()) {
            entity.setSpecialties(dto.getSpecialties());
        }
    }

    public Result<DoctorDto> createDoctor(UUID userId, String fullName, List<Specialty> specialties) {
        try {
            if (userId == null) {
                return Result.failure("User ID is required");
            }

            if (fullName == null || fullName.trim().isEmpty()) {
                return Result.failure("Full name is required");
            }

            if (specialties == null || specialties.isEmpty()) {
                return Result.failure("At least one specialty is required");
            }

            User user = userService.getEntityById(userId);
            if (user == null) {
                return Result.failure("User not found");
            }

            boolean hasDoctorRole = user.getRoles().stream()
                    .anyMatch(role -> role.getName() == UserRole.DOCTOR);

            if (!hasDoctorRole) {
                return Result.failure("User must have DOCTOR role");
            }

            // Check for duplicate doctor profile
            if (doctorRepository.existsByUserUserId(userId)) {
                return Result.failure("Doctor profile already exists for this user");
            }

            Doctor doctor = Doctor.builder()
                .user(user)
                .fullName(fullName.trim())
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

    @Transactional(readOnly = true)
    public Result<DoctorDto> findDoctorById(UUID doctorId) {
        return findById(doctorId);
    }

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

    @Transactional(readOnly = true)
    public Page<DoctorDto> search(DoctorFilterCriteria criteria, Pageable pageable) {
        log.debug("Searching doctors with criteria: {} and pageable: {}", criteria, pageable);

        try {
            Specification<Doctor> spec = specificationBuilder.build(criteria);
            Page<Doctor> entityPage = doctorRepository.findAll(spec, pageable);
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

    @Transactional(readOnly = true)
    public Result<Void> validateDoctorExists(UUID doctorId) {
        return validateExists(doctorId);
    }

    /**
     * INTERNAL: Creates a doctor profile linked to an existing user.
     * Used by AuthenticationService during doctor registration with user account.
     *
     * @param user User entity (already saved)
     * @param fullName Doctor full name
     * @param specialties List of specialties
     * @return Result containing DoctorDto or error message
     */
    @Transactional
    public Result<DoctorDto> createDoctorWithUser(User user, String fullName,
                                                   List<Specialty> specialties) {
        try {
            if (user == null) {
                return Result.failure("User is required");
            }
            if (fullName == null || fullName.trim().isEmpty()) {
                return Result.failure("Full name is required");
            }
            if (specialties == null || specialties.isEmpty()) {
                return Result.failure("At least one specialty is required");
            }

            // Check for duplicate doctor profile for this user
            if (doctorRepository.existsByUserUserId(user.getUserId())) {
                return Result.failure("Doctor profile already exists for this user");
            }

            // Build doctor entity
            Doctor doctor = Doctor.builder()
                    .user(user)
                    .fullName(fullName.trim())
                    .specialties(specialties)
                    .build();

            Doctor savedDoctor = doctorRepository.save(doctor);

            log.info("Doctor profile created: {} for user {}", savedDoctor.getDoctorId(), user.getUserId());

            // Publish event
            eventPublisher.publishEvent(new DoctorProfileCreated(
                    savedDoctor.getDoctorId(),
                    user.getUserId()
            ));

            return Result.success(doctorMapper.toDto(savedDoctor));

        } catch (Exception e) {
            log.error("Error creating doctor profile for user {}", user.getUserId(), e);
            return Result.failure("Failed to create doctor profile: " + e.getMessage());
        }
    }
}
