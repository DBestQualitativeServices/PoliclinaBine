package com.example.policlicabine.controller;

import com.example.policlicabine.builder.UserTestBuilder;
import com.example.policlicabine.entity.User;
import com.example.policlicabine.entity.enums.UserRole;
import com.example.policlicabine.repository.AppointmentSessionRepository;
import com.example.policlicabine.repository.DoctorRepository;
import com.example.policlicabine.repository.UserRepository;
import com.example.policlicabine.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import com.example.policlicabine.dto.UserDto;
import com.example.policlicabine.dto.UserFilterCriteria;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for User search functionality with pagination.
 * <p>
 * Tests the complete flow from service to database query and response.
 * Uses an in-memory H2 database for testing.
 * </p>
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("User Search Integration Tests")
class UserControllerSearchIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private AppointmentSessionRepository appointmentRepository;

    @BeforeEach
    void setUp() {
        // Clean database before each test - correct order to avoid FK violations
        appointmentRepository.deleteAll();  // Delete appointments first (references doctors)
        doctorRepository.deleteAll();        // Delete doctors second (references users)
        userRepository.deleteAll();          // Delete users last

        // Create test users with different attributes
        User doctor1 = UserTestBuilder.aDoctor()
                .withUsername("dr.john.doe")
                .withFullName("Dr. John Doe")
                .withRole(UserRole.DOCTOR)
                .withEnabled(true)
                .withAccountNonLocked(true)
                .withCreatedAt(OffsetDateTime.now(ZoneOffset.UTC).minusDays(10))
                .build();

        User doctor2 = UserTestBuilder.aDoctor()
                .withUsername("dr.jane.smith")
                .withFullName("Dr. Jane Smith")
                .withRole(UserRole.DOCTOR)
                .withEnabled(true)
                .withAccountNonLocked(true)
                .withCreatedAt(OffsetDateTime.now(ZoneOffset.UTC).minusDays(8))
                .build();

        User receptionist = UserTestBuilder.aReceptionist()
                .withUsername("jane.receptionist")
                .withFullName("Jane Receptionist")
                .withRole(UserRole.RECEPTIONIST)
                .withEnabled(true)
                .withAccountNonLocked(true)
                .withCreatedAt(OffsetDateTime.now(ZoneOffset.UTC).minusDays(5))
                .build();

        User admin = UserTestBuilder.anAdmin()
                .withUsername("admin.user")
                .withFullName("Admin User")
                .withRole(UserRole.ADMIN)
                .withEnabled(false)
                .withAccountNonLocked(false)
                .withCreatedAt(OffsetDateTime.now(ZoneOffset.UTC).minusDays(1))
                .build();

        User disabledDoctor = UserTestBuilder.aDoctor()
                .withUsername("dr.disabled")
                .withFullName("Dr. Disabled")
                .withRole(UserRole.DOCTOR)
                .withEnabled(false)
                .withAccountNonLocked(true)
                .withCreatedAt(OffsetDateTime.now(ZoneOffset.UTC).minusDays(3))
                .build();

        // Save all test users
        userRepository.save(doctor1);
        userRepository.save(doctor2);
        userRepository.save(receptionist);
        userRepository.save(admin);
        userRepository.save(disabledDoctor);
    }

    @Test
    @DisplayName("Should search users with username filter and return results")
    void searchUsers_WithUsernameFilter_ReturnsResults() {
        // Given
        UserFilterCriteria criteria = UserFilterCriteria.builder()
                .username("dr.john")
                .build();
        Pageable pageable = PageRequest.of(0, 20);

        // When
        Page<UserDto> result = userService.search(criteria, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getUsername()).isEqualTo("dr.john.doe");
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getTotalPages()).isEqualTo(1);
        assertThat(result.getNumber()).isEqualTo(0);
        assertThat(result.getSize()).isEqualTo(20);
    }

    @Test
    @DisplayName("Should search with role=DOCTOR and return only doctors")
    void searchUsers_WithRoleDoctor_ReturnsOnlyDoctors() {
        // Given
        UserFilterCriteria criteria = UserFilterCriteria.builder()
                .role(UserRole.DOCTOR)
                .build();
        Pageable pageable = PageRequest.of(0, 20);

        // When
        Page<UserDto> result = userService.search(criteria, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(3); // 2 enabled + 1 disabled
        assertThat(result.getContent()).allMatch(user -> user.getRole() == UserRole.DOCTOR);
        assertThat(result.getTotalElements()).isEqualTo(3);
    }

    @Test
    @DisplayName("Should search with pagination params (page=1, size=2)")
    void searchUsers_WithPaginationParams_ReturnsPaginatedResults() {
        // Given
        UserFilterCriteria criteria = UserFilterCriteria.builder().build();
        Pageable pageable = PageRequest.of(1, 2, org.springframework.data.domain.Sort.by("username").ascending());

        // When
        Page<UserDto> result = userService.search(criteria, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2); // Page 1 with size 2
        assertThat(result.getTotalElements()).isEqualTo(5);
        assertThat(result.getTotalPages()).isEqualTo(3); // 5 users / 2 per page = 3 pages
        assertThat(result.getNumber()).isEqualTo(1);
        assertThat(result.getSize()).isEqualTo(2);
        assertThat(result.isFirst()).isFalse();
        assertThat(result.isLast()).isFalse();
    }

    @Test
    @DisplayName("Should search with sorting (sort=username,desc)")
    void searchUsers_WithSorting_ReturnsSortedResults() {
        // Given
        UserFilterCriteria criteria = UserFilterCriteria.builder().build();
        Pageable pageable = PageRequest.of(0, 20, org.springframework.data.domain.Sort.by("username").descending());

        // When
        Page<UserDto> result = userService.search(criteria, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(5);
        assertThat(result.getSort().isSorted()).isTrue();
    }

    @Test
    @DisplayName("Should return proper Page structure")
    void searchUsers_Response_HasProperPageStructure() {
        // Given
        UserFilterCriteria criteria = UserFilterCriteria.builder().build();
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<UserDto> result = userService.search(criteria, pageable);

        // Then - Verify Page structure
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isNotNull();
        assertThat(result.getPageable()).isNotNull();
        assertThat(result.getTotalElements()).isNotNull();
        assertThat(result.getTotalPages()).isNotNull();
        assertThat(result.getNumber()).isNotNull();
        assertThat(result.getSize()).isNotNull();
        assertThat(result.getSort()).isNotNull();
        assertThat(result.isFirst()).isNotNull();
        assertThat(result.isLast()).isNotNull();
        assertThat(result.getNumberOfElements()).isNotNull();
        assertThat(result.isEmpty()).isNotNull();
    }

    @Test
    @DisplayName("Should search with multiple filters combined")
    void searchUsers_WithMultipleFilters_ReturnsCombinedResults() {
        // Given
        UserFilterCriteria criteria = UserFilterCriteria.builder()
                .role(UserRole.DOCTOR)
                .enabled(true)
                .accountNonLocked(true)
                .build();
        Pageable pageable = PageRequest.of(0, 20);

        // When
        Page<UserDto> result = userService.search(criteria, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2); // Only 2 enabled, unlocked doctors
        assertThat(result.getContent()).allMatch(user -> user.getRole() == UserRole.DOCTOR);
        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should search with no filters and return all users")
    void searchUsers_WithNoFilters_ReturnsAllUsers() {
        // Given
        UserFilterCriteria criteria = UserFilterCriteria.builder().build();
        Pageable pageable = PageRequest.of(0, 20);

        // When
        Page<UserDto> result = userService.search(criteria, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(5); // All 5 test users
        assertThat(result.getTotalElements()).isEqualTo(5);
        assertThat(result.getTotalPages()).isEqualTo(1);
        assertThat(result.isFirst()).isTrue();
        assertThat(result.isLast()).isTrue();
    }

    @Test
    @DisplayName("Should return empty page when no users match criteria")
    void searchUsers_NoMatches_ReturnsEmptyPage() {
        // Given
        UserFilterCriteria criteria = UserFilterCriteria.builder()
                .username("nonexistent.user")
                .build();
        Pageable pageable = PageRequest.of(0, 20);

        // When
        Page<UserDto> result = userService.search(criteria, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
        assertThat(result.getTotalPages()).isEqualTo(0);
        assertThat(result.isEmpty()).isTrue();
    }

    @Test
    @DisplayName("Should search with fullName filter (case-insensitive)")
    void searchUsers_WithFullNameFilter_ReturnsCaseInsensitiveResults() {
        // Given
        UserFilterCriteria criteria = UserFilterCriteria.builder()
                .fullName("doe") // Should match "Dr. John Doe"
                .build();
        Pageable pageable = PageRequest.of(0, 20);

        // When
        Page<UserDto> result = userService.search(criteria, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getFullName()).isEqualTo("Dr. John Doe");
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should search with enabled=false filter")
    void searchUsers_WithEnabledFalseFilter_ReturnsDisabledUsers() {
        // Given
        UserFilterCriteria criteria = UserFilterCriteria.builder()
                .enabled(false)
                .build();
        Pageable pageable = PageRequest.of(0, 20);

        // When
        Page<UserDto> result = userService.search(criteria, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2); // admin + disabled doctor
        assertThat(result.getTotalElements()).isEqualTo(2);
    }
}
