package com.example.policlicabine.service;

import com.example.policlicabine.base.BaseServiceTest;
import com.example.policlicabine.builder.UserTestBuilder;
import com.example.policlicabine.dto.UserDto;
import com.example.policlicabine.dto.UserFilterCriteria;
import com.example.policlicabine.entity.User;
import com.example.policlicabine.entity.enums.UserRole;
import com.example.policlicabine.mapper.UserMapper;
import com.example.policlicabine.repository.RoleRepository;
import com.example.policlicabine.repository.UserRepository;
import com.example.policlicabine.specification.UserSpecificationBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for UserService search functionality with pagination and filtering.
 * <p>
 * Tests the search method that uses UserSpecificationBuilder and pagination.
 * Follows the Given-When-Then pattern and uses Mockito for mocking dependencies.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Search Tests")
class UserServiceSearchTest extends BaseServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private com.example.policlicabine.mapper.DoctorMapper doctorMapper;

    @Mock
    private com.example.policlicabine.mapper.PatientMapper patientMapper;

    @Mock
    private com.example.policlicabine.mapper.ManagerMapper managerMapper;

    @Mock
    private UserSpecificationBuilder specificationBuilder;

    private UserService userService;

    private User testUser1;
    private User testUser2;
    private User testUser3;
    private UserDto testUserDto1;
    private UserDto testUserDto2;
    private UserDto testUserDto3;

    @BeforeEach
    void setUp() {
        eventPublisher = createEventPublisher();
        userService = new UserService(userRepository, roleRepository, userMapper,
                doctorMapper, patientMapper, managerMapper, eventPublisher, specificationBuilder);

        // Create test users with different attributes
        testUser1 = UserTestBuilder.aUser()
                .withUsername("john.doe")
                .withRoles(Set.of(UserRole.DOCTOR))
                .withEnabled(true)
                .withAccountNonLocked(true)
                .withCreatedAt(OffsetDateTime.now(ZoneOffset.UTC).minusDays(10))
                .build();

        testUser2 = UserTestBuilder.aUser()
                .withUsername("jane.smith")
                .withRoles(Set.of(UserRole.RECEPTIONIST))
                .withEnabled(true)
                .withAccountNonLocked(true)
                .withCreatedAt(OffsetDateTime.now(ZoneOffset.UTC).minusDays(5))
                .build();

        testUser3 = UserTestBuilder.aUser()
                .withUsername("admin.user")
                .withRoles(Set.of(UserRole.ADMIN))
                .withEnabled(false)
                .withAccountNonLocked(false)
                .withCreatedAt(OffsetDateTime.now(ZoneOffset.UTC).minusDays(1))
                .build();

        // Create corresponding DTOs
        testUserDto1 = UserDto.builder()
                .userId(testUser1.getUserId())
                .username(testUser1.getUsername())
                .roles(Set.of(UserRole.DOCTOR))
                .build();

        testUserDto2 = UserDto.builder()
                .userId(testUser2.getUserId())
                .username(testUser2.getUsername())
                .roles(Set.of(UserRole.RECEPTIONIST))
                .build();

        testUserDto3 = UserDto.builder()
                .userId(testUser3.getUserId())
                .username(testUser3.getUsername())
                .roles(Set.of(UserRole.ADMIN))
                .build();
    }

    @Test
    @DisplayName("Should search users with username filter and pagination")
    void search_WithUsernameFilter_Success() {
        // Given
        UserFilterCriteria criteria = UserFilterCriteria.builder()
                .username("john")
                .build();
        Pageable pageable = PageRequest.of(0, 20, Sort.by("username"));

        Specification<User> mockSpec = mock(Specification.class);
        Page<User> entityPage = new PageImpl<>(
                List.of(testUser1), pageable, 1
        );

        when(specificationBuilder.build(criteria)).thenReturn(mockSpec);
        when(userRepository.findAll(mockSpec, pageable)).thenReturn(entityPage);
        when(userMapper.toDto(testUser1)).thenReturn(testUserDto1);

        // When
        Page<UserDto> result = userService.search(criteria, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getTotalPages()).isEqualTo(1);
        assertThat(result.getNumber()).isEqualTo(0);
        assertThat(result.getSize()).isEqualTo(20);
        assertThat(result.getContent().get(0).getUsername()).isEqualTo("john.doe");

        verify(specificationBuilder).build(criteria);
        verify(userRepository).findAll(mockSpec, pageable);
        verify(userMapper).toDto(testUser1);
    }

    @Test
    @DisplayName("Should search users with role filter")
    void search_WithRoleFilter_Success() {
        // Given
        UserFilterCriteria criteria = UserFilterCriteria.builder()
                .role(UserRole.DOCTOR)
                .build();
        Pageable pageable = PageRequest.of(0, 20);

        Specification<User> mockSpec = mock(Specification.class);
        Page<User> entityPage = new PageImpl<>(
                List.of(testUser1), pageable, 1
        );

        when(specificationBuilder.build(criteria)).thenReturn(mockSpec);
        when(userRepository.findAll(mockSpec, pageable)).thenReturn(entityPage);
        when(userMapper.toDto(testUser1)).thenReturn(testUserDto1);

        // When
        Page<UserDto> result = userService.search(criteria, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getRoles()).contains(UserRole.DOCTOR);

        verify(specificationBuilder).build(criteria);
        verify(userRepository).findAll(mockSpec, pageable);
    }

    @Test
    @DisplayName("Should search users with multiple filters")
    void search_WithMultipleFilters_Success() {
        // Given
        UserFilterCriteria criteria = UserFilterCriteria.builder()
                .username("john")
                .role(UserRole.DOCTOR)
                .enabled(true)
                .accountNonLocked(true)
                .build();
        Pageable pageable = PageRequest.of(0, 20);

        Specification<User> mockSpec = mock(Specification.class);
        Page<User> entityPage = new PageImpl<>(
                List.of(testUser1), pageable, 1
        );

        when(specificationBuilder.build(criteria)).thenReturn(mockSpec);
        when(userRepository.findAll(mockSpec, pageable)).thenReturn(entityPage);
        when(userMapper.toDto(testUser1)).thenReturn(testUserDto1);

        // When
        Page<UserDto> result = userService.search(criteria, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getUsername()).isEqualTo("john.doe");
        assertThat(result.getContent().get(0).getRoles()).contains(UserRole.DOCTOR);

        verify(specificationBuilder).build(criteria);
        verify(userRepository).findAll(mockSpec, pageable);
    }

    @Test
    @DisplayName("Should search users with date range filters")
    void search_WithDateRangeFilters_Success() {
        // Given
        OffsetDateTime startDate = OffsetDateTime.now(ZoneOffset.UTC).minusDays(7);
        OffsetDateTime endDate = OffsetDateTime.now(ZoneOffset.UTC);

        UserFilterCriteria criteria = UserFilterCriteria.builder()
                .createdAfter(startDate)
                .createdBefore(endDate)
                .build();
        Pageable pageable = PageRequest.of(0, 20);

        Specification<User> mockSpec = mock(Specification.class);
        Page<User> entityPage = new PageImpl<>(
                List.of(testUser2, testUser3), pageable, 2
        );

        when(specificationBuilder.build(criteria)).thenReturn(mockSpec);
        when(userRepository.findAll(mockSpec, pageable)).thenReturn(entityPage);
        when(userMapper.toDto(testUser2)).thenReturn(testUserDto2);
        when(userMapper.toDto(testUser3)).thenReturn(testUserDto3);

        // When
        Page<UserDto> result = userService.search(criteria, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);

        verify(specificationBuilder).build(criteria);
        verify(userRepository).findAll(mockSpec, pageable);
    }

    @Test
    @DisplayName("Should search with empty criteria and return all users")
    void search_WithEmptyCriteria_ReturnsAllUsers() {
        // Given
        UserFilterCriteria criteria = UserFilterCriteria.builder().build();
        Pageable pageable = PageRequest.of(0, 20);

        Specification<User> mockSpec = mock(Specification.class);
        Page<User> entityPage = new PageImpl<>(
                List.of(testUser1, testUser2, testUser3), pageable, 3
        );

        when(specificationBuilder.build(criteria)).thenReturn(mockSpec);
        when(userRepository.findAll(mockSpec, pageable)).thenReturn(entityPage);
        when(userMapper.toDto(testUser1)).thenReturn(testUserDto1);
        when(userMapper.toDto(testUser2)).thenReturn(testUserDto2);
        when(userMapper.toDto(testUser3)).thenReturn(testUserDto3);

        // When
        Page<UserDto> result = userService.search(criteria, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(3);
        assertThat(result.getTotalElements()).isEqualTo(3);

        verify(specificationBuilder).build(criteria);
        verify(userRepository).findAll(mockSpec, pageable);
    }

    @Test
    @DisplayName("Should return empty page when no users match criteria")
    void search_NoMatches_EmptyPage() {
        // Given
        UserFilterCriteria criteria = UserFilterCriteria.builder()
                .username("nonexistent")
                .build();
        Pageable pageable = PageRequest.of(0, 20);

        Specification<User> mockSpec = mock(Specification.class);
        Page<User> entityPage = Page.empty(pageable);

        when(specificationBuilder.build(criteria)).thenReturn(mockSpec);
        when(userRepository.findAll(mockSpec, pageable)).thenReturn(entityPage);

        // When
        Page<UserDto> result = userService.search(criteria, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
        assertThat(result.getTotalPages()).isEqualTo(0);
        assertThat(result.isEmpty()).isTrue();

        verify(specificationBuilder).build(criteria);
        verify(userRepository).findAll(mockSpec, pageable);
    }

    @Test
    @DisplayName("Should handle pagination with multiple pages")
    void search_MultiplePagesNavigation_Success() {
        // Given
        UserFilterCriteria criteria = UserFilterCriteria.builder().build();
        Pageable page0 = PageRequest.of(0, 2);
        Pageable page1 = PageRequest.of(1, 2);

        Specification<User> mockSpec = mock(Specification.class);

        // First page (2 users out of 3 total)
        Page<User> firstPageEntities = new PageImpl<>(
                List.of(testUser1, testUser2), page0, 3
        );

        when(specificationBuilder.build(criteria)).thenReturn(mockSpec);
        when(userRepository.findAll(mockSpec, page0)).thenReturn(firstPageEntities);
        when(userMapper.toDto(testUser1)).thenReturn(testUserDto1);
        when(userMapper.toDto(testUser2)).thenReturn(testUserDto2);

        // When
        Page<UserDto> result = userService.search(criteria, page0);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getTotalPages()).isEqualTo(2);
        assertThat(result.getNumber()).isEqualTo(0);
        assertThat(result.isFirst()).isTrue();
        assertThat(result.hasNext()).isTrue();
        assertThat(result.hasPrevious()).isFalse();

        verify(specificationBuilder).build(criteria);
        verify(userRepository).findAll(mockSpec, page0);
    }

    @Test
    @DisplayName("Should sort results by username ascending")
    void search_SortByUsernameAsc_Success() {
        // Given
        UserFilterCriteria criteria = UserFilterCriteria.builder().build();
        Pageable pageable = PageRequest.of(0, 20, Sort.by("username").ascending());

        Specification<User> mockSpec = mock(Specification.class);
        Page<User> entityPage = new PageImpl<>(
                List.of(testUser3, testUser2, testUser1), pageable, 3
        );

        when(specificationBuilder.build(criteria)).thenReturn(mockSpec);
        when(userRepository.findAll(mockSpec, pageable)).thenReturn(entityPage);
        when(userMapper.toDto(testUser1)).thenReturn(testUserDto1);
        when(userMapper.toDto(testUser2)).thenReturn(testUserDto2);
        when(userMapper.toDto(testUser3)).thenReturn(testUserDto3);

        // When
        Page<UserDto> result = userService.search(criteria, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(3);
        assertThat(result.getSort()).isEqualTo(Sort.by("username").ascending());

        verify(userRepository).findAll(eq(mockSpec), eq(pageable));
    }

    @Test
    @DisplayName("Should verify Page metadata is correct")
    void search_PageMetadata_Correct() {
        // Given
        UserFilterCriteria criteria = UserFilterCriteria.builder().build();
        Pageable pageable = PageRequest.of(1, 10);

        Specification<User> mockSpec = mock(Specification.class);
        Page<User> entityPage = new PageImpl<>(
                List.of(testUser3), pageable, 21
        );

        when(specificationBuilder.build(criteria)).thenReturn(mockSpec);
        when(userRepository.findAll(mockSpec, pageable)).thenReturn(entityPage);
        when(userMapper.toDto(testUser3)).thenReturn(testUserDto3);

        // When
        Page<UserDto> result = userService.search(criteria, pageable);

        // Then - Verify all Page metadata
        assertThat(result.getContent()).hasSize(1); // Number of elements in current page
        assertThat(result.getNumberOfElements()).isEqualTo(1);
        assertThat(result.getTotalElements()).isEqualTo(21); // Total across all pages
        assertThat(result.getTotalPages()).isEqualTo(3); // 21 / 10 = 3 pages
        assertThat(result.getNumber()).isEqualTo(1); // Current page number (0-indexed)
        assertThat(result.getSize()).isEqualTo(10); // Page size
        assertThat(result.isFirst()).isFalse();
        assertThat(result.isLast()).isFalse();
        assertThat(result.hasNext()).isTrue();
        assertThat(result.hasPrevious()).isTrue();
    }

    @Test
    @DisplayName("Should throw RuntimeException when repository throws exception")
    void search_RepositoryThrowsException_ThrowsRuntimeException() {
        // Given
        UserFilterCriteria criteria = UserFilterCriteria.builder()
                .username("john")
                .build();
        Pageable pageable = PageRequest.of(0, 20);

        Specification<User> mockSpec = mock(Specification.class);

        when(specificationBuilder.build(criteria)).thenReturn(mockSpec);
        when(userRepository.findAll(mockSpec, pageable))
                .thenThrow(new RuntimeException("Database connection error"));

        // When & Then
        assertThatThrownBy(() -> userService.search(criteria, pageable))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to search users")
                .hasMessageContaining("Database connection error");

        verify(specificationBuilder).build(criteria);
        verify(userRepository).findAll(mockSpec, pageable);
    }
}
