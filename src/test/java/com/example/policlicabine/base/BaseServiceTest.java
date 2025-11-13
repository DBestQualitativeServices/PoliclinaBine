package com.example.policlicabine.base;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.ActiveProfiles;

import static org.mockito.Mockito.mock;

/**
 * Abstract base class for service layer unit tests.
 * <p>
 * Provides common configuration and utilities for testing services with Mockito.
 * Uses @ExtendWith(MockitoExtension.class) to enable Mockito annotations.
 * </p>
 * <p>Features:</p>
 * <ul>
 *   <li>Mockito extension for @Mock, @InjectMocks support</li>
 *   <li>Test profile activation</li>
 *   <li>Pre-configured event publisher mock</li>
 *   <li>Common test utilities</li>
 * </ul>
 * <p>Example usage:</p>
 * <pre>
 * {@literal @}ExtendWith(MockitoExtension.class)
 * class PatientServiceTest extends BaseServiceTest {
 *
 *     {@literal @}Mock
 *     private PatientRepository patientRepository;
 *
 *     {@literal @}Mock
 *     private PatientMapper patientMapper;
 *
 *     {@literal @}InjectMocks
 *     private PatientService patientService;
 *
 *     {@literal @}BeforeEach
 *     void setUp() {
 *         // Additional setup if needed
 *         // eventPublisher is already available from base class
 *     }
 *
 *     {@literal @}Test
 *     void testFindById_Success() {
 *         // Test implementation
 *     }
 * }
 * </pre>
 */
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
public abstract class BaseServiceTest {

    /**
     * Mock ApplicationEventPublisher for testing domain event publishing.
     * Services often inject ApplicationEventPublisher to publish events.
     */
    protected ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);

    /**
     * Creates a mock ApplicationEventPublisher instance.
     * Useful when manually constructing service instances in tests.
     *
     * @return a new mock ApplicationEventPublisher
     */
    protected ApplicationEventPublisher createEventPublisher() {
        return mock(ApplicationEventPublisher.class);
    }
}
