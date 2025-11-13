package com.example.policlicabine.event;

import com.example.policlicabine.base.BaseServiceTest;
import com.example.policlicabine.builder.ConsultationTestBuilder;
import com.example.policlicabine.builder.PatientTestBuilder;
import com.example.policlicabine.common.Result;
import com.example.policlicabine.dto.ConsultationDto;
import com.example.policlicabine.dto.PatientDto;
import com.example.policlicabine.entity.Consultation;
import com.example.policlicabine.entity.Patient;
import com.example.policlicabine.mapper.ConsultationMapper;
import com.example.policlicabine.mapper.PatientMapper;
import com.example.policlicabine.repository.ConsultationRepository;
import com.example.policlicabine.repository.PatientRepository;
import com.example.policlicabine.service.ConsultationService;
import com.example.policlicabine.service.PatientService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static com.example.policlicabine.util.ResultAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Domain Event Publishing Tests demonstrating:
 * - Event publishing after successful operations
 * - No events published on failure
 * - Event data integrity
 * - Multiple event types
 * - Java records as events
 * <p>
 * IMPORTANT: Domain events should ONLY be published AFTER successful operations,
 * and BEFORE returning Result.success(). Never publish events on failure paths.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Domain Event Publishing Tests")
class DomainEventTest extends BaseServiceTest {

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private PatientMapper patientMapper;

    @Mock
    private ConsultationRepository consultationRepository;

    @Mock
    private ConsultationMapper consultationMapper;

    @InjectMocks
    private PatientService patientService;

    @InjectMocks
    private ConsultationService consultationService;

    // ========================================================================
    // PatientRegistered Event Tests
    // ========================================================================

    @Test
    @DisplayName("Should publish PatientRegistered event after successful registration")
    void registerPatient_Success_PublishesEvent() {
        // Given
        patientService = new PatientService(patientRepository, patientMapper, eventPublisher);

        Patient patient = PatientTestBuilder.aPatient()
                .withFirstName("John")
                .withLastName("Doe")
                .withPhone("0700123456")
                .withEmail("john.doe@test.com")
                .build();

        PatientDto patientDto = PatientDto.builder()
                .patientId(patient.getPatientId())
                .firstName("John")
                .lastName("Doe")
                .phone("0700123456")
                .email("john.doe@test.com")
                .build();

        when(patientRepository.existsByPhone(anyString())).thenReturn(false);
        when(patientRepository.save(any(Patient.class))).thenReturn(patient);
        when(patientMapper.toDto(any(Patient.class))).thenReturn(patientDto);

        // When
        Result<PatientDto> result = patientService.registerNewPatient(
                "John", "Doe", "0700123456", "john.doe@test.com", "123 Test St"
        );

        // Then - Verify result is successful
        assertThat(result).isSuccess();

        // Verify event was published EXACTLY ONCE
        ArgumentCaptor<PatientRegistered> eventCaptor = ArgumentCaptor.forClass(PatientRegistered.class);
        verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());

        // Verify event data integrity
        PatientRegistered event = eventCaptor.getValue();
        assertThat(event).isNotNull();
        assertThat(event.patientId()).isEqualTo(patient.getPatientId());
        assertThat(event.firstName()).isEqualTo("John");
        assertThat(event.lastName()).isEqualTo("Doe");
        assertThat(event.phone()).isEqualTo("0700123456");
        assertThat(event.email()).isEqualTo("john.doe@test.com");
    }

    @Test
    @DisplayName("Should NOT publish event when registration fails")
    void registerPatient_Failure_NoEventPublished() {
        // Given
        patientService = new PatientService(patientRepository, patientMapper, eventPublisher);

        when(patientRepository.existsByPhone("0700123456")).thenReturn(true);

        // When - Registration fails due to duplicate phone
        Result<PatientDto> result = patientService.registerNewPatient(
                "John", "Doe", "0700123456", "john.doe@test.com", "123 Test St"
        );

        // Then - Verify result is failure
        assertThat(result).isFailure();

        // Verify NO event was published
        verifyNoInteractions(eventPublisher);
    }

    @Test
    @DisplayName("Should NOT publish event when validation fails")
    void registerPatient_ValidationFailure_NoEventPublished() {
        // Given
        patientService = new PatientService(patientRepository, patientMapper, eventPublisher);

        // When - Missing required field (first name)
        Result<PatientDto> result = patientService.registerNewPatient(
                null, "Doe", "0700123456", "john.doe@test.com", "123 Test St"
        );

        // Then - Verify result is failure
        assertThat(result).isFailure();

        // Verify NO event was published
        verifyNoInteractions(eventPublisher);
    }

    // ========================================================================
    // ConsultationDeactivated Event Tests
    // ========================================================================

    @Test
    @DisplayName("Should publish ConsultationDeactivated event after successful deactivation")
    void deactivateConsultation_Success_PublishesEvent() {
        // Given
        consultationService = new ConsultationService(consultationRepository, consultationMapper, eventPublisher);

        Consultation consultation = ConsultationTestBuilder.generalConsultation()
                .withIsActive(true)
                .build();

        ConsultationDto consultationDto = ConsultationDto.builder()
                .consultationId(consultation.getConsultationId())
                .name(consultation.getName())
                .isActive(false)
                .build();

        when(consultationRepository.findById(consultation.getConsultationId()))
                .thenReturn(Optional.of(consultation));
        when(consultationRepository.save(any(Consultation.class))).thenReturn(consultation);
        when(consultationMapper.toDto(any(Consultation.class))).thenReturn(consultationDto);

        // When
        Result<ConsultationDto> result = consultationService.deactivateConsultation(
                consultation.getConsultationId()
        );

        // Then - Verify result is successful
        assertThat(result).isSuccess();

        // Verify event was published
        ArgumentCaptor<ConsultationDeactivated> eventCaptor =
                ArgumentCaptor.forClass(ConsultationDeactivated.class);
        verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());

        // Verify event data
        ConsultationDeactivated event = eventCaptor.getValue();
        assertThat(event.consultationId()).isEqualTo(consultation.getConsultationId());
        assertThat(event.consultationName()).isEqualTo(consultation.getName());
    }

    @Test
    @DisplayName("Should NOT publish event when deactivation fails (not found)")
    void deactivateConsultation_NotFound_NoEventPublished() {
        // Given
        consultationService = new ConsultationService(consultationRepository, consultationMapper, eventPublisher);

        when(consultationRepository.findById(any())).thenReturn(Optional.empty());

        // When
        Result<ConsultationDto> result = consultationService.deactivateConsultation(
                java.util.UUID.randomUUID()
        );

        // Then
        assertThat(result).isFailure();
        verifyNoInteractions(eventPublisher);
    }

    @Test
    @DisplayName("Should NOT publish event when consultation already inactive")
    void deactivateConsultation_AlreadyInactive_NoEventPublished() {
        // Given
        consultationService = new ConsultationService(consultationRepository, consultationMapper, eventPublisher);

        Consultation inactiveConsultation = ConsultationTestBuilder.generalConsultation()
                .withIsActive(false)
                .build();

        when(consultationRepository.findById(inactiveConsultation.getConsultationId()))
                .thenReturn(Optional.of(inactiveConsultation));

        // When
        Result<ConsultationDto> result = consultationService.deactivateConsultation(
                inactiveConsultation.getConsultationId()
        );

        // Then
        assertThat(result).isFailure();
        verifyNoInteractions(eventPublisher);
    }

    // ========================================================================
    // ConsultationActivated Event Tests
    // ========================================================================

    @Test
    @DisplayName("Should publish ConsultationActivated event after successful activation")
    void activateConsultation_Success_PublishesEvent() {
        // Given
        consultationService = new ConsultationService(consultationRepository, consultationMapper, eventPublisher);

        Consultation consultation = ConsultationTestBuilder.generalConsultation()
                .withIsActive(false)
                .build();

        ConsultationDto consultationDto = ConsultationDto.builder()
                .consultationId(consultation.getConsultationId())
                .name(consultation.getName())
                .isActive(true)
                .build();

        when(consultationRepository.findById(consultation.getConsultationId()))
                .thenReturn(Optional.of(consultation));
        when(consultationRepository.save(any(Consultation.class))).thenReturn(consultation);
        when(consultationMapper.toDto(any(Consultation.class))).thenReturn(consultationDto);

        // When
        Result<ConsultationDto> result = consultationService.activateConsultation(
                consultation.getConsultationId()
        );

        // Then
        assertThat(result).isSuccess();

        // Verify event was published
        ArgumentCaptor<ConsultationActivated> eventCaptor =
                ArgumentCaptor.forClass(ConsultationActivated.class);
        verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());

        // Verify event data
        ConsultationActivated event = eventCaptor.getValue();
        assertThat(event.consultationId()).isEqualTo(consultation.getConsultationId());
        assertThat(event.consultationName()).isEqualTo(consultation.getName());
    }

    // ========================================================================
    // Event Publishing Patterns - Best Practices Tests
    // ========================================================================

    @Test
    @DisplayName("Events should be Java records (immutable)")
    void events_AreRecords() {
        // Java records are implicitly final and immutable
        // This test verifies the event types are records

        // PatientRegistered
        PatientRegistered patientEvent = new PatientRegistered(
                java.util.UUID.randomUUID(),
                "John",
                "Doe",
                "0700123456",
                "john.doe@test.com"
        );
        assertThat(patientEvent).isInstanceOf(Record.class);

        // ConsultationDeactivated
        ConsultationDeactivated deactivatedEvent = new ConsultationDeactivated(
                java.util.UUID.randomUUID(),
                "General Consultation"
        );
        assertThat(deactivatedEvent).isInstanceOf(Record.class);

        // ConsultationActivated
        ConsultationActivated activatedEvent = new ConsultationActivated(
                java.util.UUID.randomUUID(),
                "General Consultation"
        );
        assertThat(activatedEvent).isInstanceOf(Record.class);
    }

    @Test
    @DisplayName("Event records should have proper equals/hashCode")
    void events_HaveProperEqualsHashCode() {
        // Given
        java.util.UUID id = java.util.UUID.randomUUID();

        PatientRegistered event1 = new PatientRegistered(id, "John", "Doe", "0700123456", "test@example.com");
        PatientRegistered event2 = new PatientRegistered(id, "John", "Doe", "0700123456", "test@example.com");
        PatientRegistered event3 = new PatientRegistered(java.util.UUID.randomUUID(), "Jane", "Smith", "0700999888", "jane@example.com");

        // Then - Records have automatic equals/hashCode based on all fields
        assertThat(event1).isEqualTo(event2);
        assertThat(event1).isNotEqualTo(event3);
        assertThat(event1.hashCode()).isEqualTo(event2.hashCode());
    }
}
