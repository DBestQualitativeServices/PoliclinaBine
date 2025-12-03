package com.example.policlicabine.controller;

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
        return managerService.createManager(
                managerDto.getUser().getUserId(),
                managerDto.getFullName(),
                managerDto.getDepartment(),
                managerDto.getHireDate()
        );
    }

    @GetMapping("/{managerId}")
    @StandardApiResponses
    @SecurityRequirement(name = "bearer-jwt")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Get manager by ID")
    public ManagerDto getManager(@PathVariable UUID managerId) {
        log.info("REST: Getting manager by ID: {}", managerId);
        return managerService.findById(managerId);
    }

    @GetMapping
    @StandardApiResponses
    @SecurityRequirement(name = "bearer-jwt")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Get all managers")
    public List<ManagerDto> getAllManagers() {
        log.info("REST: Getting all managers");
        return managerService.findAll();
    }

    @GetMapping("/user/{userId}")
    @StandardApiResponses
    @SecurityRequirement(name = "bearer-jwt")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Get manager by user ID")
    public ManagerDto getManagerByUserId(@PathVariable UUID userId) {
        log.info("REST: Getting manager by user ID: {}", userId);
        return managerService.findManagerByUserId(userId);
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
        return managerService.update(managerId, managerDto);
    }

    @DeleteMapping("/{managerId}")
    @StandardApiResponses
    @SecurityRequirement(name = "bearer-jwt")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete manager profile")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteManager(@PathVariable UUID managerId) {
        log.info("REST: Deleting manager: {}", managerId);
        managerService.deleteById(managerId);
    }
}
