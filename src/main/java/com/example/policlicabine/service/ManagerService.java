package com.example.policlicabine.service;

import com.example.policlicabine.common.Result;
import com.example.policlicabine.dto.ManagerDto;
import com.example.policlicabine.entity.Manager;
import com.example.policlicabine.entity.User;
import com.example.policlicabine.entity.enums.UserRole;
import com.example.policlicabine.mapper.ManagerMapper;
import com.example.policlicabine.repository.ManagerRepository;
import com.example.policlicabine.service.base.BaseServiceImpl;
import lombok.extern.slf4j.Slf4j;
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

    public ManagerService(ManagerRepository managerRepository,
                          UserService userService,
                          ManagerMapper managerMapper) {
        super(managerRepository, managerMapper);
        this.managerRepository = managerRepository;
        this.userService = userService;
        this.managerMapper = managerMapper;
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

    public Result<ManagerDto> createManager(UUID userId, String fullName, String department, OffsetDateTime hireDate) {
        try {
            if (userId == null) {
                return Result.failure("User ID is required");
            }

            if (fullName == null || fullName.trim().isEmpty()) {
                return Result.failure("Full name is required");
            }

            User user = userService.getEntityById(userId);
            if (user == null) {
                return Result.failure("User not found");
            }

            boolean hasManagerRole = user.getRoles().stream()
                    .anyMatch(role -> role.getName() == UserRole.MANAGER);

            if (!hasManagerRole) {
                return Result.failure("User must have MANAGER role");
            }

            if (managerRepository.existsByUserUserId(userId)) {
                return Result.failure("Manager profile already exists for this user");
            }

            Manager manager = Manager.builder()
                    .user(user)
                    .fullName(fullName.trim())
                    .department(department != null ? department.trim() : null)
                    .hireDate(hireDate)
                    .build();

            Manager savedManager = managerRepository.save(manager);

            log.info("Manager profile created: {} for user {}", savedManager.getManagerId(), userId);

            return Result.success(managerMapper.toDto(savedManager));

        } catch (Exception e) {
            log.error("Error creating manager", e);
            return Result.failure("Failed to create manager: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public Result<ManagerDto> findManagerById(UUID managerId) {
        return findById(managerId);
    }

    @Transactional(readOnly = true)
    public Result<ManagerDto> findManagerByUserId(UUID userId) {
        try {
            if (userId == null) {
                return Result.failure("User ID is required");
            }

            Manager manager = managerRepository.findByUserUserId(userId)
                    .orElse(null);
            if (manager == null) {
                return Result.failure("Manager not found for user");
            }

            return Result.success(managerMapper.toDto(manager));

        } catch (Exception e) {
            log.error("Error finding manager by user ID", e);
            return Result.failure("Failed to find manager: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public Result<Void> validateManagerExists(UUID managerId) {
        return validateExists(managerId);
    }
}
