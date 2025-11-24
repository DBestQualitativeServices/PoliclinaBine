package com.example.policlicabine.security;

import com.example.policlicabine.config.properties.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.*;
import java.util.function.Function;

@Service
@Slf4j
@RequiredArgsConstructor
public class JwtService {

    private final JwtProperties jwtProperties;

    public String generateToken(UserDetails userDetails, String userId) {
        Map<String, Object> claims = new HashMap<>();

        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith("ROLE_"))
                .toList();

        List<String> permissions = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> !authority.startsWith("ROLE_") && !authority.startsWith("PROFILE_"))
                .distinct()
                .toList();

        List<String> profileAuthorities = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith("PROFILE_"))
                .toList();

        claims.put("userId", userId);
        claims.put("roles", roles);
        claims.put("permissions", permissions);
        claims.put("profileAuthorities", profileAuthorities);

        if (userDetails instanceof UserPrincipal) {
            UserPrincipal principal = (UserPrincipal) userDetails;
            Map<String, String> profileIds = new HashMap<>();
            principal.getProfileIds().forEach((key, value) -> profileIds.put(key, value.toString()));
            claims.put("profiles", profileIds);
        }

        return createToken(claims, userDetails.getUsername(), jwtProperties.getExpiration());
    }

    public String generateRefreshToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("tokenType", "refresh");
        return createToken(claims, userDetails.getUsername(), jwtProperties.getRefreshExpiration());
    }

    private String createToken(Map<String, Object> claims, String subject, Long expirationTime) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationTime);

        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {
        Claims claims = extractAllClaims(token);
        return claims.get("roles", List.class);
    }

    @SuppressWarnings("unchecked")
    public List<String> extractPermissions(String token) {
        Claims claims = extractAllClaims(token);
        return claims.get("permissions", List.class);
    }

    public String extractUserId(String token) {
        Claims claims = extractAllClaims(token);
        return claims.get("userId", String.class);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isTokenExpired(String token) {
        try {
            return extractExpiration(token).before(new Date());
        } catch (Exception e) {
            log.warn("Error checking token expiration: {}", e.getMessage());
            return true;
        }
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            final String username = extractUsername(token);
            return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
        } catch (Exception e) {
            log.warn("Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    public boolean isTokenValid(String token) {
        try {
            extractAllClaims(token);
            return !isTokenExpired(token);
        } catch (Exception e) {
            log.warn("Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    public UserPrincipal buildPrincipalFromToken(String token) {
        Claims claims = extractAllClaims(token);

        String username = claims.getSubject();
        UUID userId = UUID.fromString(claims.get("userId", String.class));

        Set<GrantedAuthority> authorities = new HashSet<>();

        List<String> roles = claims.get("roles", List.class);
        if (roles != null) {
            roles.forEach(role -> authorities.add(new SimpleGrantedAuthority(role)));
        }

        List<String> permissions = claims.get("permissions", List.class);
        if (permissions != null) {
            permissions.forEach(perm -> authorities.add(new SimpleGrantedAuthority(perm)));
        }

        List<String> profileAuthorities = claims.get("profileAuthorities", List.class);
        if (profileAuthorities != null) {
            profileAuthorities.forEach(auth -> authorities.add(new SimpleGrantedAuthority(auth)));
        }

        Map<String, String> profileIdsRaw = claims.get("profiles", Map.class);
        Map<String, UUID> profileIds = new HashMap<>();
        if (profileIdsRaw != null) {
            profileIdsRaw.forEach((key, value) -> profileIds.put(key, UUID.fromString(value)));
        }

        return UserPrincipal.fromJwtClaims(username, userId, profileIds, authorities);
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtProperties.getSecret());
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
