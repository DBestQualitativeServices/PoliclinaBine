package com.example.policlicabine.service;

import com.example.policlicabine.common.Result;
import com.example.policlicabine.dto.UserDto;
import com.example.policlicabine.dto.UserFilterCriteria;
import com.example.policlicabine.entity.Role;
import com.example.policlicabine.entity.User;
import com.example.policlicabine.entity.enums.UserRole;
import com.example.policlicabine.event.NewPatientRegisteredEvent;
import com.example.policlicabine.event.UserCreated;
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
    private final ApplicationEventPublisher eventPublisher;
    private final UserSpecificationBuilder specificationBuilder;

    public UserService(UserRepository userRepository, RoleRepository roleRepository,
                      UserMapper userMapper,
                      ApplicationEventPublisher eventPublisher,
                      UserSpecificationBuilder specificationBuilder) {
        super(userRepository, userMapper);
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userMapper = userMapper;
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

    public Result<UserDto> createUser(String username, Set<UserRole> roleNames) {
        try {
            if (username == null || username.trim().isEmpty()) {
                return Result.failure("Username is required");
            }
            if (roleNames == null || roleNames.isEmpty()) {
                return Result.failure("At least one role is required");
            }

            if (userRepository.existsByUsername(username.trim())) {
                return Result.failure("Username already exists");
            }

            Set<Role> roles = roleRepository.findByNameIn(roleNames);

            if (roles.size() != roleNames.size()) {
                return Result.failure("One or more roles not found in database");
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

            return Result.success(userMapper.toDto(savedUser));

        } catch (Exception e) {
            log.error("Error creating user", e);
            return Result.failure("Failed to create user: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public Page<UserDto> search(UserFilterCriteria criteria, Pageable pageable) {
        log.debug("Searching users with criteria: {} and pageable: {}", criteria, pageable);

        try {
            Specification<User> spec = specificationBuilder.build(criteria);
            Page<User> entityPage = userRepository.findAll(spec, pageable);
            Page<UserDto> dtoPage = entityPage.map(this::toDto);

            log.info("User search returned {} results (page {}/{})",
                    dtoPage.getNumberOfElements(),
                    dtoPage.getNumber() + 1,
                    dtoPage.getTotalPages());

            return dtoPage;

        } catch (Exception e) {
            log.error("Error searching users with criteria: {}", criteria, e);
            throw new RuntimeException("Failed to search users: " + e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public Result<UserDto> findUserById(UUID userId) {
        return findById(userId);
    }

    @Transactional(readOnly = true)
    public Result<UserDto> findUserByUsername(String username) {
        try {
            if (username == null || username.trim().isEmpty()) {
                return Result.failure("Username is required");
            }

            User user = userRepository.findByUsername(username.trim()).orElse(null);
            if (user == null) {
                return Result.failure("User not found with username: " + username);
            }

            return Result.success(userMapper.toDto(user));

        } catch (Exception e) {
            log.error("Error finding user by username", e);
            return Result.failure("Failed to find user: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public Result<List<UserDto>> findUsersByRole(UserRole roleName) {
        try {
            if (roleName == null) {
                return Result.failure("Role is required");
            }

            Role role = roleRepository.findByName(roleName).orElse(null);

            if (role == null) {
                return Result.failure("Role not found: " + roleName);
            }

            List<User> users = new ArrayList<>(role.getUsers());

            List<UserDto> userDtos = users.stream()
                .map(userMapper::toDto)
                .collect(Collectors.toList());

            return Result.success(userDtos);

        } catch (Exception e) {
            log.error("Error finding users by role", e);
            return Result.failure("Failed to find users: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public Result<Void> validateUserExists(UUID userId) {
        return validateExists(userId);
    }


    @EventListener
    public void onNewPatientRegistered(NewPatientRegisteredEvent event) {
        log.info("New patient account preparation: Patient {} ({} {}) is ready for user account creation. Email: {}",
                event.patientId(), event.firstName(), event.lastName(), event.email());
    }
}
