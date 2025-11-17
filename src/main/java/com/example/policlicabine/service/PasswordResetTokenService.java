package com.example.policlicabine.service;

import com.example.policlicabine.common.Result;
import com.example.policlicabine.entity.PasswordResetToken;
import com.example.policlicabine.entity.User;
import com.example.policlicabine.repository.PasswordResetTokenRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PasswordResetTokenService {

    private final PasswordResetTokenRepository resetTokenRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Value("${password-reset.token-expiration}")
    private Long tokenExpirationMs;

    /**
     * Create a password reset token for a user
     */
    public Result<PasswordResetToken> createResetToken(UUID userId) {
        try {
            // Invalidate any previous reset tokens for this user
            resetTokenRepository.deleteByUserId(userId);

            // Create user reference without DB hit
            User userRef = entityManager.getReference(User.class, userId);

            // Generate token and expiry date
            String token = UUID.randomUUID().toString();
            OffsetDateTime expiryDate = OffsetDateTime.now(ZoneOffset.UTC).plusSeconds(tokenExpirationMs / 1000);

            PasswordResetToken resetToken = PasswordResetToken.builder()
                    .token(token)
                    .user(userRef)
                    .expiryDate(expiryDate)
                    .used(false)
                    .build();

            PasswordResetToken saved = resetTokenRepository.save(resetToken);
            log.info("Created password reset token for user: {}", userId);

            return Result.success(saved);
        } catch (Exception e) {
            log.error("Error creating reset token for user {}: {}", userId, e.getMessage(), e);
            return Result.failure("Failed to create password reset token");
        }
    }

    /**
     * Validate reset token and return it if valid
     */
    @Transactional(readOnly = true)
    public Result<PasswordResetToken> validateResetToken(String token) {
        try {
            PasswordResetToken resetToken = resetTokenRepository.findByToken(token)
                    .orElse(null);

            if (resetToken == null) {
                log.warn("Invalid reset token attempted: {}", token);
                return Result.failure("Invalid or expired reset token");
            }

            if (resetToken.isUsed()) {
                log.warn("Already used reset token attempted: {}", token);
                return Result.failure("Reset token has already been used");
            }

            if (resetToken.getExpiryDate().isBefore(OffsetDateTime.now(ZoneOffset.UTC))) {
                log.warn("Expired reset token attempted: {}", token);
                return Result.failure("Reset token has expired");
            }

            return Result.success(resetToken);
        } catch (Exception e) {
            log.error("Error validating reset token: {}", e.getMessage(), e);
            return Result.failure("Failed to validate reset token");
        }
    }

    /**
     * Mark reset token as used
     */
    public Result<Void> markTokenAsUsed(String token) {
        try {
            PasswordResetToken resetToken = resetTokenRepository.findByToken(token)
                    .orElse(null);

            if (resetToken == null) {
                return Result.failure("Reset token not found");
            }

            resetToken.setUsed(true);
            resetTokenRepository.save(resetToken);
            log.info("Marked reset token as used for user: {}", resetToken.getUser().getUserId());

            return Result.success(null);
        } catch (Exception e) {
            log.error("Error marking token as used: {}", e.getMessage(), e);
            return Result.failure("Failed to mark token as used");
        }
    }

    /**
     * Clean up expired tokens (can be scheduled)
     */
    public void cleanupExpiredTokens() {
        try {
            resetTokenRepository.deleteByExpiryDateBefore(OffsetDateTime.now(ZoneOffset.UTC));
            log.info("Cleaned up expired password reset tokens");
        } catch (Exception e) {
            log.error("Error cleaning up expired tokens: {}", e.getMessage(), e);
        }
    }
}
