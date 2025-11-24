package com.example.policlicabine.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.function.Predicate;

/**
 * Utility for generating secure credentials for auto-registration scenarios.
 * Uses SecureRandom for cryptographically strong random generation.
 */
@Component
@Slf4j
public class CredentialGenerator {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int MAX_USERNAME_COLLISION_ATTEMPTS = 5;

    // Character sets for password generation (lowercase + digits only)
    private static final String LOWERCASE = "abcdefghijklmnopqrstuvwxyz";
    private static final String DIGITS = "0123456789";
    private static final String PASSWORD_CHARS = LOWERCASE + DIGITS;

    /**
     * Generates a username from firstName and lastName with random suffix.
     * Format: FirstnameLastname42 (sanitized, no spaces/special chars)
     *
     * @param firstName User's first name
     * @param lastName User's last name
     * @param collisionCheck Function to check if username already exists
     * @return Generated username or null if all attempts collide
     */
    public String generateUsername(String firstName, String lastName,
                                    Predicate<String> collisionCheck) {
        String baseName = sanitizeForUsername(firstName) + sanitizeForUsername(lastName);

        if (baseName.length() < 3) {
            log.warn("Generated username base too short: {}", baseName);
            baseName = "User" + baseName; // Fallback prefix
        }

        // Try up to MAX_ATTEMPTS to find non-colliding username
        for (int attempt = 0; attempt < MAX_USERNAME_COLLISION_ATTEMPTS; attempt++) {
            int randomSuffix = SECURE_RANDOM.nextInt(90) + 10; // 10-99
            String username = baseName + randomSuffix;

            if (!collisionCheck.test(username)) {
                log.info("Generated unique username: {} (attempt {})", username, attempt + 1);
                return username;
            }
            log.debug("Username collision detected: {} (attempt {})", username, attempt + 1);
        }

        log.error("Failed to generate unique username after {} attempts for base: {}",
                MAX_USERNAME_COLLISION_ATTEMPTS, baseName);
        return null;
    }

    /**
     * Sanitizes a string for use in username by:
     * - Removing all whitespace
     * - Removing all non-alphanumeric characters
     * - Capitalizing first letter
     *
     * @param input Raw string (firstName or lastName)
     * @return Sanitized string suitable for username
     */
    public String sanitizeForUsername(String input) {
        if (input == null || input.trim().isEmpty()) {
            return "";
        }

        // Remove whitespace and non-alphanumeric, capitalize first letter
        String sanitized = input.trim()
                .replaceAll("[^a-zA-Z0-9]", "") // Remove special chars
                .replaceAll("\\s+", "");         // Remove whitespace

        if (sanitized.isEmpty()) {
            return "";
        }

        // Capitalize first letter, lowercase the rest
        return sanitized.substring(0, 1).toUpperCase() + sanitized.substring(1).toLowerCase();
    }

    /**
     * Generates a cryptographically secure random password.
     * Uses lowercase letters and digits only for easy verbal communication.
     *
     * Format: 12 characters, lowercase + digits (e.g., "k3mpz9wqr742")
     *
     * @return Secure random password (12 characters, lowercase + digits)
     */
    public String generateSecurePasswordLowercase() {
        int length = 12;
        StringBuilder password = new StringBuilder(length);

        password.append(LOWERCASE.charAt(SECURE_RANDOM.nextInt(LOWERCASE.length())));
        password.append(DIGITS.charAt(SECURE_RANDOM.nextInt(DIGITS.length())));

        for (int i = 2; i < length; i++) {
            password.append(PASSWORD_CHARS.charAt(SECURE_RANDOM.nextInt(PASSWORD_CHARS.length())));
        }

        String generatedPassword = shuffleString(password.toString());
        log.info("Secure password generated (length: {})", generatedPassword.length());
        return generatedPassword;
    }

    /**
     * Shuffles a string randomly using SecureRandom.
     */
    private String shuffleString(String input) {
        char[] chars = input.toCharArray();
        for (int i = chars.length - 1; i > 0; i--) {
            int j = SECURE_RANDOM.nextInt(i + 1);
            char temp = chars[i];
            chars[i] = chars[j];
            chars[j] = temp;
        }
        return new String(chars);
    }
}
