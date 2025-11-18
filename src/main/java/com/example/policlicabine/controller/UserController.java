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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller for User Management Operations.
 *
 * Provides CRUD endpoints for system user creation, retrieval,
 * update, and deletion. All operations use the UserService
 * for business logic.
 */
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

    @Operation(
            summary = "Create a new user",
            description = """
                    Creates a new system user account.

                    **Business Rules:**
                    - Username must be unique
                    - Password is required (will be hashed)
                    - Role must be valid (ADMIN, DOCTOR, RECEPTIONIST)
                    - Email must be unique (if provided)
                    - Publishes UserCreated domain event on success
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "User created successfully",
                    content = @Content(schema = @Schema(implementation = UserDto.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid input data or duplicate username/email",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PostMapping
    public ResponseEntity<?> createUser(
            @Valid @RequestBody UserDto userDto,
            HttpServletRequest request
    ) {
        log.info("REST: Creating new user: {}", userDto.getUsername());

        Result<UserDto> result = userService.createUser(
                userDto.getUsername(),
                userDto.getFullName(),
                userDto.getRole()
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

    @Operation(
            summary = "Get user by ID",
            description = "Retrieves a user's profile by their unique UUID identifier"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "User found",
                    content = @Content(schema = @Schema(implementation = UserDto.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "User not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
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

    @Operation(
            summary = "Search users with filters",
            description = """
                    Searches and filters users with pagination and sorting support.

                    **Query Parameter Format:**
                    All parameters are flat query parameters (no nesting required).
                    Simply append filters and pagination params directly to the URL.

                    **Filter Options (all optional):**
                    - `username` - Partial match, case-insensitive (e.g., "joh" matches "john.doe")
                    - `fullName` - Partial match, case-insensitive (e.g., "doe" matches "John Doe")
                    - `role` - Exact match (values: DOCTOR, RECEPTIONIST, ADMIN)
                    - `enabled` - Boolean: true = enabled users, false = disabled users
                    - `accountNonLocked` - Boolean: true = unlocked accounts, false = locked accounts
                    - `createdAfter` - Filter users created on or after this date (ISO 8601 format)
                    - `createdBefore` - Filter users created on or before this date (ISO 8601 format)

                    **Pagination Parameters:**
                    - `page` - Page number (0-indexed, default: 0)
                    - `size` - Page size (default: 20, max: 100)
                    - `sort` - Sort criteria (e.g., "username,asc" or "createdAt,desc")

                    **Examples:**
                    - `/api/users/search?username=john&page=0&size=10`
                    - `/api/users/search?role=DOCTOR&sort=username,asc`
                    - `/api/users/search?enabled=true&accountNonLocked=true`
                    - `/api/users/search?fullName=doe&role=DOCTOR&page=1&size=50`
                    - `/api/users/search?createdAfter=2025-01-01T00:00:00Z&createdBefore=2025-12-31T23:59:59Z`
                    """
    )
    @ApiResponse(
            responseCode = "200",
            description = "Paginated user results with metadata"
    )
    @GetMapping("/search")
    public ResponseEntity<Page<UserDto>> searchUsers(
            @Parameter(description = "Filter criteria - all fields are optional flat query parameters")
            @ModelAttribute UserFilterCriteria criteria,

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

    @Operation(
            summary = "Get all users",
            description = "Retrieves a list of all system users"
    )
    @ApiResponse(
            responseCode = "200",
            description = "List of users retrieved successfully"
    )
    @GetMapping
    public ResponseEntity<List<UserDto>> getAllUsers() {
        log.info("REST: Getting all users");

        Result<List<UserDto>> result = userService.findAll();

        return ResponseEntity.ok(result.getValue());
    }

    @Operation(
            summary = "Update user information",
            description = """
                    Updates mutable fields of an existing user account.

                    **Mutable Fields:**
                    - Password (will be hashed)
                    - Email
                    - Role
                    - Is active status

                    **Immutable Fields:**
                    - User ID
                    - Username (cannot be changed after creation)
                    - Created timestamp
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "User updated successfully",
                    content = @Content(schema = @Schema(implementation = UserDto.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "User not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid input data",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
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

    @Operation(
            summary = "Delete user",
            description = """
                    Permanently deletes a user account from the system.

                    **Warning:** This operation cannot be undone.
                    Use with caution and ensure proper authorization.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "User deleted successfully"),
            @ApiResponse(
                    responseCode = "404",
                    description = "User not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
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
