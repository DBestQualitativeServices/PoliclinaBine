package com.example.policlicabine.service;

import com.example.policlicabine.dto.UserDto;
import com.example.policlicabine.dto.UserFilterCriteria;
import com.example.policlicabine.dto.UserProfileDto;
import com.example.policlicabine.entity.Role;
import com.example.policlicabine.entity.User;
import com.example.policlicabine.entity.enums.UserRole;
import com.example.policlicabine.event.NewPatientRegisteredEvent;
import com.example.policlicabine.event.UserCreated;
import com.example.policlicabine.exception.BusinessException;
import com.example.policlicabine.exception.ResourceNotFoundException;
import com.example.policlicabine.mapper.DoctorMapper;
import com.example.policlicabine.mapper.ManagerMapper;
import com.example.policlicabine.mapper.PatientMapper;
import com.example.policlicabine.mapper.UserMapper;
import com.example.policlicabine.repository.RoleRepository;
import com.example.policlicabine.repository.UserRepository;
import com.example.policlicabine.service.base.BaseServiceImpl;
import com.example.policlicabine.specification.UserSpecificationBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional
public class UserService extends BaseServiceImpl<User, UserDto, UUID> {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserMapper userMapper;
    private final DoctorMapper doctorMapper;
    private final PatientMapper patientMapper;
    private final ManagerMapper managerMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final UserSpecificationBuilder specificationBuilder;

    public UserService(UserRepository userRepository, RoleRepository roleRepository,
                      UserMapper userMapper,
                      DoctorMapper doctorMapper,
                      PatientMapper patientMapper,
                      ManagerMapper managerMapper,
                      ApplicationEventPublisher eventPublisher,
                      UserSpecificationBuilder specificationBuilder) {
        super(userRepository, userMapper);
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userMapper = userMapper;
        this.doctorMapper = doctorMapper;
        this.patientMapper = patientMapper;
        this.managerMapper = managerMapper;
        this.eventPublisher = eventPublisher;
        this.specificationBuilder = specificationBuilder;
    }

    @Override
    protected UserDto toDto(User entity) {
        return userMapper.toDto(entity);
    }

    @Override
    protected String getEntityName() {
        return "User";
    }

    @Override
    protected void updateEntityFromDto(User entity, UserDto dto) {
        if (dto.getRoles() != null && !dto.getRoles().isEmpty()) {
            entity.getRoles().clear();
            Set<Role> newRoles = roleRepository.findByNameIn(dto.getRoles());
            newRoles.forEach(entity::addRole);
        }
    }

    public UserDto createUser(String username, Set<UserRole> roleNames) {
        if (username == null || username.trim().isEmpty()) {
            throw new BusinessException("Username is required");
        }
        if (roleNames == null || roleNames.isEmpty()) {
            throw new BusinessException("At least one role is required");
        }

        if (userRepository.existsByUsername(username.trim())) {
            throw new BusinessException("Username already exists");
        }

        Set<Role> roles = roleRepository.findByNameIn(roleNames);

        if (roles.size() != roleNames.size()) {
            throw new BusinessException("One or more roles not found in database");
        }

        User user = User.builder()
            .username(username.trim())
            .build();

        roles.forEach(user::addRole);

        User savedUser = userRepository.save(user);

        eventPublisher.publishEvent(new UserCreated(
            savedUser.getUserId(),
            savedUser.getUsername(),
            roleNames
        ));

        log.info("User created: {} with roles {}", username, roleNames);

        return userMapper.toDto(savedUser);
    }

    @Transactional(readOnly = true)
    public Page<UserDto> search(UserFilterCriteria criteria, Pageable pageable) {
        log.debug("Searching users with criteria: {} and pageable: {}", criteria, pageable);

        Specification<User> spec = specificationBuilder.build(criteria);
        Page<User> entityPage = userRepository.findAll(spec, pageable);
        Page<UserDto> dtoPage = entityPage.map(this::toDto);

        log.info("User search returned {} results (page {}/{})",
                dtoPage.getNumberOfElements(),
                dtoPage.getNumber() + 1,
                dtoPage.getTotalPages());

        return dtoPage;
    }

    @Transactional(readOnly = true)
    public UserDto findUserById(UUID userId) {
        return findById(userId);
    }

    @Transactional(readOnly = true)
    public UserDto findUserByUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new BusinessException("Username is required");
        }

        User user = userRepository.findByUsername(username.trim())
            .orElseThrow(() -> new ResourceNotFoundException("User", "username: " + username));

        return userMapper.toDto(user);
    }

    @Transactional(readOnly = true)
    public List<UserDto> findUsersByRole(UserRole roleName) {
        if (roleName == null) {
            throw new BusinessException("Role is required");
        }

        Role role = roleRepository.findByName(roleName)
            .orElseThrow(() -> new ResourceNotFoundException("Role", roleName.name()));

        List<User> users = new ArrayList<>(role.getUsers());

        return users.stream()
            .map(userMapper::toDto)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public void validateUserExists(UUID userId) {
        validateExists(userId);
    }

    @EventListener
    public void onNewPatientRegistered(NewPatientRegisteredEvent event) {
        log.info("New patient account preparation: Patient {} ({} {}) is ready for user account creation. Email: {}",
                event.patientId(), event.firstName(), event.lastName(), event.email());
    }

    /**
     * Get current user profile with all profile data loaded.
     */
    @Transactional(readOnly = true)
    public UserProfileDto getCurrentUserProfile(UUID userId) {
        if (userId == null) {
            throw new BusinessException("User ID is required");
        }

        User user = userRepository.findWithProfileByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        UserProfileDto profileDto = UserProfileDto.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .roles(user.getRoles().stream()
                        .map(role -> role.getName())
                        .collect(java.util.stream.Collectors.toSet()))
                .doctorProfile(user.getDoctorProfile() != null
                        ? doctorMapper.toDto(user.getDoctorProfile())
                        : null)
                .patientProfile(user.getPatientProfile() != null
                        ? patientMapper.toDto(user.getPatientProfile())
                        : null)
                .managerProfile(user.getManagerProfile() != null
                        ? managerMapper.toDto(user.getManagerProfile())
                        : null)
                .build();

        log.debug("User profile loaded for user {}: type={}", userId, profileDto.getProfileType());

        return profileDto;
    }
}
