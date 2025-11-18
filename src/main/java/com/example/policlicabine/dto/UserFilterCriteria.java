package com.example.policlicabine.dto;

import com.example.policlicabine.entity.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * Filter criteria DTO for searching and filtering users.
 * <p>
 * This DTO is used as a request parameter in search endpoints to specify
 * filtering conditions. All fields are optional (nullable), and null values
 * are ignored during specification building.
 * </p>
 * <p>
 * Supports:
 * <ul>
 *   <li>Text filters: Partial match, case-insensitive (username, fullName)</li>
 *   <li>Enum filter: Exact match (role)</li>
 *   <li>Boolean filters: enabled, accountNonLocked</li>
 *   <li>Date range filters: createdAfter, createdBefore</li>
 * </ul>
 * </p>
 *
 * @since 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserFilterCriteria {

    /**
     * Filter by username (partial match, case-insensitive).
     * <p>
     * Example: "joh" matches "john.doe", "JOHANNA", "johann_smith"
     * </p>
     */
    private String username;

    /**
     * Filter by full name (partial match, case-insensitive).
     * <p>
     * Example: "doe" matches "John Doe", "JANE DOE", "doerr"
     * </p>
     */
    private String fullName;

    /**
     * Filter by user role (exact match).
     * <p>
     * Example: DOCTOR, RECEPTIONIST, ADMIN
     * </p>
     */
    private UserRole role;

    /**
     * Filter by enabled status.
     * <p>
     * - true: only enabled users
     * - false: only disabled users
     * - null: no filtering on enabled status
     * </p>
     */
    private Boolean enabled;

    /**
     * Filter by account lock status.
     * <p>
     * - true: only unlocked accounts (accountNonLocked = true)
     * - false: only locked accounts (accountNonLocked = false)
     * - null: no filtering on lock status
     * </p>
     */
    private Boolean accountNonLocked;

    /**
     * Filter users created on or after this date/time.
     * <p>
     * Inclusive: users with createdAt >= createdAfter
     * </p>
     */
    private OffsetDateTime createdAfter;

    /**
     * Filter users created on or before this date/time.
     * <p>
     * Inclusive: users with createdAt <= createdBefore
     * </p>
     */
    private OffsetDateTime createdBefore;
}
