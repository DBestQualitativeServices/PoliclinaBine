package com.example.policlicabine.service;

import com.example.policlicabine.base.BaseServiceTest;
import com.example.policlicabine.builder.ConsultationTestBuilder;
import com.example.policlicabine.common.Result;
import com.example.policlicabine.dto.ConsultationTypeDto;
import com.example.policlicabine.entity.ConsultationType;
import com.example.policlicabine.entity.enums.Specialty;
import com.example.policlicabine.mapper.ConsultationTypeMapper;
import com.example.policlicabine.repository.ConsultationRepository;
import com.example.policlicabine.repository.FormTemplateRepository;
import com.example.policlicabine.specification.ConsultationTypeSpecificationBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.example.policlicabine.util.ResultAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ConsultationService demonstrating:
 * - BaseService implementation testing
 * - Business logic for activation/deactivation
 * - Entity retrieval methods for service-to-service calls
 * - Domain event verification
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ConsultationService Unit Tests")
class ConsultationServiceTest extends BaseServiceTest {

    @Mock
    private ConsultationRepository consultationRepository;

    @Mock
    private ConsultationTypeMapper consultationTypeMapper;

    @Mock
    private ConsultationTypeSpecificationBuilder specificationBuilder;

    @Mock
    private FormTemplateRepository formTemplateRepository;

    @InjectMocks
    private ConsultationService consultationService;

    private ConsultationType testConsultation;
    private ConsultationTypeDto testConsultationTypeDto;

    @BeforeEach
    void setUp() {
        consultationService = new ConsultationService(
                consultationRepository,
                consultationTypeMapper,
                specificationBuilder,
                formTemplateRepository
        );

        testConsultation = ConsultationTestBuilder.generalConsultation()
                .withIsActive(true)
                .build();

        testConsultationTypeDto = ConsultationTypeDto.builder()
                .consultationId(testConsultation.getConsultationId())
                .name(testConsultation.getName())
                .specialty(testConsultation.getSpecialty())
                .price(testConsultation.getPrice())
                .isActive(testConsultation.getIsActive())
                .build();
    }

    // ========================================================================
    // deactivateConsultation() Tests
    // ========================================================================

    @Test
    @DisplayName("Should deactivate consultation successfully")
    void deactivateConsultation_Success() {
        // Given
        UUID consultationId = testConsultation.getConsultationId();
        when(consultationRepository.findById(consultationId)).thenReturn(Optional.of(testConsultation));
        when(consultationRepository.save(any(ConsultationType.class))).thenReturn(testConsultation);
        when(consultationTypeMapper.toDto(any(ConsultationType.class))).thenReturn(testConsultationTypeDto);

        // When
        Result<ConsultationTypeDto> result = consultationService.deactivateConsultation(consultationId);

        // Then
        assertThat(result).isSuccess().hasValue();

        // Verify entity was deactivated
        assertThat(testConsultation.getIsActive()).isFalse();

        // Events are no longer published for simple CRUD operations
    }

    @Test
    @DisplayName("Should fail to deactivate when consultation not found")
    void deactivateConsultation_NotFound_Failure() {
        // Given
        UUID nonExistentId = UUID.randomUUID();
        when(consultationRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // When
        Result<ConsultationTypeDto> result = consultationService.deactivateConsultation(nonExistentId);

        // Then
        assertThat(result)
                .isFailure()
                .hasErrorMessageContaining("ConsultationType not found");

        verify(consultationRepository).findById(nonExistentId);
        verify(consultationRepository, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    @DisplayName("Should fail to deactivate already inactive consultation")
    void deactivateConsultation_AlreadyInactive_Failure() {
        // Given
        testConsultation.setIsActive(false);
        UUID consultationId = testConsultation.getConsultationId();
        when(consultationRepository.findById(consultationId)).thenReturn(Optional.of(testConsultation));

        // When
        Result<ConsultationTypeDto> result = consultationService.deactivateConsultation(consultationId);

        // Then
        assertThat(result)
                .isFailure()
                .hasErrorMessageContaining("already inactive");

        verify(consultationRepository).findById(consultationId);
        verify(consultationRepository, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }

    // ========================================================================
    // activateConsultation() Tests
    // ========================================================================

    @Test
    @DisplayName("Should activate consultation successfully")
    void activateConsultation_Success() {
        // Given
        testConsultation.setIsActive(false);
        UUID consultationId = testConsultation.getConsultationId();
        when(consultationRepository.findById(consultationId)).thenReturn(Optional.of(testConsultation));
        when(consultationRepository.save(any(ConsultationType.class))).thenReturn(testConsultation);
        when(consultationTypeMapper.toDto(any(ConsultationType.class))).thenReturn(testConsultationTypeDto);

        // When
        Result<ConsultationTypeDto> result = consultationService.activateConsultation(consultationId);

        // Then
        assertThat(result).isSuccess().hasValue();

        // Verify entity was activated
        assertThat(testConsultation.getIsActive()).isTrue();

        // Events are no longer published for simple CRUD operations
    }

    // ========================================================================
    // getEntitiesByNames() Tests - Service-to-Service Communication
    // ========================================================================

    @Test
    @DisplayName("Should retrieve consultation entities by names for service calls")
    void getEntitiesByNames_Success() {
        // Given
        ConsultationType cardiology = ConsultationTestBuilder.cardiologyConsultation().build();
        ConsultationType dermatology = ConsultationTestBuilder.dermatologyConsultation().build();

        List<String> names = Arrays.asList("Cardiology ConsultationType", "Dermatology ConsultationType");
        when(consultationRepository.findByNameInAndIsActiveTrue(names))
                .thenReturn(Arrays.asList(cardiology, dermatology));

        // When
        List<ConsultationType> result = consultationService.getEntitiesByNames(names);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).contains(cardiology, dermatology);

        verify(consultationRepository).findByNameInAndIsActiveTrue(names);
    }

    @Test
    @DisplayName("Should return empty list when no consultations match names")
    void getEntitiesByNames_NoMatches_EmptyList() {
        // Given
        List<String> names = List.of("Non-existent ConsultationType");
        when(consultationRepository.findByNameInAndIsActiveTrue(names))
                .thenReturn(List.of());

        // When
        List<ConsultationType> result = consultationService.getEntitiesByNames(names);

        // Then
        assertThat(result).isEmpty();
        verify(consultationRepository).findByNameInAndIsActiveTrue(names);
    }

    // ========================================================================
    // getEntityById() Tests - Internal Method for Service Calls
    // ========================================================================

    @Test
    @DisplayName("Should retrieve entity by ID for service-to-service communication")
    void getEntityById_Success() {
        // Given
        UUID consultationId = testConsultation.getConsultationId();
        when(consultationRepository.findById(consultationId)).thenReturn(Optional.of(testConsultation));

        // When
        ConsultationType result = consultationService.getEntityById(consultationId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(testConsultation);
        verify(consultationRepository).findById(consultationId);
    }

    @Test
    @DisplayName("Should return null when entity not found by ID")
    void getEntityById_NotFound_ReturnsNull() {
        // Given
        UUID nonExistentId = UUID.randomUUID();
        when(consultationRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // When
        ConsultationType result = consultationService.getEntityById(nonExistentId);

        // Then
        assertThat(result).isNull();
        verify(consultationRepository).findById(nonExistentId);
    }

    // ========================================================================
    // Inherited CRUD Tests
    // ========================================================================

    @Test
    @DisplayName("Should find consultation by ID successfully")
    void findById_Success() {
        // Given
        UUID consultationId = testConsultation.getConsultationId();
        when(consultationRepository.findById(consultationId)).thenReturn(Optional.of(testConsultation));
        when(consultationTypeMapper.toDto(testConsultation)).thenReturn(testConsultationTypeDto);

        // When
        Result<ConsultationTypeDto> result = consultationService.findById(consultationId);

        // Then
        assertThat(result).isSuccess().hasValue(testConsultationTypeDto);
    }

    @Test
    @DisplayName("Should update consultation price successfully")
    void update_UpdatePrice_Success() {
        // Given
        UUID consultationId = testConsultation.getConsultationId();
        ConsultationTypeDto updateDto = ConsultationTypeDto.builder()
                .consultationId(consultationId)
                .name("General ConsultationType")
                .specialty(Specialty.GENERAL_DERMATOLOGY)
                .price(new BigDecimal("200.00"))
                .build();

        when(consultationRepository.findById(consultationId)).thenReturn(Optional.of(testConsultation));
        when(consultationRepository.save(any(ConsultationType.class))).thenReturn(testConsultation);
        when(consultationTypeMapper.toDto(any(ConsultationType.class))).thenReturn(updateDto);

        // When
        Result<ConsultationTypeDto> result = consultationService.update(consultationId, updateDto);

        // Then
        assertThat(result).isSuccess().hasValue();

        // Verify price was updated
        assertThat(testConsultation.getPrice()).isEqualByComparingTo(new BigDecimal("200.00"));
        verify(consultationRepository).save(testConsultation);
    }
}
