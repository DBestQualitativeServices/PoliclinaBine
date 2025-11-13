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
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link JwtAuthenticationFilter}.
 * <p>
 * Tests JWT authentication filter behavior:
 * - Extracting JWT from Authorization header
 * - Validating JWT and setting authentication context
 * - Continuing filter chain with/without authentication
 * - Handling missing, invalid, or expired tokens
 * - Handling non-Bearer tokens
 * <p>
 * Uses Mockito mocks for JwtService, UserDetailsService, and servlet components.
 */
@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private UserDetails testUserDetails;

    @BeforeEach
    void setUp() {
        jwtAuthenticationFilter = new JwtAuthenticationFilter(jwtService, userDetailsService);

        testUserDetails = User.builder()
                .username("testuser")
                .password("password")
                .authorities(new SimpleGrantedAuthority("ROLE_DOCTOR"))
                .build();

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
        // Given
        String validToken = "valid.jwt.token";
        String authHeader = "Bearer " + validToken;

        when(request.getHeader("Authorization")).thenReturn(authHeader);
        when(jwtService.extractUsername(validToken)).thenReturn("testuser");
        when(userDetailsService.loadUserByUsername("testuser")).thenReturn(testUserDetails);
        when(jwtService.isTokenValid(validToken, testUserDetails)).thenReturn(true);

        // When
        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        // Then
        // Verify authentication set in SecurityContext
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getPrincipal()).isEqualTo(testUserDetails);
        assertThat(authentication.isAuthenticated()).isTrue();
        assertThat(authentication.getAuthorities().stream()
                .map(org.springframework.security.core.GrantedAuthority::getAuthority)
                .toList()).containsExactly("ROLE_DOCTOR");

        // Verify filter chain continued
        verify(filterChain).doFilter(request, response);

        // Verify services called
        verify(jwtService).extractUsername(validToken);
        verify(userDetailsService).loadUserByUsername("testuser");
        verify(jwtService).isTokenValid(validToken, testUserDetails);
    }

    @Test
    void doFilterInternal_WithValidJwt_ShouldNotLoadUserTwice() throws ServletException, IOException {
        // Given - Already authenticated user
        String validToken = "valid.jwt.token";
        String authHeader = "Bearer " + validToken;

        // Set existing authentication
        Authentication existingAuth = org.springframework.security.authentication.UsernamePasswordAuthenticationToken
                .authenticated(testUserDetails, null, testUserDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(existingAuth);

        when(request.getHeader("Authorization")).thenReturn(authHeader);
        when(jwtService.extractUsername(validToken)).thenReturn("testuser");

        // When
        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        // Then
        // Should not load user details or validate token again
        verify(userDetailsService, never()).loadUserByUsername(anyString());
        verify(jwtService, never()).isTokenValid(anyString(), any());

        // Filter chain should still continue
        verify(filterChain).doFilter(request, response);
    }

    // ===== MISSING/INVALID HEADER TESTS =====

    @Test
    void doFilterInternal_WithNoAuthorizationHeader_ShouldContinueWithoutAuthentication() throws ServletException, IOException {
        // Given
        when(request.getHeader("Authorization")).thenReturn(null);

        // When
        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        // Then
        // No authentication set
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNull();

        // Filter chain continued
        verify(filterChain).doFilter(request, response);

        // No JWT service calls
        verifyNoInteractions(jwtService);
        verifyNoInteractions(userDetailsService);
    }

    @Test
    void doFilterInternal_WithNonBearerToken_ShouldContinueWithoutAuthentication() throws ServletException, IOException {
        // Given - Authorization header with non-Bearer scheme
        when(request.getHeader("Authorization")).thenReturn("Basic dXNlcjpwYXNz");

        // When
        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        // Then
        // No authentication set
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNull();

        // Filter chain continued
        verify(filterChain).doFilter(request, response);

        // No JWT service calls
        verifyNoInteractions(jwtService);
        verifyNoInteractions(userDetailsService);
    }

    @Test
    void doFilterInternal_WithEmptyAuthorizationHeader_ShouldContinueWithoutAuthentication() throws ServletException, IOException {
        // Given
        when(request.getHeader("Authorization")).thenReturn("");

        // When
        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        // Then
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNull();

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtService);
        verifyNoInteractions(userDetailsService);
    }

    @Test
    void doFilterInternal_WithBearerOnlyHeader_ShouldContinueWithoutAuthentication() throws ServletException, IOException {
        // Given - "Bearer" without token
        when(request.getHeader("Authorization")).thenReturn("Bearer");

        // When
        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        // Then
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNull();

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtService);
        verifyNoInteractions(userDetailsService);
    }

    // ===== INVALID JWT TESTS =====

    @Test
    void doFilterInternal_WithInvalidJwt_ShouldContinueWithoutAuthentication() throws ServletException, IOException {
        // Given
        String invalidToken = "invalid.jwt.token";
        String authHeader = "Bearer " + invalidToken;

        when(request.getHeader("Authorization")).thenReturn(authHeader);
        when(jwtService.extractUsername(invalidToken)).thenReturn("testuser");
        when(userDetailsService.loadUserByUsername("testuser")).thenReturn(testUserDetails);
        when(jwtService.isTokenValid(invalidToken, testUserDetails)).thenReturn(false);

        // When
        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        // Then
        // No authentication set (invalid token)
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNull();

        // Filter chain still continued
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_WithExpiredJwt_ShouldContinueWithoutAuthentication() throws ServletException, IOException {
        // Given
        String expiredToken = "expired.jwt.token";
        String authHeader = "Bearer " + expiredToken;

        when(request.getHeader("Authorization")).thenReturn(authHeader);
        when(jwtService.extractUsername(expiredToken)).thenReturn("testuser");
        when(userDetailsService.loadUserByUsername("testuser")).thenReturn(testUserDetails);
        when(jwtService.isTokenValid(expiredToken, testUserDetails)).thenReturn(false);

        // When
        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        // Then
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNull();

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_WithJwtForNonExistentUser_ShouldContinueWithoutAuthentication() throws ServletException, IOException {
        // Given
        String token = "valid.jwt.token";
        String authHeader = "Bearer " + token;

        when(request.getHeader("Authorization")).thenReturn(authHeader);
        when(jwtService.extractUsername(token)).thenReturn("nonexistent");
        when(userDetailsService.loadUserByUsername("nonexistent"))
                .thenThrow(new org.springframework.security.core.userdetails.UsernameNotFoundException("User not found"));

        // When
        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        // Then
        // Exception should be caught, no authentication set
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNull();

        // Filter chain should still continue (fail gracefully)
        verify(filterChain).doFilter(request, response);
    }

    // ===== EXCEPTION HANDLING TESTS =====

    @Test
    void doFilterInternal_WithMalformedJwt_ShouldContinueWithoutAuthentication() throws ServletException, IOException {
        // Given
        String malformedToken = "not.a.jwt";
        String authHeader = "Bearer " + malformedToken;

        when(request.getHeader("Authorization")).thenReturn(authHeader);
        when(jwtService.extractUsername(malformedToken))
                .thenThrow(new io.jsonwebtoken.MalformedJwtException("Invalid JWT"));

        // When
        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        // Then
        // Exception caught, no authentication set
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNull();

        // Filter chain continued (fail gracefully)
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_WithNullUsernameFromJwt_ShouldContinueWithoutAuthentication() throws ServletException, IOException {
        // Given
        String token = "token.with.no.username";
        String authHeader = "Bearer " + token;

        when(request.getHeader("Authorization")).thenReturn(authHeader);
        when(jwtService.extractUsername(token)).thenReturn(null);

        // When
        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        // Then
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNull();

        // Should not try to load user details with null username
        verify(userDetailsService, never()).loadUserByUsername(any());

        verify(filterChain).doFilter(request, response);
    }

    // ===== AUTHENTICATION DETAILS TESTS =====

    @Test
    void doFilterInternal_WithValidJwt_ShouldSetCorrectAuthenticationDetails() throws ServletException, IOException {
        // Given
        String validToken = "valid.jwt.token";
        String authHeader = "Bearer " + validToken;

        UserDetails adminUser = User.builder()
                .username("admin")
                .password("password")
                .authorities(
                        new SimpleGrantedAuthority("ROLE_ADMIN"),
                        new SimpleGrantedAuthority("ROLE_MANAGER")
                )
                .build();

        when(request.getHeader("Authorization")).thenReturn(authHeader);
        when(jwtService.extractUsername(validToken)).thenReturn("admin");
        when(userDetailsService.loadUserByUsername("admin")).thenReturn(adminUser);
        when(jwtService.isTokenValid(validToken, adminUser)).thenReturn(true);

        // When
        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        // Then
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getPrincipal()).isEqualTo(adminUser);
        assertThat(authentication.getCredentials()).isNull(); // Credentials cleared for security
        assertThat(authentication.getAuthorities()).hasSize(2);
        assertThat(authentication.getAuthorities().stream()
                .map(org.springframework.security.core.GrantedAuthority::getAuthority)
                .toList()).containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_MANAGER");

        verify(filterChain).doFilter(request, response);
    }

    // ===== FILTER CHAIN CONTINUATION =====

    @Test
    void doFilterInternal_ShouldAlwaysContinueFilterChain() throws ServletException, IOException {
        // Given - Various scenarios should all continue the filter chain
        String[] testCases = {
                null,                          // No header
                "",                            // Empty header
                "Bearer",                      // Bearer without token
                "Basic dXNlcjpwYXNz",         // Non-Bearer
                "Bearer invalid.token"         // Bearer with token (even if invalid)
        };

        for (String authHeader : testCases) {
            // Reset
            SecurityContextHolder.clearContext();
            reset(filterChain);

            // Given
            when(request.getHeader("Authorization")).thenReturn(authHeader);
            if (authHeader != null && authHeader.startsWith("Bearer ") && authHeader.length() > 7) {
                String token = authHeader.substring(7);
                when(jwtService.extractUsername(token)).thenReturn("user");
                when(userDetailsService.loadUserByUsername("user")).thenReturn(testUserDetails);
                when(jwtService.isTokenValid(token, testUserDetails)).thenReturn(false);
            }

            // When
            jwtAuthenticationFilter.doFilter(request, response, filterChain);

            // Then
            verify(filterChain).doFilter(request, response);
        }
    }
}
