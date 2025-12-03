package com.example.policlicabine.service;

import com.example.policlicabine.dto.*;
import com.example.policlicabine.entity.PasswordResetToken;
import com.example.policlicabine.entity.Role;
import com.example.policlicabine.entity.User;
import com.example.policlicabine.entity.enums.Specialty;
import com.example.policlicabine.entity.enums.UserRole;
import com.example.policlicabine.event.*;
import com.example.policlicabine.exception.BusinessException;
import com.example.policlicabine.exception.ResourceNotFoundException;
import com.example.policlicabine.repository.RoleRepository;
import com.example.policlicabine.repository.UserRepository;
import com.example.policlicabine.security.JwtService;
import com.example.policlicabine.util.CredentialGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordResetTokenService resetTokenService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final ApplicationEventPublisher eventPublisher;
    private final PatientService patientService;
    private final DoctorService doctorService;
    private final ManagerService managerService;
    private final CredentialGenerator credentialGenerator;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            log.warn("Registration failed: Username already exists: {}", request.getUsername());
            throw new BusinessException("Username already exists");
        }

        if (request.getRoles() == null || request.getRoles().isEmpty()) {
            throw new BusinessException("At least one role is required");
        }

        Set<Role> roles = roleRepository.findByNameIn(request.getRoles());

        if (roles.size() != request.getRoles().size()) {
            throw new BusinessException("One or more roles not found in database");
        }

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .enabled(true)
                .accountNonLocked(true)
                .build();

        roles.forEach(user::addRole);

        User saved = userRepository.save(user);
        log.info("User registered successfully: {}", saved.getUsername());

        eventPublisher.publishEvent(new UserRegistered(
                saved.getUserId(),
                saved.getUsername(),
                request.getRoles()
        ));

        UserDetails userDetails = userDetailsService.loadUserByUsername(saved.getUsername());
        String accessToken = jwtService.generateToken(userDetails, saved.getUserId().toString());
        String refreshToken = jwtService.generateRefreshToken(userDetails);

        Set<UserRole> roleNames = saved.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .userId(saved.getUserId())
                .username(saved.getUsername())
                .roles(roleNames)
                .build();
    }

    public AuthResponse authenticate(LoginRequest request, String ipAddress) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()
                    )
            );

            User user = userRepository.findWithRolesAndPermissionsByUsername(request.getUsername())
                    .orElseThrow(() -> new BusinessException("Invalid credentials"));

            user.setLastLogin(OffsetDateTime.now(ZoneOffset.UTC));
            userRepository.save(user);

            UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
            String accessToken = jwtService.generateToken(userDetails, user.getUserId().toString());
            String refreshToken = jwtService.generateRefreshToken(userDetails);

            log.info("User authenticated successfully: {}", user.getUsername());

            eventPublisher.publishEvent(new UserAuthenticated(
                    user.getUserId(),
                    user.getUsername(),
                    OffsetDateTime.now(ZoneOffset.UTC),
                    ipAddress != null ? ipAddress : "unknown"
            ));

            Set<UserRole> roleNames = user.getRoles().stream()
                    .map(Role::getName)
                    .collect(Collectors.toSet());

            return AuthResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .userId(user.getUserId())
                    .username(user.getUsername())
                    .roles(roleNames)
                    .build();

        } catch (BadCredentialsException e) {
            log.warn("Authentication failed: Invalid credentials for user: {}", request.getUsername());
            throw new BusinessException("Invalid credentials");
        }
    }

    @Transactional(readOnly = true)
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();

        String username = jwtService.extractUsername(refreshToken);

        if (username == null) {
            log.warn("Refresh token invalid: Cannot extract username");
            throw new BusinessException("Invalid refresh token");
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        if (!jwtService.isTokenValid(refreshToken, userDetails)) {
            log.warn("Refresh token invalid or expired for user: {}", username);
            throw new BusinessException("Invalid or expired refresh token");
        }

        User user = userRepository.findWithRolesAndPermissionsByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User", "username: " + username));

        String newAccessToken = jwtService.generateToken(userDetails, user.getUserId().toString());

        log.info("Access token refreshed for user: {}", username);

        Set<UserRole> roleNames = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .userId(user.getUserId())
                .username(user.getUsername())
                .roles(roleNames)
                .build();
    }

    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException("User not authenticated");
        }

        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User", "username: " + username));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            log.warn("Password change failed: Invalid current password for user: {}", username);
            throw new BusinessException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        log.info("Password changed successfully for user: {}", username);

        eventPublisher.publishEvent(new PasswordChanged(
                user.getUserId(),
                user.getUsername(),
                OffsetDateTime.now(ZoneOffset.UTC)
        ));
    }

    @Transactional
    public String initiatePasswordReset(InitiatePasswordResetRequest request) {
        User user = userRepository.findByUsername(request.getUsername()).orElse(null);

        if (user == null) {
            log.warn("Password reset requested for non-existent user: {}", request.getUsername());
            return "If the username exists, a password reset link will be sent";
        }

        PasswordResetToken resetToken = resetTokenService.createResetToken(user.getUserId());

        log.info("Password reset initiated for user: {}", user.getUsername());

        eventPublisher.publishEvent(new PasswordResetInitiated(
                user.getUserId(),
                user.getUsername(),
                resetToken.getToken(),
                resetToken.getExpiryDate()
        ));

        return "Password reset token: " + resetToken.getToken();
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken resetToken = resetTokenService.validateResetToken(request.getToken());

        User user = resetToken.getUser();

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        resetTokenService.markTokenAsUsed(request.getToken());

        log.info("Password reset successfully for user: {}", user.getUsername());

        eventPublisher.publishEvent(new PasswordReset(
                user.getUserId(),
                user.getUsername(),
                OffsetDateTime.now(ZoneOffset.UTC)
        ));
    }

    @Transactional
    public AuthResponseWrapper<PatientDto> registerPatient(RegisterPatientRequest request) {
        String finalUsername = request.getUsername();
        String finalPassword = request.getPassword();
        boolean needsGeneration = (finalUsername == null || finalUsername.trim().isEmpty())
                || (finalPassword == null || finalPassword.trim().isEmpty());

        if (needsGeneration) {
            log.info("Auto-generating credentials for patient: {} {}",
                    request.getFirstName(), request.getLastName());

            finalUsername = credentialGenerator.generateUsername(
                    request.getFirstName(),
                    request.getLastName(),
                    userRepository::existsByUsername
            );

            if (finalUsername == null) {
                throw new BusinessException("Failed to generate unique username after multiple attempts");
            }

            finalPassword = credentialGenerator.generateSecurePasswordLowercase();

            log.info("Generated credentials for patient - username: {}, password: {}",
                    finalUsername, finalPassword);
        }

        if (userRepository.existsByUsername(finalUsername)) {
            log.warn("Patient registration failed: Username already exists: {}", finalUsername);
            throw new BusinessException("Username already exists");
        }

        Role patientRole = roleRepository.findByName(UserRole.PATIENT)
                .orElseThrow(() -> new RuntimeException("PATIENT role not initialized"));

        User user = User.builder()
                .username(finalUsername)
                .password(passwordEncoder.encode(finalPassword))
                .enabled(true)
                .accountNonLocked(true)
                .build();

        user.addRole(patientRole);
        user = userRepository.save(user);

        PatientDto patientDto = patientService.createPatientWithUser(
                user,
                request.getFirstName(),
                request.getLastName(),
                request.getPhone(),
                request.getEmail(),
                request.getAddress(),
                null, null, null, null, null
        );

        AuthResponse authResponse = buildAuthResponse(user);

        log.info("Patient registered successfully: {} with profile ID: {}", user.getUsername(), patientDto.getPatientId());

        return AuthResponseWrapper.<PatientDto>builder()
                .authResponse(authResponse)
                .profile(patientDto)
                .build();
    }

    @Transactional
    public AuthResponseWrapper<DoctorDto> registerDoctor(RegisterDoctorRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            log.warn("Doctor registration failed: Username already exists: {}", request.getUsername());
            throw new BusinessException("Username already exists");
        }

        Role doctorRole = roleRepository.findByName(UserRole.DOCTOR)
                .orElseThrow(() -> new RuntimeException("DOCTOR role not initialized"));

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .enabled(true)
                .accountNonLocked(true)
                .build();

        user.addRole(doctorRole);
        user = userRepository.save(user);

        List<Specialty> specialties = request.getSpecialties().stream()
                .map(Specialty::valueOf)
                .toList();

        DoctorDto doctorDto = doctorService.createDoctorWithUser(
                user,
                request.getFullName(),
                specialties
        );

        AuthResponse authResponse = buildAuthResponse(user);

        log.info("Doctor registered successfully: {} with profile ID: {}", user.getUsername(), doctorDto.getDoctorId());

        return AuthResponseWrapper.<DoctorDto>builder()
                .authResponse(authResponse)
                .profile(doctorDto)
                .build();
    }

    @Transactional
    public AuthResponseWrapper<ManagerDto> registerManager(RegisterManagerRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            log.warn("Manager registration failed: Username already exists: {}", request.getUsername());
            throw new BusinessException("Username already exists");
        }

        Role managerRole = roleRepository.findByName(UserRole.MANAGER)
                .orElseThrow(() -> new RuntimeException("MANAGER role not initialized"));

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .enabled(true)
                .accountNonLocked(true)
                .build();

        user.addRole(managerRole);
        user = userRepository.save(user);

        ManagerDto managerDto = managerService.createManagerWithUser(
                user,
                request.getFullName()
        );

        AuthResponse authResponse = buildAuthResponse(user);

        log.info("Manager registered successfully: {} with profile ID: {}", user.getUsername(), managerDto.getManagerId());

        return AuthResponseWrapper.<ManagerDto>builder()
                .authResponse(authResponse)
                .profile(managerDto)
                .build();
    }

    private AuthResponse buildAuthResponse(User user) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
        String accessToken = jwtService.generateToken(userDetails, user.getUserId().toString());
        String refreshToken = jwtService.generateRefreshToken(userDetails);

        Set<UserRole> roleNames = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .userId(user.getUserId())
                .username(user.getUsername())
                .roles(roleNames)
                .build();
    }
}
