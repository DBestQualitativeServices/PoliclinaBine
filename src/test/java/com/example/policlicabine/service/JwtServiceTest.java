package com.example.policlicabine.service;

import com.example.policlicabine.config.properties.JwtProperties;
import com.example.policlicabine.security.JwtService;
import com.example.policlicabine.security.UserPrincipal;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import javax.crypto.SecretKey;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;
    private UserPrincipal testUserPrincipal;
    private static final UUID TEST_USER_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
    private static final UUID TEST_DOCTOR_ID = UUID.fromString("223e4567-e89b-12d3-a456-426614174000");

    // Test configuration
    private static final String TEST_SECRET = "dGVzdC1zZWNyZXQta2V5LW11c3QtYmUtYXQtbGVhc3QtMjU2LWJpdHMtbG9uZy1mb3ItSFMyNTYtYWxnb3JpdGht";
    private static final long ACCESS_TOKEN_EXPIRATION = 1800000L; // 30 minutes
    private static final long REFRESH_TOKEN_EXPIRATION = 604800000L; // 7 days

    @BeforeEach
    void setUp() {
        JwtProperties jwtProperties = new JwtProperties();
        jwtProperties.setSecret(TEST_SECRET);
        jwtProperties.setExpiration(ACCESS_TOKEN_EXPIRATION);
        jwtProperties.setRefreshExpiration(REFRESH_TOKEN_EXPIRATION);

        jwtService = new JwtService(jwtProperties);

        Map<String, UUID> profileIds = new HashMap<>();
        profileIds.put("DOCTOR", TEST_DOCTOR_ID);

        Collection<SimpleGrantedAuthority> authorities = Arrays.asList(
                new SimpleGrantedAuthority("ROLE_DOCTOR"),
                new SimpleGrantedAuthority("PROFILE_DOCTOR"),
                new SimpleGrantedAuthority("ALL")
        );

        testUserPrincipal = UserPrincipal.fromJwtClaims(
                "testuser",
                TEST_USER_ID,
                profileIds,
                authorities
        );
    }

    // ===== GENERATE TOKEN TESTS =====

    @Test
    void generateToken_ShouldCreateValidAccessToken() {
        String token = jwtService.generateToken(testUserPrincipal, TEST_USER_ID.toString());

        assertThat(token).isNotNull();
        assertThat(token).isNotBlank();
        assertThat(token.split("\\.")).hasSize(3);

        SecretKey key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(TEST_SECRET));
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        assertThat(claims.getSubject()).isEqualTo("testuser");
        assertThat(claims.get("userId", String.class)).isEqualTo(TEST_USER_ID.toString());
        assertThat(claims.get("roles", List.class)).contains("ROLE_DOCTOR");
        assertThat(claims.get("profileAuthorities", List.class)).contains("PROFILE_DOCTOR");
        assertThat(claims.get("permissions", List.class)).contains("ALL");

        Map<String, String> profiles = claims.get("profiles", Map.class);
        assertThat(profiles).containsEntry("DOCTOR", TEST_DOCTOR_ID.toString());

        assertThat(claims.getIssuedAt()).isNotNull();
        assertThat(claims.getExpiration()).isNotNull();

        long expectedExpiration = System.currentTimeMillis() + ACCESS_TOKEN_EXPIRATION;
        long actualExpiration = claims.getExpiration().getTime();
        assertThat(actualExpiration).isBetween(
                expectedExpiration - 5000,
                expectedExpiration + 5000
        );
    }

    @Test
    void generateToken_WithMultipleRoles_ShouldIncludeAllRoles() {
        Collection<SimpleGrantedAuthority> multipleAuthorities = Arrays.asList(
                new SimpleGrantedAuthority("ROLE_ADMIN"),
                new SimpleGrantedAuthority("ROLE_MANAGER"),
                new SimpleGrantedAuthority("PROFILE_MANAGER")
        );

        UserPrincipal multiRolePrincipal = UserPrincipal.fromJwtClaims(
                "admin",
                TEST_USER_ID,
                Collections.emptyMap(),
                multipleAuthorities
        );

        String token = jwtService.generateToken(multiRolePrincipal, TEST_USER_ID.toString());

        SecretKey key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(TEST_SECRET));
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        assertThat(claims.get("roles", List.class)).contains("ROLE_ADMIN", "ROLE_MANAGER");
        assertThat(claims.get("profileAuthorities", List.class)).contains("PROFILE_MANAGER");
    }

    // ===== GENERATE REFRESH TOKEN TESTS =====

    @Test
    void generateRefreshToken_ShouldCreateValidRefreshToken() {
        String token = jwtService.generateRefreshToken(testUserPrincipal);

        assertThat(token).isNotNull();
        assertThat(token).isNotBlank();
        assertThat(token.split("\\.")).hasSize(3);

        SecretKey key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(TEST_SECRET));
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        assertThat(claims.getSubject()).isEqualTo("testuser");
        assertThat(claims.get("tokenType")).isEqualTo("refresh");

        long expectedExpiration = System.currentTimeMillis() + REFRESH_TOKEN_EXPIRATION;
        long actualExpiration = claims.getExpiration().getTime();
        assertThat(actualExpiration).isBetween(
                expectedExpiration - 5000,
                expectedExpiration + 5000
        );
    }

    @Test
    void generateRefreshToken_ShouldHaveLongerExpirationThanAccessToken() {
        String accessToken = jwtService.generateToken(testUserPrincipal, TEST_USER_ID.toString());
        String refreshToken = jwtService.generateRefreshToken(testUserPrincipal);

        Date accessExpiration = jwtService.extractExpiration(accessToken);
        Date refreshExpiration = jwtService.extractExpiration(refreshToken);

        assertThat(refreshExpiration).isAfter(accessExpiration);

        long diffMillis = refreshExpiration.getTime() - accessExpiration.getTime();
        long expectedDiff = REFRESH_TOKEN_EXPIRATION - ACCESS_TOKEN_EXPIRATION;
        assertThat(diffMillis).isBetween(
                expectedDiff - 10000,
                expectedDiff + 10000
        );
    }

    // ===== EXTRACT USERNAME TESTS =====

    @Test
    void extractUsername_FromValidToken_ShouldReturnCorrectUsername() {
        String token = jwtService.generateToken(testUserPrincipal, TEST_USER_ID.toString());

        String username = jwtService.extractUsername(token);

        assertThat(username).isEqualTo("testuser");
    }

    @Test
    void extractUsername_FromDifferentUsers_ShouldReturnDifferentUsernames() {
        UserPrincipal user1 = UserPrincipal.fromJwtClaims(
                "user1",
                UUID.randomUUID(),
                Collections.emptyMap(),
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );

        UserPrincipal user2 = UserPrincipal.fromJwtClaims(
                "user2",
                UUID.randomUUID(),
                Collections.emptyMap(),
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );

        String token1 = jwtService.generateToken(user1, user1.getUserId().toString());
        String token2 = jwtService.generateToken(user2, user2.getUserId().toString());

        assertThat(jwtService.extractUsername(token1)).isEqualTo("user1");
        assertThat(jwtService.extractUsername(token2)).isEqualTo("user2");
    }

    // ===== EXTRACT USER ID TESTS =====

    @Test
    void extractUserId_FromValidToken_ShouldReturnCorrectUserId() {
        String token = jwtService.generateToken(testUserPrincipal, TEST_USER_ID.toString());

        String userId = jwtService.extractUserId(token);

        assertThat(userId).isEqualTo(TEST_USER_ID.toString());
    }

    @Test
    void extractUserId_FromTokensWithDifferentUserIds_ShouldReturnCorrectIds() {
        String userId1 = "user-id-1";
        String userId2 = "user-id-2";

        String token1 = jwtService.generateToken(testUserPrincipal, userId1);
        String token2 = jwtService.generateToken(testUserPrincipal, userId2);

        String extractedId1 = jwtService.extractUserId(token1);
        String extractedId2 = jwtService.extractUserId(token2);

        assertThat(extractedId1).isEqualTo(userId1);
        assertThat(extractedId2).isEqualTo(userId2);
    }

    // ===== EXTRACT EXPIRATION TESTS =====

    @Test
    void extractExpiration_FromValidToken_ShouldReturnFutureDate() {
        String token = jwtService.generateToken(testUserPrincipal, TEST_USER_ID.toString());
        Date now = new Date();

        Date expiration = jwtService.extractExpiration(token);

        assertThat(expiration).isAfter(now);
        assertThat(expiration.getTime()).isBetween(
                now.getTime() + ACCESS_TOKEN_EXPIRATION - 5000,
                now.getTime() + ACCESS_TOKEN_EXPIRATION + 5000
        );
    }

    // ===== IS TOKEN EXPIRED TESTS =====

    @Test
    void isTokenExpired_WithValidToken_ShouldReturnFalse() {
        String token = jwtService.generateToken(testUserPrincipal, TEST_USER_ID.toString());

        boolean isExpired = jwtService.isTokenExpired(token);

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
        String token = jwtService.generateToken(testUserPrincipal, TEST_USER_ID.toString());

        boolean isValid = jwtService.isTokenValid(token, testUserPrincipal);

        assertThat(isValid).isTrue();
    }

    @Test
    void isTokenValid_WithValidTokenButDifferentUser_ShouldReturnFalse() {
        String token = jwtService.generateToken(testUserPrincipal, TEST_USER_ID.toString());

        UserPrincipal differentUser = UserPrincipal.fromJwtClaims(
                "differentuser",
                UUID.randomUUID(),
                Collections.emptyMap(),
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );

        boolean isValid = jwtService.isTokenValid(token, differentUser);

        assertThat(isValid).isFalse();
    }

    @Test
    void isTokenValid_WithExpiredToken_ShouldReturnFalse() {
        SecretKey key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(TEST_SECRET));
        String expiredToken = Jwts.builder()
                .subject("testuser")
                .issuedAt(new Date(System.currentTimeMillis() - 10000))
                .expiration(new Date(System.currentTimeMillis() - 5000))
                .signWith(key)
                .compact();

        boolean isValid = jwtService.isTokenValid(expiredToken, testUserPrincipal);

        assertThat(isValid).isFalse();
    }

    @Test
    void isTokenValid_WithInvalidSignature_ShouldReturnFalse() {
        SecretKey differentKey = Jwts.SIG.HS256.key().build();
        String tokenWithWrongSignature = Jwts.builder()
                .subject("testuser")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(differentKey)
                .compact();

        boolean isValid = jwtService.isTokenValid(tokenWithWrongSignature, testUserPrincipal);

        assertThat(isValid).isFalse();
    }

    @Test
    void isTokenValid_WithMalformedToken_ShouldReturnFalse() {
        String malformedToken = "not.a.valid.jwt.token";

        boolean isValid = jwtService.isTokenValid(malformedToken, testUserPrincipal);

        assertThat(isValid).isFalse();
    }

    @Test
    void isTokenValid_WithNullToken_ShouldReturnFalse() {
        boolean isValid = jwtService.isTokenValid(null, testUserPrincipal);

        assertThat(isValid).isFalse();
    }

    // ===== TOKEN SIGNATURE VERIFICATION TESTS =====

    @Test
    void generateToken_ShouldProduceVerifiableSignature() {
        String token = jwtService.generateToken(testUserPrincipal, TEST_USER_ID.toString());

        SecretKey key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(TEST_SECRET));
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        assertThat(claims).isNotNull();
        assertThat(claims.getSubject()).isEqualTo("testuser");
    }

    @Test
    void tokensForSameUser_ShouldBeDifferent() {
        String token1 = jwtService.generateToken(testUserPrincipal, TEST_USER_ID.toString());

        try {
            Thread.sleep(1001);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        String token2 = jwtService.generateToken(testUserPrincipal, TEST_USER_ID.toString());

        assertThat(token1).isNotEqualTo(token2);
        assertThat(jwtService.isTokenValid(token1)).isTrue();
        assertThat(jwtService.isTokenValid(token2)).isTrue();
    }

    @Test
    void buildPrincipalFromToken_ShouldReconstructUserPrincipal() {
        String token = jwtService.generateToken(testUserPrincipal, TEST_USER_ID.toString());

        UserPrincipal reconstructed = jwtService.buildPrincipalFromToken(token);

        assertThat(reconstructed).isNotNull();
        assertThat(reconstructed.getUsername()).isEqualTo("testuser");
        assertThat(reconstructed.getUserId()).isEqualTo(TEST_USER_ID);
        assertThat(reconstructed.getAuthorities()).hasSize(3);
        assertThat(reconstructed.hasProfile("DOCTOR")).isTrue();
        assertThat(reconstructed.getProfileId("DOCTOR")).contains(TEST_DOCTOR_ID);
    }

    @Test
    void isTokenValid_WithoutUserDetails_ShouldValidateCorrectly() {
        String validToken = jwtService.generateToken(testUserPrincipal, TEST_USER_ID.toString());

        assertThat(jwtService.isTokenValid(validToken)).isTrue();

        SecretKey key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(TEST_SECRET));
        String expiredToken = Jwts.builder()
                .subject("testuser")
                .issuedAt(new Date(System.currentTimeMillis() - 10000))
                .expiration(new Date(System.currentTimeMillis() - 5000))
                .signWith(key)
                .compact();

        assertThat(jwtService.isTokenValid(expiredToken)).isFalse();
    }
}
