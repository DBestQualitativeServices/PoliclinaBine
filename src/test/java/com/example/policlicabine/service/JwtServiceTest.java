package com.example.policlicabine.service;

import com.example.policlicabine.security.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link JwtService}.
 * <p>
 * Tests JWT token operations:
 * - Access token generation with role claims
 * - Refresh token generation with type claims
 * - Username extraction from tokens
 * - Expiration date extraction
 * - Token validation (signature, expiration, username matching)
 * <p>
 * Uses real JJWT library for token generation and parsing.
 */
class JwtServiceTest {

    private JwtService jwtService;
    private UserDetails testUserDetails;

    // Test configuration
    private static final String TEST_SECRET = "dGVzdC1zZWNyZXQta2V5LW11c3QtYmUtYXQtbGVhc3QtMjU2LWJpdHMtbG9uZy1mb3ItSFMyNTYtYWxnb3JpdGht";
    private static final long ACCESS_TOKEN_EXPIRATION = 1800000L; // 30 minutes
    private static final long REFRESH_TOKEN_EXPIRATION = 604800000L; // 7 days

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();

        // Set test configuration using reflection (since JwtService uses @Value)
        ReflectionTestUtils.setField(jwtService, "secret", TEST_SECRET);
        ReflectionTestUtils.setField(jwtService, "expiration", ACCESS_TOKEN_EXPIRATION);
        ReflectionTestUtils.setField(jwtService, "refreshExpiration", REFRESH_TOKEN_EXPIRATION);

        // Create test user details
        testUserDetails = User.builder()
                .username("testuser")
                .password("password")
                .authorities(new SimpleGrantedAuthority("ROLE_DOCTOR"))
                .build();
    }

    // ===== GENERATE TOKEN TESTS =====

    @Test
    void generateToken_ShouldCreateValidAccessToken() {
        // When
        String token = jwtService.generateToken(testUserDetails);

        // Then
        assertThat(token).isNotNull();
        assertThat(token).isNotBlank();
        assertThat(token.split("\\.")).hasSize(3); // JWT has 3 parts: header.payload.signature

        // Parse and verify token contents
        SecretKey key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(TEST_SECRET));
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        assertThat(claims.getSubject()).isEqualTo("testuser");
        assertThat(claims.get("role")).isEqualTo("ROLE_DOCTOR");
        assertThat(claims.getIssuedAt()).isNotNull();
        assertThat(claims.getExpiration()).isNotNull();

        // Verify expiration is approximately 30 minutes from now
        long expectedExpiration = System.currentTimeMillis() + ACCESS_TOKEN_EXPIRATION;
        long actualExpiration = claims.getExpiration().getTime();
        assertThat(actualExpiration).isBetween(
                expectedExpiration - 5000, // Allow 5 second tolerance
                expectedExpiration + 5000
        );
    }

    @Test
    void generateToken_WithMultipleRoles_ShouldIncludeFirstRole() {
        // Given
        UserDetails multiRoleUser = User.builder()
                .username("admin")
                .password("password")
                .authorities(
                        new SimpleGrantedAuthority("ROLE_ADMIN"),
                        new SimpleGrantedAuthority("ROLE_MANAGER")
                )
                .build();

        // When
        String token = jwtService.generateToken(multiRoleUser);

        // Then
        SecretKey key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(TEST_SECRET));
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        assertThat(claims.get("role")).isEqualTo("ROLE_ADMIN");
    }

    // ===== GENERATE REFRESH TOKEN TESTS =====

    @Test
    void generateRefreshToken_ShouldCreateValidRefreshToken() {
        // When
        String token = jwtService.generateRefreshToken(testUserDetails);

        // Then
        assertThat(token).isNotNull();
        assertThat(token).isNotBlank();
        assertThat(token.split("\\.")).hasSize(3);

        // Parse and verify token contents
        SecretKey key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(TEST_SECRET));
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        assertThat(claims.getSubject()).isEqualTo("testuser");
        assertThat(claims.get("tokenType")).isEqualTo("refresh");

        // Verify expiration is approximately 7 days from now
        long expectedExpiration = System.currentTimeMillis() + REFRESH_TOKEN_EXPIRATION;
        long actualExpiration = claims.getExpiration().getTime();
        assertThat(actualExpiration).isBetween(
                expectedExpiration - 5000,
                expectedExpiration + 5000
        );
    }

    @Test
    void generateRefreshToken_ShouldHaveLongerExpirationThanAccessToken() {
        // When
        String accessToken = jwtService.generateToken(testUserDetails);
        String refreshToken = jwtService.generateRefreshToken(testUserDetails);

        // Then
        Date accessExpiration = jwtService.extractExpiration(accessToken);
        Date refreshExpiration = jwtService.extractExpiration(refreshToken);

        assertThat(refreshExpiration).isAfter(accessExpiration);

        // Verify approximately 7 days difference
        long diffMillis = refreshExpiration.getTime() - accessExpiration.getTime();
        long expectedDiff = REFRESH_TOKEN_EXPIRATION - ACCESS_TOKEN_EXPIRATION;
        assertThat(diffMillis).isBetween(
                expectedDiff - 10000, // 10 second tolerance
                expectedDiff + 10000
        );
    }

    // ===== EXTRACT USERNAME TESTS =====

    @Test
    void extractUsername_FromValidToken_ShouldReturnCorrectUsername() {
        // Given
        String token = jwtService.generateToken(testUserDetails);

        // When
        String username = jwtService.extractUsername(token);

        // Then
        assertThat(username).isEqualTo("testuser");
    }

    @Test
    void extractUsername_FromDifferentUsers_ShouldReturnDifferentUsernames() {
        // Given
        UserDetails user1 = User.builder()
                .username("user1")
                .password("password")
                .authorities(new SimpleGrantedAuthority("ROLE_USER"))
                .build();

        UserDetails user2 = User.builder()
                .username("user2")
                .password("password")
                .authorities(new SimpleGrantedAuthority("ROLE_USER"))
                .build();

        // When
        String token1 = jwtService.generateToken(user1);
        String token2 = jwtService.generateToken(user2);

        // Then
        assertThat(jwtService.extractUsername(token1)).isEqualTo("user1");
        assertThat(jwtService.extractUsername(token2)).isEqualTo("user2");
    }

    // ===== EXTRACT EXPIRATION TESTS =====

    @Test
    void extractExpiration_FromValidToken_ShouldReturnFutureDate() {
        // Given
        String token = jwtService.generateToken(testUserDetails);
        Date now = new Date();

        // When
        Date expiration = jwtService.extractExpiration(token);

        // Then
        assertThat(expiration).isAfter(now);
        assertThat(expiration.getTime()).isBetween(
                now.getTime() + ACCESS_TOKEN_EXPIRATION - 5000,
                now.getTime() + ACCESS_TOKEN_EXPIRATION + 5000
        );
    }

    // ===== IS TOKEN EXPIRED TESTS =====

    @Test
    void isTokenExpired_WithValidToken_ShouldReturnFalse() {
        // Given
        String token = jwtService.generateToken(testUserDetails);

        // When
        boolean isExpired = jwtService.isTokenExpired(token);

        // Then
        assertThat(isExpired).isFalse();
    }

    @Test
    void isTokenExpired_WithExpiredToken_ShouldReturnTrue() {
        // Given - Create a token that expires immediately
        SecretKey key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(TEST_SECRET));
        String expiredToken = Jwts.builder()
                .subject("testuser")
                .issuedAt(new Date(System.currentTimeMillis() - 10000)) // 10 seconds ago
                .expiration(new Date(System.currentTimeMillis() - 5000)) // Expired 5 seconds ago
                .signWith(key)
                .compact();

        // When
        boolean isExpired = jwtService.isTokenExpired(expiredToken);

        // Then
        assertThat(isExpired).isTrue();
    }

    @Test
    void isTokenExpired_WithInvalidToken_ShouldReturnTrue() {
        // Given
        String invalidToken = "invalid.token.string";

        // When
        boolean isExpired = jwtService.isTokenExpired(invalidToken);

        // Then - Invalid tokens should be treated as expired
        assertThat(isExpired).isTrue();
    }

    // ===== IS TOKEN VALID TESTS =====

    @Test
    void isTokenValid_WithValidTokenAndMatchingUser_ShouldReturnTrue() {
        // Given
        String token = jwtService.generateToken(testUserDetails);

        // When
        boolean isValid = jwtService.isTokenValid(token, testUserDetails);

        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    void isTokenValid_WithValidTokenButDifferentUser_ShouldReturnFalse() {
        // Given
        String token = jwtService.generateToken(testUserDetails);

        UserDetails differentUser = User.builder()
                .username("differentuser")
                .password("password")
                .authorities(new SimpleGrantedAuthority("ROLE_USER"))
                .build();

        // When
        boolean isValid = jwtService.isTokenValid(token, differentUser);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    void isTokenValid_WithExpiredToken_ShouldReturnFalse() {
        // Given
        SecretKey key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(TEST_SECRET));
        String expiredToken = Jwts.builder()
                .subject("testuser")
                .issuedAt(new Date(System.currentTimeMillis() - 10000))
                .expiration(new Date(System.currentTimeMillis() - 5000)) // Expired
                .signWith(key)
                .compact();

        // When
        boolean isValid = jwtService.isTokenValid(expiredToken, testUserDetails);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    void isTokenValid_WithInvalidSignature_ShouldReturnFalse() {
        // Given - Create token with different secret
        SecretKey differentKey = Jwts.SIG.HS256.key().build();
        String tokenWithWrongSignature = Jwts.builder()
                .subject("testuser")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(differentKey)
                .compact();

        // When
        boolean isValid = jwtService.isTokenValid(tokenWithWrongSignature, testUserDetails);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    void isTokenValid_WithMalformedToken_ShouldReturnFalse() {
        // Given
        String malformedToken = "not.a.valid.jwt.token";

        // When
        boolean isValid = jwtService.isTokenValid(malformedToken, testUserDetails);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    void isTokenValid_WithNullToken_ShouldReturnFalse() {
        // When
        boolean isValid = jwtService.isTokenValid(null, testUserDetails);

        // Then
        assertThat(isValid).isFalse();
    }

    // ===== TOKEN SIGNATURE VERIFICATION TESTS =====

    @Test
    void generateToken_ShouldProduceVerifiableSignature() {
        // Given
        String token = jwtService.generateToken(testUserDetails);

        // When - Parse token to verify signature
        SecretKey key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(TEST_SECRET));
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        // Then - If parsing succeeds, signature is valid
        assertThat(claims).isNotNull();
        assertThat(claims.getSubject()).isEqualTo("testuser");
    }

    @Test
    void tokensForSameUser_ShouldBeDifferent() {
        // When - Generate two tokens for same user at different times
        String token1 = jwtService.generateToken(testUserDetails);

        // Delay to ensure different issuedAt timestamp (JWT uses seconds)
        try {
            Thread.sleep(1001); // Sleep for just over 1 second
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        String token2 = jwtService.generateToken(testUserDetails);

        // Then - Tokens should be different (different issuedAt timestamps)
        assertThat(token1).isNotEqualTo(token2);

        // But both should be valid for the same user
        assertThat(jwtService.isTokenValid(token1, testUserDetails)).isTrue();
        assertThat(jwtService.isTokenValid(token2, testUserDetails)).isTrue();
    }
}
