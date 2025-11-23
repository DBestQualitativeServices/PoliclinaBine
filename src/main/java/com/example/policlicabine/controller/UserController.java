package com.example.policlicabine.controller;

import com.example.policlicabine.common.Result;
import com.example.policlicabine.common.StandardApiResponses;
import com.example.policlicabine.dto.UserDto;
import com.example.policlicabine.dto.UserFilterCriteria;
import com.example.policlicabine.dto.UserProfileDto;
import com.example.policlicabine.exception.BusinessException;
import com.example.policlicabine.exception.ResourceNotFoundException;
import com.example.policlicabine.exception.UnauthorizedException;
import com.example.policlicabine.security.JwtService;
import com.example.policlicabine.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "User Management")
public class UserController {

    private final UserService userService;
    private final JwtService jwtService;

    @PostMapping
    @StandardApiResponses
    @Operation(summary = "Create new user", description = "Create a new system user with specified roles")
    public UserDto createUser(@Valid @RequestBody UserDto userDto) {
        log.info("REST: Creating new user: {}", userDto.getUsername());

        Result<UserDto> result = userService.createUser(
                userDto.getUsername(),
                userDto.getRoles()
        );

        if (result.isFailure()) {
            throw new BusinessException(result.getErrorMessage());
        }

        return result.getValue();
    }

    @GetMapping("/{userId}")
    @StandardApiResponses
    @Operation(summary = "Get user by ID")
    public UserDto getUser(@PathVariable UUID userId) {
        log.info("REST: Getting user by ID: {}", userId);

        Result<UserDto> result = userService.findById(userId);

        if (result.isFailure()) {
            throw new ResourceNotFoundException("User", userId);
        }

        return result.getValue();
    }

    @GetMapping("/search")
    @StandardApiResponses
    @Operation(summary = "Search users")
    public Page<UserDto> searchUsers(
            @ModelAttribute UserFilterCriteria criteria,
            @ParameterObject @PageableDefault(size = 20, sort = "username") Pageable pageable
    ) {
        log.info("REST: Searching users with criteria: {} and pageable: {}", criteria, pageable);
        return userService.search(criteria, pageable);
    }

    @GetMapping
    @StandardApiResponses
    @Operation(summary = "Get all users")
    public List<UserDto> getAllUsers() {
        log.info("REST: Getting all users");
        return userService.findAll().getValue();
    }

    @PutMapping("/{userId}")
    @StandardApiResponses
    @Operation(summary = "Update user")
    public UserDto updateUser(
            @PathVariable UUID userId,
            @Valid @RequestBody UserDto userDto
    ) {
        log.info("REST: Updating user: {}", userId);

        Result<UserDto> result = userService.update(userId, userDto);

        if (result.isFailure()) {
            if (result.getErrorMessage().contains("not found")) {
                throw new ResourceNotFoundException("User", userId);
            }
            throw new BusinessException(result.getErrorMessage());
        }

        return result.getValue();
    }

    @DeleteMapping("/{userId}")
    @StandardApiResponses
    @Operation(summary = "Delete user")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable UUID userId) {
        log.info("REST: Deleting user: {}", userId);

        Result<Void> result = userService.deleteById(userId);

        if (result.isFailure()) {
            throw new ResourceNotFoundException("User", userId);
        }
    }

    @GetMapping("/me")
    @StandardApiResponses
    @Operation(summary = "Get current user profile")
    public UserProfileDto getCurrentUserProfile(HttpServletRequest request) {
        try {
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                throw new UnauthorizedException("Missing or invalid Authorization header");
            }

            String jwt = authHeader.substring(7);
            String userIdStr = jwtService.extractUserId(jwt);

            if (userIdStr == null) {
                throw new UnauthorizedException("Invalid token: userId claim not found");
            }

            UUID userId = UUID.fromString(userIdStr);
            log.info("REST: Getting current user profile for userId: {}", userId);

            Result<UserProfileDto> result = userService.getCurrentUserProfile(userId);

            if (result.isFailure()) {
                throw new ResourceNotFoundException("User", userId);
            }

            return result.getValue();
        } catch (IllegalArgumentException e) {
            log.warn("Invalid userId format in JWT token: {}", e.getMessage());
            throw new UnauthorizedException("Invalid token: userId format is invalid");
        } catch (UnauthorizedException | ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error getting current user profile: {}", e.getMessage(), e);
            throw new BusinessException("Failed to retrieve user profile");
        }
    }
}
