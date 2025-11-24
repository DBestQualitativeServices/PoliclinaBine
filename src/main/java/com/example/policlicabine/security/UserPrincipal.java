package com.example.policlicabine.security;

import com.example.policlicabine.entity.User;
import org.springframework.security.core.GrantedAuthority;

import java.util.*;

public class UserPrincipal extends org.springframework.security.core.userdetails.User {

    private final UUID userId;
    private final Map<String, UUID> profileIds;

    private UserPrincipal(String username,
                         String password,
                         UUID userId,
                         Map<String, UUID> profileIds,
                         boolean enabled,
                         boolean accountNonLocked,
                         Collection<? extends GrantedAuthority> authorities) {
        super(username,
              password != null ? password : "",
              enabled,
              true,
              true,
              accountNonLocked,
              authorities);
        this.userId = userId;
        this.profileIds = Collections.unmodifiableMap(new HashMap<>(profileIds));
    }

    public UserPrincipal(User user, Collection<? extends GrantedAuthority> authorities) {
        this(user.getUsername(),
             user.getPassword(),
             user.getUserId(),
             buildProfileMap(user),
             user.isEnabled(),
             user.isAccountNonLocked(),
             authorities);
    }

    public static UserPrincipal fromJwtClaims(String username,
                                              UUID userId,
                                              Map<String, UUID> profileIds,
                                              Collection<? extends GrantedAuthority> authorities) {
        return new UserPrincipal(username, null, userId, profileIds, true, true, authorities);
    }

    private static Map<String, UUID> buildProfileMap(User user) {
        Map<String, UUID> profiles = new HashMap<>();
        if (user.getDoctorProfile() != null) {
            profiles.put("DOCTOR", user.getDoctorProfile().getDoctorId());
        }
        if (user.getPatientProfile() != null) {
            profiles.put("PATIENT", user.getPatientProfile().getPatientId());
        }
        if (user.getManagerProfile() != null) {
            profiles.put("MANAGER", user.getManagerProfile().getManagerId());
        }
        return profiles;
    }

    public UUID getUserId() {
        return userId;
    }

    public Optional<UUID> getProfileId(String profileType) {
        return Optional.ofNullable(profileIds.get(profileType));
    }

    public boolean hasProfile(String profileType) {
        return profileIds.containsKey(profileType);
    }

    public Map<String, UUID> getProfileIds() {
        return profileIds;
    }
}
