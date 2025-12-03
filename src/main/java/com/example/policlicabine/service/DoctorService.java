package com.example.policlicabine.service;

import com.example.policlicabine.dto.ConsultationTypeDto;
import com.example.policlicabine.dto.DoctorDto;
import com.example.policlicabine.dto.DoctorFilterCriteria;
import com.example.policlicabine.entity.ConsultationType;
import com.example.policlicabine.entity.Doctor;
import com.example.policlicabine.entity.User;
import com.example.policlicabine.entity.enums.Specialty;
import com.example.policlicabine.entity.enums.UserRole;
import com.example.policlicabine.event.DoctorProfileCreated;
import com.example.policlicabine.exception.BusinessException;
import com.example.policlicabine.exception.ResourceNotFoundException;
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

    public DoctorDto createDoctor(UUID userId, String fullName, List<Specialty> specialties) {
        if (userId == null) {
            throw new BusinessException("User ID is required");
        }
        if (fullName == null || fullName.trim().isEmpty()) {
            throw new BusinessException("Full name is required");
        }
        if (specialties == null || specialties.isEmpty()) {
            throw new BusinessException("At least one specialty is required");
        }

        User user = userService.getEntityById(userId);
        if (user == null) {
            throw new ResourceNotFoundException("User", userId);
        }

        boolean hasDoctorRole = user.getRoles().stream()
                .anyMatch(role -> role.getName() == UserRole.DOCTOR);

        if (!hasDoctorRole) {
            throw new BusinessException("User must have DOCTOR role");
        }

        if (doctorRepository.existsByUserUserId(userId)) {
            throw new BusinessException("Doctor profile already exists for this user");
        }

        Doctor doctor = Doctor.builder()
            .user(user)
            .fullName(fullName.trim())
            .specialties(specialties)
            .build();

        Doctor savedDoctor = doctorRepository.save(doctor);

        log.info("Doctor profile created: {} for user {}", savedDoctor.getDoctorId(), userId);

        return doctorMapper.toDto(savedDoctor);
    }

    @Transactional(readOnly = true)
    public List<ConsultationTypeDto> getConsultationsForDoctor(UUID doctorId) {
        if (doctorId == null) {
            throw new BusinessException("Doctor ID is required");
        }

        Doctor doctor = doctorRepository.findById(doctorId)
            .orElseThrow(() -> new ResourceNotFoundException("Doctor", doctorId));

        List<ConsultationType> consultations = consultationService
            .getEntitiesBySpecialties(doctor.getSpecialties());

        return consultations.stream()
            .map(consultationMapper::toDto)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DoctorDto findDoctorById(UUID doctorId) {
        return findById(doctorId);
    }

    @Transactional(readOnly = true)
    public DoctorDto findDoctorByUserId(UUID userId) {
        if (userId == null) {
            throw new BusinessException("User ID is required");
        }

        Doctor doctor = doctorRepository.findByUserUserId(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Doctor", "userId: " + userId));

        return doctorMapper.toDto(doctor);
    }

    @Transactional(readOnly = true)
    public List<DoctorDto> findDoctorsBySpecialty(Specialty specialty) {
        if (specialty == null) {
            throw new BusinessException("Specialty is required");
        }

        List<Doctor> doctors = doctorRepository.findBySpecialtiesIn(List.of(specialty));

        return doctors.stream()
            .map(doctorMapper::toDto)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<DoctorDto> search(DoctorFilterCriteria criteria, Pageable pageable) {
        log.debug("Searching doctors with criteria: {} and pageable: {}", criteria, pageable);

        Specification<Doctor> spec = specificationBuilder.build(criteria);
        Page<Doctor> entityPage = doctorRepository.findAll(spec, pageable);
        Page<DoctorDto> dtoPage = entityPage.map(this::toDto);

        log.info("Doctor search returned {} results (page {}/{})",
                dtoPage.getNumberOfElements(),
                dtoPage.getNumber() + 1,
                dtoPage.getTotalPages());

        return dtoPage;
    }

    @Transactional(readOnly = true)
    public void validateDoctorExists(UUID doctorId) {
        validateExists(doctorId);
    }

    /**
     * INTERNAL: Creates a doctor profile linked to an existing user.
     */
    @Transactional
    public DoctorDto createDoctorWithUser(User user, String fullName, List<Specialty> specialties) {
        if (user == null) {
            throw new BusinessException("User is required");
        }
        if (fullName == null || fullName.trim().isEmpty()) {
            throw new BusinessException("Full name is required");
        }
        if (specialties == null || specialties.isEmpty()) {
            throw new BusinessException("At least one specialty is required");
        }

        if (doctorRepository.existsByUserUserId(user.getUserId())) {
            throw new BusinessException("Doctor profile already exists for this user");
        }

        Doctor doctor = Doctor.builder()
                .user(user)
                .fullName(fullName.trim())
                .specialties(specialties)
                .build();

        Doctor savedDoctor = doctorRepository.save(doctor);

        log.info("Doctor profile created: {} for user {}", savedDoctor.getDoctorId(), user.getUserId());

        eventPublisher.publishEvent(new DoctorProfileCreated(
                savedDoctor.getDoctorId(),
                user.getUserId()
        ));

        return doctorMapper.toDto(savedDoctor);
    }
}
