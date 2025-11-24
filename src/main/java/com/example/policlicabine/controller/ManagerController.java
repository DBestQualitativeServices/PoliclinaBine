package com.example.policlicabine.controller;

import com.example.policlicabine.common.Result;
import com.example.policlicabine.common.StandardApiResponses;
import com.example.policlicabine.dto.ManagerDto;
import com.example.policlicabine.exception.BusinessException;
import com.example.policlicabine.exception.ResourceNotFoundException;
import com.example.policlicabine.service.ManagerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/managers")
@RequiredArgsConstructor
@Slf4j
@Tag(
        name = "Manager Management",
        description = "APIs for manager profile management"
)
public class ManagerController {

    private final ManagerService managerService;

    @PostMapping
    @StandardApiResponses
    @SecurityRequirement(name = "bearer-jwt")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create manager profile (admin only - for linking existing users)")
    public ManagerDto createManager(@Valid @RequestBody ManagerDto managerDto) {
        log.info("REST: Creating new manager profile for user: {}", managerDto.getUser().getUserId());

        Result<ManagerDto> result = managerService.createManager(
                managerDto.getUser().getUserId(),
                managerDto.getFullName(),
                managerDto.getDepartment(),
                managerDto.getHireDate()
        );

        if (result.isFailure()) {
            throw new BusinessException(result.getErrorMessage());
        }

        return result.getValue();
    }

    @GetMapping("/{managerId}")
    @StandardApiResponses
    @SecurityRequirement(name = "bearer-jwt")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Get manager by ID")
    public ManagerDto getManager(@PathVariable UUID managerId) {
        log.info("REST: Getting manager by ID: {}", managerId);

        Result<ManagerDto> result = managerService.findById(managerId);

        if (result.isFailure()) {
            throw new ResourceNotFoundException("Manager", managerId);
        }

        return result.getValue();
    }

    @GetMapping
    @StandardApiResponses
    @SecurityRequirement(name = "bearer-jwt")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Get all managers")
    public List<ManagerDto> getAllManagers() {
        log.info("REST: Getting all managers");
        return managerService.findAll().getValue();
    }

    @GetMapping("/user/{userId}")
    @StandardApiResponses
    @SecurityRequirement(name = "bearer-jwt")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Get manager by user ID")
    public ManagerDto getManagerByUserId(@PathVariable UUID userId) {
        log.info("REST: Getting manager by user ID: {}", userId);

        Result<ManagerDto> result = managerService.findManagerByUserId(userId);

        if (result.isFailure()) {
            throw new ResourceNotFoundException("Manager not found for user: " + userId);
        }

        return result.getValue();
    }

    @PutMapping("/{managerId}")
    @StandardApiResponses
    @SecurityRequirement(name = "bearer-jwt")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update manager profile")
    public ManagerDto updateManager(
            @PathVariable UUID managerId,
            @Valid @RequestBody ManagerDto managerDto
    ) {
        log.info("REST: Updating manager: {}", managerId);

        Result<ManagerDto> result = managerService.update(managerId, managerDto);

        if (result.isFailure()) {
            if (result.getErrorMessage().contains("not found")) {
                throw new ResourceNotFoundException("Manager", managerId);
            }
            throw new BusinessException(result.getErrorMessage());
        }

        return result.getValue();
    }

    @DeleteMapping("/{managerId}")
    @StandardApiResponses
    @SecurityRequirement(name = "bearer-jwt")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete manager profile")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteManager(@PathVariable UUID managerId) {
        log.info("REST: Deleting manager: {}", managerId);

        Result<Void> result = managerService.deleteById(managerId);

        if (result.isFailure()) {
            throw new ResourceNotFoundException("Manager", managerId);
        }
    }
}
