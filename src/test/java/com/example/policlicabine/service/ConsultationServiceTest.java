package com.example.policlicabine.service;

import com.example.policlicabine.base.BaseServiceTest;
import com.example.policlicabine.builder.ConsultationTestBuilder;
import com.example.policlicabine.common.Result;
import com.example.policlicabine.dto.ConsultationDto;
import com.example.policlicabine.entity.Consultation;
import com.example.policlicabine.entity.enums.Specialty;
import com.example.policlicabine.event.ConsultationActivated;
import com.example.policlicabine.event.ConsultationDeactivated;
import com.example.policlicabine.mapper.ConsultationMapper;
import com.example.policlicabine.repository.ConsultationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
    private ConsultationMapper consultationMapper;

    @InjectMocks
    private ConsultationService consultationService;

    private Consultation testConsultation;
    private ConsultationDto testConsultationDto;

    @BeforeEach
    void setUp() {
        eventPublisher = createEventPublisher();
        consultationService = new ConsultationService(consultationRepository, consultationMapper, eventPublisher);

        testConsultation = ConsultationTestBuilder.generalConsultation()
                .withIsActive(true)
                .build();

        testConsultationDto = ConsultationDto.builder()
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
        when(consultationRepository.save(any(Consultation.class))).thenReturn(testConsultation);
        when(consultationMapper.toDto(any(Consultation.class))).thenReturn(testConsultationDto);

        // When
        Result<ConsultationDto> result = consultationService.deactivateConsultation(consultationId);

        // Then
        assertThat(result).isSuccess().hasValue();

        // Verify entity was deactivated
        assertThat(testConsultation.getIsActive()).isFalse();

        // Verify domain event was published
        ArgumentCaptor<ConsultationDeactivated> eventCaptor = ArgumentCaptor.forClass(ConsultationDeactivated.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        ConsultationDeactivated event = eventCaptor.getValue();
        assertThat(event.consultationId()).isEqualTo(consultationId);
        assertThat(event.consultationName()).isEqualTo(testConsultation.getName());
    }

    @Test
    @DisplayName("Should fail to deactivate when consultation not found")
    void deactivateConsultation_NotFound_Failure() {
        // Given
        UUID nonExistentId = UUID.randomUUID();
        when(consultationRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // When
        Result<ConsultationDto> result = consultationService.deactivateConsultation(nonExistentId);

        // Then
        assertThat(result)
                .isFailure()
                .hasErrorMessageContaining("Consultation not found");

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
        Result<ConsultationDto> result = consultationService.deactivateConsultation(consultationId);

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
        when(consultationRepository.save(any(Consultation.class))).thenReturn(testConsultation);
        when(consultationMapper.toDto(any(Consultation.class))).thenReturn(testConsultationDto);

        // When
        Result<ConsultationDto> result = consultationService.activateConsultation(consultationId);

        // Then
        assertThat(result).isSuccess().hasValue();

        // Verify entity was activated
        assertThat(testConsultation.getIsActive()).isTrue();

        // Verify domain event was published
        ArgumentCaptor<ConsultationActivated> eventCaptor = ArgumentCaptor.forClass(ConsultationActivated.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        ConsultationActivated event = eventCaptor.getValue();
        assertThat(event.consultationId()).isEqualTo(consultationId);
    }

    // ========================================================================
    // getEntitiesByNames() Tests - Service-to-Service Communication
    // ========================================================================

    @Test
    @DisplayName("Should retrieve consultation entities by names for service calls")
    void getEntitiesByNames_Success() {
        // Given
        Consultation cardiology = ConsultationTestBuilder.cardiologyConsultation().build();
        Consultation dermatology = ConsultationTestBuilder.dermatologyConsultation().build();

        List<String> names = Arrays.asList("Cardiology Consultation", "Dermatology Consultation");
        when(consultationRepository.findByNameInAndIsActiveTrue(names))
                .thenReturn(Arrays.asList(cardiology, dermatology));

        // When
        List<Consultation> result = consultationService.getEntitiesByNames(names);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).contains(cardiology, dermatology);

        verify(consultationRepository).findByNameInAndIsActiveTrue(names);
    }

    @Test
    @DisplayName("Should return empty list when no consultations match names")
    void getEntitiesByNames_NoMatches_EmptyList() {
        // Given
        List<String> names = Arrays.asList("Non-existent Consultation");
        when(consultationRepository.findByNameInAndIsActiveTrue(names))
                .thenReturn(List.of());

        // When
        List<Consultation> result = consultationService.getEntitiesByNames(names);

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
        Consultation result = consultationService.getEntityById(consultationId);

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
        Consultation result = consultationService.getEntityById(nonExistentId);

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
        when(consultationMapper.toDto(testConsultation)).thenReturn(testConsultationDto);

        // When
        Result<ConsultationDto> result = consultationService.findById(consultationId);

        // Then
        assertThat(result).isSuccess().hasValue(testConsultationDto);
    }

    @Test
    @DisplayName("Should update consultation price successfully")
    void update_UpdatePrice_Success() {
        // Given
        UUID consultationId = testConsultation.getConsultationId();
        ConsultationDto updateDto = ConsultationDto.builder()
                .consultationId(consultationId)
                .name("General Consultation")
                .specialty(Specialty.GENERAL_DERMATOLOGY)
                .price(new BigDecimal("200.00"))
                .build();

        when(consultationRepository.findById(consultationId)).thenReturn(Optional.of(testConsultation));
        when(consultationRepository.save(any(Consultation.class))).thenReturn(testConsultation);
        when(consultationMapper.toDto(any(Consultation.class))).thenReturn(updateDto);

        // When
        Result<ConsultationDto> result = consultationService.update(consultationId, updateDto);

        // Then
        assertThat(result).isSuccess().hasValue();

        // Verify price was updated
        assertThat(testConsultation.getPrice()).isEqualByComparingTo(new BigDecimal("200.00"));
        verify(consultationRepository).save(testConsultation);
    }
}
