package com.example.policlicabine.service;

import com.example.policlicabine.entity.PasswordResetToken;
import com.example.policlicabine.entity.User;
import com.example.policlicabine.exception.BusinessException;
import com.example.policlicabine.repository.PasswordResetTokenRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link PasswordResetTokenService}.
 * <p>
 * Tests password reset token lifecycle:
 * - Token creation with automatic expiration
 * - Token validation (exists, not used, not expired)
 * - Token usage marking to prevent reuse
 * - Expired token cleanup
 * <p>
 * Uses Mockito mocks for repository and EntityManager.
 */
@ExtendWith(MockitoExtension.class)
class PasswordResetTokenServiceTest {

    @Mock
    private PasswordResetTokenRepository resetTokenRepository;

    @Mock
    private EntityManager entityManager;

    private PasswordResetTokenService passwordResetTokenService;

    private UUID testUserId;
    private User mockUserReference;

    // Token expiration: 1 hour (3600000 ms)
    private static final long TOKEN_EXPIRATION_MS = 3600000L;

    @BeforeEach
    void setUp() {
        passwordResetTokenService = new PasswordResetTokenService(resetTokenRepository);

        // Inject EntityManager and token expiration using reflection
        org.springframework.test.util.ReflectionTestUtils.setField(
                passwordResetTokenService, "entityManager", entityManager);
        org.springframework.test.util.ReflectionTestUtils.setField(
                passwordResetTokenService, "tokenExpirationMs", TOKEN_EXPIRATION_MS);

        testUserId = UUID.randomUUID();
        mockUserReference = new User();
        mockUserReference.setUserId(testUserId);
    }

    // ===== CREATE RESET TOKEN TESTS =====

    @Test
    void createResetToken_ShouldDeletePreviousTokensAndCreateNew() {
        // Given
        when(entityManager.getReference(eq(User.class), eq(testUserId)))
                .thenReturn(mockUserReference);
        when(resetTokenRepository.save(any(PasswordResetToken.class)))
                .thenAnswer(invocation -> {
                    PasswordResetToken token = invocation.getArgument(0);
                    token.setTokenId(UUID.randomUUID());
                    return token;
                });

        // When
        PasswordResetToken result = passwordResetTokenService.createResetToken(testUserId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getToken()).isNotNull();
        assertThat(result.getToken()).hasSize(36); // UUID format
        assertThat(result.getUser()).isEqualTo(mockUserReference);
        assertThat(result.isUsed()).isFalse();
        assertThat(result.getExpiryDate()).isAfter(OffsetDateTime.now(ZoneOffset.UTC));
        assertThat(result.getExpiryDate())
                .isBefore(OffsetDateTime.now(ZoneOffset.UTC).plusHours(1).plusMinutes(1));

        // Verify old tokens deleted first
        verify(resetTokenRepository).deleteByUserId(testUserId);

        // Verify token saved
        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(resetTokenRepository).save(tokenCaptor.capture());
        PasswordResetToken savedToken = tokenCaptor.getValue();
        assertThat(savedToken.getToken()).isNotBlank();
        assertThat(savedToken.isUsed()).isFalse();
    }

    @Test
    void createResetToken_ShouldSetCorrectExpirationTime() {
        // Given
        OffsetDateTime beforeCreation = OffsetDateTime.now(ZoneOffset.UTC);

        when(entityManager.getReference(eq(User.class), eq(testUserId)))
                .thenReturn(mockUserReference);
        when(resetTokenRepository.save(any(PasswordResetToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        PasswordResetToken token = passwordResetTokenService.createResetToken(testUserId);

        // Then
        assertThat(token).isNotNull();

        OffsetDateTime expectedExpiry = beforeCreation.plusHours(1);

        // Allow 5 second tolerance for test execution time
        assertThat(token.getExpiryDate())
                .isAfter(expectedExpiry.minusSeconds(5))
                .isBefore(expectedExpiry.plusSeconds(5));
    }

    // ===== VALIDATE RESET TOKEN TESTS =====

    @Test
    void validateResetToken_WithValidToken_ShouldReturnSuccess() {
        // Given
        String validToken = "valid-token-123";
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .tokenId(UUID.randomUUID())
                .token(validToken)
                .user(mockUserReference)
                .expiryDate(OffsetDateTime.now(ZoneOffset.UTC).plusHours(1))
                .used(false)
                .build();

        when(resetTokenRepository.findByToken(validToken))
                .thenReturn(Optional.of(resetToken));

        // When
        PasswordResetToken result = passwordResetTokenService.validateResetToken(validToken);

        // Then
        assertThat(result).isEqualTo(resetToken);

        verify(resetTokenRepository).findByToken(validToken);
    }

    @Test
    void validateResetToken_WithNonExistentToken_ShouldReturnFailure() {
        // Given
        String nonExistentToken = "non-existent-token";

        when(resetTokenRepository.findByToken(nonExistentToken))
                .thenReturn(Optional.empty());

        // When & Then
        BusinessException ex = assertThrows(BusinessException.class, () ->
                passwordResetTokenService.validateResetToken(nonExistentToken));

        assertThat(ex.getMessage()).contains("Invalid or expired reset token");
    }

    @Test
    void validateResetToken_WithUsedToken_ShouldReturnFailure() {
        // Given
        String usedToken = "used-token";
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .tokenId(UUID.randomUUID())
                .token(usedToken)
                .user(mockUserReference)
                .expiryDate(OffsetDateTime.now(ZoneOffset.UTC).plusHours(1))
                .used(true) // Already used
                .build();

        when(resetTokenRepository.findByToken(usedToken))
                .thenReturn(Optional.of(resetToken));

        // When & Then
        BusinessException ex = assertThrows(BusinessException.class, () ->
                passwordResetTokenService.validateResetToken(usedToken));

        assertThat(ex.getMessage()).contains("already been used");
    }

    @Test
    void validateResetToken_WithExpiredToken_ShouldReturnFailure() {
        // Given
        String expiredToken = "expired-token";
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .tokenId(UUID.randomUUID())
                .token(expiredToken)
                .user(mockUserReference)
                .expiryDate(OffsetDateTime.now(ZoneOffset.UTC).minusHours(1)) // Expired 1 hour ago
                .used(false)
                .build();

        when(resetTokenRepository.findByToken(expiredToken))
                .thenReturn(Optional.of(resetToken));

        // When & Then
        BusinessException ex = assertThrows(BusinessException.class, () ->
                passwordResetTokenService.validateResetToken(expiredToken));

        assertThat(ex.getMessage()).contains("expired");
    }

    @Test
    void validateResetToken_WithTokenExpiringNow_ShouldReturnFailure() {
        // Given - Token expires exactly now
        String tokenExpiringNow = "expiring-now-token";
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .tokenId(UUID.randomUUID())
                .token(tokenExpiringNow)
                .user(mockUserReference)
                .expiryDate(OffsetDateTime.now(ZoneOffset.UTC).minusNanos(1)) // Expired 1 nanosecond ago
                .used(false)
                .build();

        when(resetTokenRepository.findByToken(tokenExpiringNow))
                .thenReturn(Optional.of(resetToken));

        // When & Then - Should be considered expired (not valid anymore)
        BusinessException ex = assertThrows(BusinessException.class, () ->
                passwordResetTokenService.validateResetToken(tokenExpiringNow));

        assertThat(ex.getMessage()).contains("expired");
    }

    // ===== MARK TOKEN AS USED TESTS =====

    @Test
    void markTokenAsUsed_WithValidToken_ShouldReturnSuccessAndUpdateToken() {
        // Given
        String tokenString = "valid-token";
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .tokenId(UUID.randomUUID())
                .token(tokenString)
                .user(mockUserReference)
                .expiryDate(OffsetDateTime.now(ZoneOffset.UTC).plusHours(1))
                .used(false)
                .build();

        when(resetTokenRepository.findByToken(tokenString))
                .thenReturn(Optional.of(resetToken));
        when(resetTokenRepository.save(any(PasswordResetToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When & Then
        passwordResetTokenService.markTokenAsUsed(tokenString);

        // Verify token marked as used
        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(resetTokenRepository).save(tokenCaptor.capture());
        assertThat(tokenCaptor.getValue().isUsed()).isTrue();
    }

    @Test
    void markTokenAsUsed_WithNonExistentToken_ShouldReturnFailure() {
        // Given
        String nonExistentToken = "non-existent";

        when(resetTokenRepository.findByToken(nonExistentToken))
                .thenReturn(Optional.empty());

        // When & Then
        BusinessException ex = assertThrows(BusinessException.class, () ->
                passwordResetTokenService.markTokenAsUsed(nonExistentToken));

        assertThat(ex.getMessage()).contains("Reset token not found");

        // Verify no save attempted
        verify(resetTokenRepository, never()).save(any());
    }

    // ===== CLEANUP EXPIRED TOKENS TESTS =====

    @Test
    void cleanupExpiredTokens_ShouldDeleteExpiredTokensOnly() {
        // Given
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        // When
        passwordResetTokenService.cleanupExpiredTokens();

        // Then
        ArgumentCaptor<OffsetDateTime> dateCaptor = ArgumentCaptor.forClass(OffsetDateTime.class);
        verify(resetTokenRepository).deleteByExpiryDateBefore(dateCaptor.capture());

        // Verify cleanup called with approximately current time (within 5 seconds)
        OffsetDateTime capturedTime = dateCaptor.getValue();
        assertThat(capturedTime)
                .isAfter(now.minusSeconds(5))
                .isBefore(now.plusSeconds(5));
    }

    @Test
    void cleanupExpiredTokens_ShouldBeIdempotent() {
        // Given - Cleanup can be called multiple times safely

        // When
        passwordResetTokenService.cleanupExpiredTokens();
        passwordResetTokenService.cleanupExpiredTokens();
        passwordResetTokenService.cleanupExpiredTokens();

        // Then - Should be called 3 times without errors
        verify(resetTokenRepository, times(3)).deleteByExpiryDateBefore(any(OffsetDateTime.class));
    }
}
