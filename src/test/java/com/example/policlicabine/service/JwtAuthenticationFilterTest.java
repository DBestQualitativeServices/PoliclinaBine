package com.example.policlicabine.service;

import com.example.policlicabine.security.JwtAuthenticationFilter;
import com.example.policlicabine.security.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import com.example.policlicabine.security.UserPrincipal;

import java.io.IOException;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link JwtAuthenticationFilter}.
 * Tests JWT authentication filter behavior with UserPrincipal from JWT claims (no DB query).
 */
@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private UserPrincipal testUserPrincipal;

    @BeforeEach
    void setUp() {
        jwtAuthenticationFilter = new JwtAuthenticationFilter(jwtService);

        Map<String, UUID> profileIds = new HashMap<>();
        profileIds.put("DOCTOR", UUID.randomUUID());

        testUserPrincipal = UserPrincipal.fromJwtClaims(
                "testuser",
                UUID.randomUUID(),
                profileIds,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_DOCTOR"))
        );

        // Clear SecurityContext before each test
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        // Clear SecurityContext after each test
        SecurityContextHolder.clearContext();
    }

    // ===== VALID JWT TESTS =====

    @Test
    void doFilterInternal_WithValidJwt_ShouldSetAuthentication() throws ServletException, IOException {
        String validToken = "valid.jwt.token";
        String authHeader = "Bearer " + validToken;

        when(request.getHeader("Authorization")).thenReturn(authHeader);
        when(jwtService.isTokenValid(validToken)).thenReturn(true);
        when(jwtService.buildPrincipalFromToken(validToken)).thenReturn(testUserPrincipal);

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getPrincipal()).isEqualTo(testUserPrincipal);
        assertThat(authentication.isAuthenticated()).isTrue();
        assertThat(authentication.getAuthorities().stream()
                .map(org.springframework.security.core.GrantedAuthority::getAuthority)
                .toList()).containsExactly("ROLE_DOCTOR");

        verify(filterChain).doFilter(request, response);
        verify(jwtService).isTokenValid(validToken);
        verify(jwtService).buildPrincipalFromToken(validToken);
    }

    @Test
    void doFilterInternal_WithValidJwt_ShouldNotBuildPrincipalIfAlreadyAuthenticated() throws ServletException, IOException {
        String validToken = "valid.jwt.token";
        String authHeader = "Bearer " + validToken;

        Authentication existingAuth = org.springframework.security.authentication.UsernamePasswordAuthenticationToken
                .authenticated(testUserPrincipal, null, testUserPrincipal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(existingAuth);

        when(request.getHeader("Authorization")).thenReturn(authHeader);

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        verify(jwtService, never()).isTokenValid(anyString());
        verify(jwtService, never()).buildPrincipalFromToken(anyString());
        verify(filterChain).doFilter(request, response);
    }

    // ===== MISSING/INVALID HEADER TESTS =====

    @Test
    void doFilterInternal_WithNoAuthorizationHeader_ShouldContinueWithoutAuthentication() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn(null);

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNull();

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtService);
    }

    @Test
    void doFilterInternal_WithNonBearerToken_ShouldContinueWithoutAuthentication() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn("Basic dXNlcjpwYXNz");

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNull();

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtService);
    }

    @Test
    void doFilterInternal_WithEmptyAuthorizationHeader_ShouldContinueWithoutAuthentication() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn("");

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNull();

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtService);
    }

    @Test
    void doFilterInternal_WithBearerOnlyHeader_ShouldContinueWithoutAuthentication() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn("Bearer");

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNull();

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtService);
    }

    // ===== INVALID JWT TESTS =====

    @Test
    void doFilterInternal_WithInvalidJwt_ShouldContinueWithoutAuthentication() throws ServletException, IOException {
        String invalidToken = "invalid.jwt.token";
        String authHeader = "Bearer " + invalidToken;

        when(request.getHeader("Authorization")).thenReturn(authHeader);
        when(jwtService.isTokenValid(invalidToken)).thenReturn(false);

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNull();

        verify(filterChain).doFilter(request, response);
        verify(jwtService).isTokenValid(invalidToken);
        verify(jwtService, never()).buildPrincipalFromToken(anyString());
    }

    @Test
    void doFilterInternal_WithExpiredJwt_ShouldContinueWithoutAuthentication() throws ServletException, IOException {
        String expiredToken = "expired.jwt.token";
        String authHeader = "Bearer " + expiredToken;

        when(request.getHeader("Authorization")).thenReturn(authHeader);
        when(jwtService.isTokenValid(expiredToken)).thenReturn(false);

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNull();

        verify(filterChain).doFilter(request, response);
        verify(jwtService).isTokenValid(expiredToken);
        verify(jwtService, never()).buildPrincipalFromToken(anyString());
    }

    @Test
    void doFilterInternal_WithJwtBuildPrincipalException_ShouldContinueWithoutAuthentication() throws ServletException, IOException {
        String token = "valid.jwt.token";
        String authHeader = "Bearer " + token;

        when(request.getHeader("Authorization")).thenReturn(authHeader);
        when(jwtService.isTokenValid(token)).thenReturn(true);
        when(jwtService.buildPrincipalFromToken(token))
                .thenThrow(new RuntimeException("Failed to build principal"));

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNull();

        verify(filterChain).doFilter(request, response);
    }

    // ===== EXCEPTION HANDLING TESTS =====

    @Test
    void doFilterInternal_WithMalformedJwt_ShouldContinueWithoutAuthentication() throws ServletException, IOException {
        String malformedToken = "not.a.jwt";
        String authHeader = "Bearer " + malformedToken;

        when(request.getHeader("Authorization")).thenReturn(authHeader);
        when(jwtService.isTokenValid(malformedToken))
                .thenThrow(new io.jsonwebtoken.MalformedJwtException("Invalid JWT"));

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNull();

        verify(filterChain).doFilter(request, response);
    }

    // ===== AUTHENTICATION DETAILS TESTS =====

    @Test
    void doFilterInternal_WithValidJwt_ShouldSetCorrectAuthenticationDetails() throws ServletException, IOException {
        String validToken = "valid.jwt.token";
        String authHeader = "Bearer " + validToken;

        Map<String, UUID> adminProfileIds = new HashMap<>();
        adminProfileIds.put("MANAGER", UUID.randomUUID());

        UserPrincipal adminUser = UserPrincipal.fromJwtClaims(
                "admin",
                UUID.randomUUID(),
                adminProfileIds,
                Arrays.asList(
                        new SimpleGrantedAuthority("ROLE_ADMIN"),
                        new SimpleGrantedAuthority("ROLE_MANAGER")
                )
        );

        when(request.getHeader("Authorization")).thenReturn(authHeader);
        when(jwtService.isTokenValid(validToken)).thenReturn(true);
        when(jwtService.buildPrincipalFromToken(validToken)).thenReturn(adminUser);

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getPrincipal()).isEqualTo(adminUser);
        assertThat(authentication.getCredentials()).isNull();
        assertThat(authentication.getAuthorities()).hasSize(2);
        assertThat(authentication.getAuthorities().stream()
                .map(org.springframework.security.core.GrantedAuthority::getAuthority)
                .toList()).containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_MANAGER");

        verify(filterChain).doFilter(request, response);
    }

    // ===== FILTER CHAIN CONTINUATION =====

    @Test
    void doFilterInternal_ShouldAlwaysContinueFilterChain() throws ServletException, IOException {
        String[] testCases = {
                null,
                "",
                "Bearer",
                "Basic dXNlcjpwYXNz",
                "Bearer invalid.token"
        };

        for (String authHeader : testCases) {
            SecurityContextHolder.clearContext();
            reset(filterChain);

            when(request.getHeader("Authorization")).thenReturn(authHeader);
            if (authHeader != null && authHeader.startsWith("Bearer ") && authHeader.length() > 7) {
                String token = authHeader.substring(7);
                when(jwtService.isTokenValid(token)).thenReturn(false);
            }

            jwtAuthenticationFilter.doFilter(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
        }
    }
}
