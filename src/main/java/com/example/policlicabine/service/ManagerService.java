package com.example.policlicabine.service;

import com.example.policlicabine.dto.ManagerDto;
import com.example.policlicabine.entity.Manager;
import com.example.policlicabine.entity.User;
import com.example.policlicabine.entity.enums.UserRole;
import com.example.policlicabine.event.ManagerProfileCreated;
import com.example.policlicabine.exception.BusinessException;
import com.example.policlicabine.exception.ResourceNotFoundException;
import com.example.policlicabine.mapper.ManagerMapper;
import com.example.policlicabine.repository.ManagerRepository;
import com.example.policlicabine.service.base.BaseServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@Slf4j
@Transactional
public class ManagerService extends BaseServiceImpl<Manager, ManagerDto, UUID> {

    private final ManagerRepository managerRepository;
    private final UserService userService;
    private final ManagerMapper managerMapper;
    private final ApplicationEventPublisher eventPublisher;

    public ManagerService(ManagerRepository managerRepository,
                          UserService userService,
                          ManagerMapper managerMapper,
                          ApplicationEventPublisher eventPublisher) {
        super(managerRepository, managerMapper);
        this.managerRepository = managerRepository;
        this.userService = userService;
        this.managerMapper = managerMapper;
        this.eventPublisher = eventPublisher;
    }

    @Override
    protected ManagerDto toDto(Manager entity) {
        return managerMapper.toDto(entity);
    }

    @Override
    protected String getEntityName() {
        return "Manager";
    }

    @Override
    protected void updateEntityFromDto(Manager entity, ManagerDto dto) {
        if (dto.getFullName() != null && !dto.getFullName().trim().isEmpty()) {
            entity.setFullName(dto.getFullName().trim());
        }
        if (dto.getDepartment() != null && !dto.getDepartment().trim().isEmpty()) {
            entity.setDepartment(dto.getDepartment().trim());
        }
        if (dto.getHireDate() != null) {
            entity.setHireDate(dto.getHireDate());
        }
    }

    public ManagerDto createManager(UUID userId, String fullName, String department, OffsetDateTime hireDate) {
        if (userId == null) {
            throw new BusinessException("User ID is required");
        }

        if (fullName == null || fullName.trim().isEmpty()) {
            throw new BusinessException("Full name is required");
        }

        User user = userService.getEntityById(userId);
        if (user == null) {
            throw new ResourceNotFoundException("User", userId);
        }

        boolean hasManagerRole = user.getRoles().stream()
                .anyMatch(role -> role.getName() == UserRole.MANAGER);

        if (!hasManagerRole) {
            throw new BusinessException("User must have MANAGER role");
        }

        if (managerRepository.existsByUserUserId(userId)) {
            throw new BusinessException("Manager profile already exists for this user");
        }

        Manager manager = Manager.builder()
                .user(user)
                .fullName(fullName.trim())
                .department(department != null ? department.trim() : null)
                .hireDate(hireDate)
                .build();

        Manager savedManager = managerRepository.save(manager);

        // Publish event for role-profile synchronization
        eventPublisher.publishEvent(new ManagerProfileCreated(savedManager.getManagerId(), userId));

        log.info("Manager profile created: {} for user {}", savedManager.getManagerId(), userId);

        return managerMapper.toDto(savedManager);
    }

    @Transactional(readOnly = true)
    public ManagerDto findManagerById(UUID managerId) {
        return findById(managerId);
    }

    @Transactional(readOnly = true)
    public ManagerDto findManagerByUserId(UUID userId) {
        if (userId == null) {
            throw new BusinessException("User ID is required");
        }

        Manager manager = managerRepository.findByUserUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Manager for user", userId));

        return managerMapper.toDto(manager);
    }

    @Transactional(readOnly = true)
    public void validateManagerExists(UUID managerId) {
        validateExists(managerId);
    }

    /**
     * Returns the total count of managers in the system.
     * Used for initialization checks.
     *
     * @return total count of managers
     */
    @Transactional(readOnly = true)
    public long count() {
        return managerRepository.count();
    }

    /**
     * INTERNAL: Creates a manager profile linked to an existing user.
     * Used by AuthenticationService during manager registration with user account.
     *
     * @param user User entity (already saved)
     * @param fullName Manager full name
     * @return ManagerDto
     */
    @Transactional
    public ManagerDto createManagerWithUser(User user, String fullName) {
        if (user == null) {
            throw new BusinessException("User is required");
        }
        if (fullName == null || fullName.trim().isEmpty()) {
            throw new BusinessException("Full name is required");
        }

        // Check for duplicate manager profile for this user
        if (managerRepository.existsByUserUserId(user.getUserId())) {
            throw new BusinessException("Manager profile already exists for this user");
        }

        // Build manager entity
        Manager manager = Manager.builder()
                .user(user)
                .fullName(fullName.trim())
                .build();

        Manager savedManager = managerRepository.save(manager);

        log.info("Manager profile created: {} for user {}", savedManager.getManagerId(), user.getUserId());

        // Publish event
        eventPublisher.publishEvent(new ManagerProfileCreated(
                savedManager.getManagerId(),
                user.getUserId()
        ));

        return managerMapper.toDto(savedManager);
    }
}
