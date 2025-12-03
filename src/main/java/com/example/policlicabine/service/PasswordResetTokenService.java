package com.example.policlicabine.service;

import com.example.policlicabine.entity.PasswordResetToken;
import com.example.policlicabine.entity.User;
import com.example.policlicabine.exception.BusinessException;
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
    public PasswordResetToken createResetToken(UUID userId) {
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

        return saved;
    }

    /**
     * Validate reset token and return it if valid
     */
    @Transactional(readOnly = true)
    public PasswordResetToken validateResetToken(String token) {
        PasswordResetToken resetToken = resetTokenRepository.findByToken(token)
                .orElse(null);

        if (resetToken == null) {
            log.warn("Invalid reset token attempted: {}", token);
            throw new BusinessException("Invalid or expired reset token");
        }

        if (resetToken.isUsed()) {
            log.warn("Already used reset token attempted: {}", token);
            throw new BusinessException("Reset token has already been used");
        }

        if (resetToken.getExpiryDate().isBefore(OffsetDateTime.now(ZoneOffset.UTC))) {
            log.warn("Expired reset token attempted: {}", token);
            throw new BusinessException("Reset token has expired");
        }

        return resetToken;
    }

    /**
     * Mark reset token as used
     */
    public void markTokenAsUsed(String token) {
        PasswordResetToken resetToken = resetTokenRepository.findByToken(token)
                .orElseThrow(() -> new BusinessException("Reset token not found"));

        resetToken.setUsed(true);
        resetTokenRepository.save(resetToken);
        log.info("Marked reset token as used for user: {}", resetToken.getUser().getUserId());
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
