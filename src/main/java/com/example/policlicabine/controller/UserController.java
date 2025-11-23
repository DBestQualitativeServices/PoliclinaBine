package com.example.policlicabine.controller;

import com.example.policlicabine.common.Result;
import com.example.policlicabine.dto.ErrorResponse;
import com.example.policlicabine.dto.UserDto;
import com.example.policlicabine.dto.UserFilterCriteria;
import com.example.policlicabine.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
@Tag(
        name = "User Management",
        description = "APIs for system user management, authentication, and authorization"
)
public class UserController {

    private final UserService userService;

    @PostMapping
    public ResponseEntity<?> createUser(
            @Valid @RequestBody UserDto userDto,
            HttpServletRequest request
    ) {
        log.info("REST: Creating new user: {}", userDto.getUsername());

        Result<UserDto> result = userService.createUser(
                userDto.getUsername(),
                userDto.getRoles()
        );

        if (result.isSuccess()) {
            return ResponseEntity.ok(result.getValue());
        } else {
            return ResponseEntity
                    .badRequest()
                    .body(ErrorResponse.of(
                            HttpStatus.BAD_REQUEST.value(),
                            result.getErrorMessage(),
                            request.getRequestURI()
                    ));
        }
    }

    @GetMapping("/{userId}")
    public ResponseEntity<?> getUser(
            @Parameter(description = "User UUID", required = true)
            @PathVariable UUID userId,
            HttpServletRequest request
    ) {
        log.info("REST: Getting user by ID: {}", userId);

        Result<UserDto> result = userService.findById(userId);

        if (result.isSuccess()) {
            return ResponseEntity.ok(result.getValue());
        } else {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(ErrorResponse.of(
                            HttpStatus.NOT_FOUND.value(),
                            result.getErrorMessage(),
                            request.getRequestURI()
                    ));
        }
    }

    @GetMapping("/search")
    public ResponseEntity<Page<UserDto>> searchUsers(
            @Parameter(description = "Filter criteria - all fields are optional flat query parameters")
            @ModelAttribute UserFilterCriteria criteria,
            @ParameterObject
            @Parameter(description = "Pagination and sorting parameters (page, size, sort)")
            @PageableDefault(size = 20, sort = "username")
            Pageable pageable
    ) {
        log.info("REST: Searching users with criteria: {} and pageable: {}", criteria, pageable);

        Page<UserDto> result = userService.search(criteria, pageable);

        log.info("REST: User search returned {} results (page {}/{})",
                result.getNumberOfElements(),
                result.getNumber() + 1,
                result.getTotalPages());

        return ResponseEntity.ok(result);
    }

    @GetMapping
    public ResponseEntity<List<UserDto>> getAllUsers() {
        log.info("REST: Getting all users");

        Result<List<UserDto>> result = userService.findAll();

        return ResponseEntity.ok(result.getValue());
    }

    @PutMapping("/{userId}")
    public ResponseEntity<?> updateUser(
            @Parameter(description = "User UUID", required = true)
            @PathVariable UUID userId,
            @Valid @RequestBody UserDto userDto,
            HttpServletRequest request
    ) {
        log.info("REST: Updating user: {}", userId);

        Result<UserDto> result = userService.update(userId, userDto);

        if (result.isSuccess()) {
            return ResponseEntity.ok(result.getValue());
        } else {
            HttpStatus status = result.getErrorMessage().contains("not found")
                    ? HttpStatus.NOT_FOUND
                    : HttpStatus.BAD_REQUEST;

            return ResponseEntity
                    .status(status)
                    .body(ErrorResponse.of(
                            status.value(),
                            result.getErrorMessage(),
                            request.getRequestURI()
                    ));
        }
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<?> deleteUser(
            @Parameter(description = "User UUID", required = true)
            @PathVariable UUID userId,
            HttpServletRequest request
    ) {
        log.info("REST: Deleting user: {}", userId);

        Result<Void> result = userService.deleteById(userId);

        if (result.isSuccess()) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(ErrorResponse.of(
                            HttpStatus.NOT_FOUND.value(),
                            result.getErrorMessage(),
                            request.getRequestURI()
                    ));
        }
    }
}
