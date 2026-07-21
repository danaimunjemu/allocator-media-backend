package com.allocator.authservice.service;

import com.allocator.authservice.dto.AssignRoleRequest;
import com.allocator.authservice.dto.AuthResponse;
import com.allocator.authservice.dto.ForgotPasswordRequest;
import com.allocator.authservice.dto.LoginRequest;
import com.allocator.authservice.dto.MfaVerifyRequest;
import com.allocator.authservice.dto.PasswordChangeRequest;
import com.allocator.authservice.dto.RegisterRequest;
import com.allocator.authservice.dto.ResendVerificationRequest;
import com.allocator.authservice.dto.ResetPasswordRequest;
import com.allocator.authservice.dto.TokenRefreshRequest;
import com.allocator.authservice.dto.VerifyEmailRequest;
import com.allocator.authservice.exception.WrongPasswordException;
import com.allocator.authservice.model.Brand;
import com.allocator.authservice.model.EmailVerificationToken;
import com.allocator.authservice.model.PasswordResetToken;
import com.allocator.authservice.model.RefreshToken;
import com.allocator.authservice.model.Role;
import com.allocator.authservice.model.RoleName;
import com.allocator.authservice.model.User;
import com.allocator.authservice.model.UserRole;
import com.allocator.authservice.repository.BrandRepository;
import com.allocator.authservice.repository.EmailVerificationTokenRepository;
import com.allocator.authservice.repository.PasswordResetTokenRepository;
import com.allocator.authservice.repository.RefreshTokenRepository;
import com.allocator.authservice.repository.RoleRepository;
import com.allocator.authservice.repository.UserRepository;
import com.allocator.authservice.repository.UserRoleRepository;
import com.allocator.authservice.security.CustomUserDetails;
import com.allocator.authservice.security.CustomUserDetailsService;
import com.allocator.authservice.security.JwtProvider;
import com.allocator.authservice.mapper.UserMapper;
import com.allocator.authservice.dto.UserDto;
import com.allocator.authservice.dto.ProfileUpdateRequest;
import com.allocator.authservice.service.EventPublisherService;
import com.allocator.common.event.EmailVerificationRequestedEvent;
import com.allocator.common.event.PasswordResetRequestedEvent;
import io.micrometer.core.instrument.Counter;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import io.micrometer.observation.annotation.Observed;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

        private final UserRepository userRepository;
        private final RoleRepository roleRepository;
        private final BrandRepository brandRepository;
        private final UserRoleRepository userRoleRepository;
        private final RefreshTokenRepository refreshTokenRepository;
        private final EmailVerificationTokenRepository emailVerificationTokenRepository;
        private final PasswordResetTokenRepository passwordResetTokenRepository;
        private final PasswordEncoder passwordEncoder;
        private final JwtProvider jwtProvider;
        private final UserMapper userMapper;
        private final AuthenticationManager authenticationManager;
        private final CustomUserDetailsService customUserDetailsService;
        private final EventPublisherService eventPublisherService;
        private final ApplicationEventPublisher applicationEventPublisher;
        private final Counter loginSuccessCounter;
        private final Counter loginFailureCounter;
        private final Counter registerCounter;
        private final MfaService mfaService;
        private final SessionService sessionService;
        private final com.allocator.paymentservice.service.SubscriptionService subscriptionService;

        private static final long EMAIL_VERIFICATION_TOKEN_TTL_HOURS = 24;
        private static final long PASSWORD_RESET_TOKEN_TTL_HOURS = 1;

        @org.springframework.beans.factory.annotation.Autowired
        private HttpServletRequest httpServletRequest;

        @Transactional
        @Observed(name = "user-registration")
        public void register(RegisterRequest request) {
                log.info("Starting user registration for email: {}, requested roles: {}", request.getEmail(), request.getRoles());
                
                if (userRepository.existsByEmail(request.getEmail())) {
                        log.warn("Registration attempt with existing email: {}", request.getEmail());
                        throw new IllegalStateException("Email already taken");
                }

                // Self-service (public reader signup) is when no roles/brand-role mappings are
                // explicitly requested — this is the same condition that later falls into the
                // default-to-READER branch below. Admin-driven staff creation always specifies roles.
                boolean isSelfService = (request.getBrandRoleMappings() == null || request.getBrandRoleMappings().isEmpty())
                                && (request.getRoles() == null || request.getRoles().isEmpty());

                User user = User.builder()
                                .username(request.getUsername())
                                .email(request.getEmail())
                                .password(passwordEncoder.encode(request.getPassword()))
                                .firstName(request.getFirstName())
                                .lastName(request.getLastName())
                                .enabled(true)
                                .mustChangePassword(!isSelfService)
                                .emailVerified(!isSelfService)
                                .build();

                userRepository.save(user);

                MDC.put("userId", user.getId().toString());
                log.info("User registered successfully with ID: {}", user.getId());

                // Find brand by code if provided, otherwise default to first brand
                Brand defaultBrand;
                if (request.getBrandCode() != null && !request.getBrandCode().isBlank()) {
                        defaultBrand = brandRepository.findByCode(request.getBrandCode())
                                        .orElse(null);
                } else {
                        defaultBrand = brandRepository.findAll().stream()
                                        .findFirst()
                                        .orElse(null);
                }

                java.util.List<String> assignedRoleNames = new java.util.ArrayList<>();
                UUID primaryBrandId = defaultBrand != null ? defaultBrand.getId() : null;

                if (request.getBrandRoleMappings() != null && !request.getBrandRoleMappings().isEmpty()) {
                        for (RegisterRequest.BrandRoleMapping mapping : request.getBrandRoleMappings()) {
                                Brand brand = brandRepository.findByCode(mapping.getBrandCode())
                                                .orElseThrow(() -> new IllegalStateException("Brand not found for code: " + mapping.getBrandCode()));
                                if (primaryBrandId == null) {
                                        primaryBrandId = brand.getId();
                                }
                                for (String roleStr : mapping.getRoles()) {
                                        RoleName rn = RoleName.READER;
                                        try {
                                                rn = RoleName.valueOf(roleStr.toUpperCase());
                                        } catch (IllegalArgumentException e) {
                                                log.warn("Invalid role name provided: {}, defaulting to READER", roleStr);
                                        }
                                        RoleName finalRn = rn;
                                        Role role = roleRepository.findByName(rn)
                                                        .orElseThrow(() -> new IllegalStateException("Role not found: " + finalRn));
                                        UserRole userRole = UserRole.builder()
                                                        .user(user)
                                                        .role(role)
                                                        .brand(brand)
                                                        .build();
                                        userRoleRepository.save(userRole);
                                        assignedRoleNames.add(rn.toString());
                                }
                        }
                } else {
                        // Fallback to legacy single brandCode + roles list
                        Brand brand = defaultBrand;
                        if (brand == null) {
                                throw new IllegalStateException("No brands found in the system. Please create a brand first.");
                        }
                        if (request.getRoles() != null && !request.getRoles().isEmpty()) {
                                for (String roleStr : request.getRoles()) {
                                        RoleName rn = RoleName.READER;
                                        try {
                                                rn = RoleName.valueOf(roleStr.toUpperCase());
                                        } catch (IllegalArgumentException e) {
                                                log.warn("Invalid role name provided: {}, defaulting to READER", roleStr);
                                        }
                                        RoleName finalRn = rn;
                                        Role role = roleRepository.findByName(rn)
                                                        .orElseThrow(() -> new IllegalStateException("Role not found: " + finalRn));
                                        UserRole userRole = UserRole.builder()
                                                        .user(user)
                                                        .role(role)
                                                        .brand(brand)
                                                        .build();
                                        userRoleRepository.save(userRole);
                                        assignedRoleNames.add(rn.toString());
                                }
                        } else {
                                Role role = roleRepository.findByName(RoleName.READER)
                                                .orElseThrow(() -> new IllegalStateException("Role not found: " + RoleName.READER));
                                UserRole userRole = UserRole.builder()
                                                .user(user)
                                                .role(role)
                                                .brand(brand)
                                                .build();
                                userRoleRepository.save(userRole);
                                assignedRoleNames.add(RoleName.READER.toString());
                        }
                }
                log.info("Assigned roles {} to new user {}", assignedRoleNames, user.getId());

                // Increment registration counter
                registerCounter.increment();

                // Publish UserCreated event with the assigned roles and primary brand ID
                eventPublisherService.publishUserCreatedEvent(user.getId(), user.getEmail(), assignedRoleNames, primaryBrandId);

                if (isSelfService) {
                        issueEmailVerificationToken(user);
                        try {
                                subscriptionService.createFreeSubscription(user.getId(), user.getEmail(), user.getFirstName(), user.getLastName());
                        } catch (Exception e) {
                                log.error("Failed to create free subscription for user {}: {}", user.getId(), e.getMessage());
                        }
                }
        }

        // OAuth2 social login (Google/Facebook/LinkedIn) entry point: find the
        // reader by email, or provision one exactly like self-service register()
        // does (default brand, READER role, free subscription), then mint the
        // same token pair a password login would produce.
        @Transactional
        public AuthResponse loginOrRegisterOAuthUser(String email, String firstName, String lastName, String avatarUrl) {
                User user = userRepository.findByEmail(email)
                                .orElseGet(() -> createOAuthUser(email, firstName, lastName, avatarUrl));

                CustomUserDetails userDetails = (CustomUserDetails) customUserDetailsService.loadUserByUsername(user.getEmail());
                sessionService.recordLoginHistory(user.getId(), user.getEmail(), httpServletRequest, true, null);
                return buildFullAuthResponse(user, userDetails);
        }

        private User createOAuthUser(String email, String firstName, String lastName, String avatarUrl) {
                User user = User.builder()
                                .username(email)
                                .email(email)
                                .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                                .firstName(firstName)
                                .lastName(lastName)
                                .avatarUrl(avatarUrl)
                                .enabled(true)
                                .mustChangePassword(false)
                                .emailVerified(true)
                                .build();
                userRepository.save(user);

                Brand defaultBrand = brandRepository.findAll().stream().findFirst()
                                .orElseThrow(() -> new IllegalStateException("No brands found in the system. Please create a brand first."));
                Role role = roleRepository.findByName(RoleName.READER)
                                .orElseThrow(() -> new IllegalStateException("Role not found: " + RoleName.READER));
                userRoleRepository.save(UserRole.builder().user(user).role(role).brand(defaultBrand).build());

                log.info("Provisioned new reader account {} via OAuth login", user.getId());
                eventPublisherService.publishUserCreatedEvent(user.getId(), user.getEmail(),
                                java.util.List.of(RoleName.READER.toString()), defaultBrand.getId());

                try {
                        subscriptionService.createFreeSubscription(user.getId(), user.getEmail(), user.getFirstName(), user.getLastName());
                } catch (Exception e) {
                        log.error("Failed to create free subscription for OAuth user {}: {}", user.getId(), e.getMessage());
                }

                return user;
        }

        private void issueEmailVerificationToken(User user) {
                EmailVerificationToken verificationToken = EmailVerificationToken.builder()
                                .user(user)
                                .token(UUID.randomUUID().toString())
                                .expiryDate(Instant.now().plus(EMAIL_VERIFICATION_TOKEN_TTL_HOURS, ChronoUnit.HOURS))
                                .used(false)
                                .build();
                emailVerificationTokenRepository.save(verificationToken);

                applicationEventPublisher.publishEvent(EmailVerificationRequestedEvent.builder()
                                .eventId(UUID.randomUUID().toString())
                                .eventType("EmailVerificationRequested")
                                .timestamp(java.time.LocalDateTime.now())
                                .sourceService("auth-service")
                                .userId(user.getId().toString())
                                .email(user.getEmail())
                                .firstName(user.getFirstName())
                                .token(verificationToken.getToken())
                                .build());

                log.info("Issued email verification token for user {}", user.getId());
        }

        @Transactional
        public void verifyEmail(VerifyEmailRequest request) {
                EmailVerificationToken token = emailVerificationTokenRepository.findByToken(request.getToken())
                                .orElseThrow(() -> new IllegalStateException("Invalid or expired verification token"));

                if (token.isUsed() || token.getExpiryDate().isBefore(Instant.now())) {
                        throw new IllegalStateException("Invalid or expired verification token");
                }

                User user = token.getUser();
                user.setEmailVerified(true);
                userRepository.save(user);

                token.setUsed(true);
                emailVerificationTokenRepository.save(token);

                log.info("Email verified for user {}", user.getId());
        }

        @Transactional
        public void resendVerification(ResendVerificationRequest request) {
                userRepository.findByEmail(request.getEmail()).ifPresent(user -> {
                        if (!user.isEmailVerified()) {
                                issueEmailVerificationToken(user);
                        }
                });
                // Always returns silently regardless of outcome to avoid leaking account existence/state.
        }

        @Transactional
        public void forgotPassword(ForgotPasswordRequest request) {
                userRepository.findByEmail(request.getEmail()).ifPresent(user -> {
                        PasswordResetToken resetToken = PasswordResetToken.builder()
                                        .user(user)
                                        .token(UUID.randomUUID().toString())
                                        .expiryDate(Instant.now().plus(PASSWORD_RESET_TOKEN_TTL_HOURS, ChronoUnit.HOURS))
                                        .used(false)
                                        .build();
                        passwordResetTokenRepository.save(resetToken);

                        applicationEventPublisher.publishEvent(PasswordResetRequestedEvent.builder()
                                        .eventId(UUID.randomUUID().toString())
                                        .eventType("PasswordResetRequested")
                                        .timestamp(java.time.LocalDateTime.now())
                                        .sourceService("auth-service")
                                        .userId(user.getId().toString())
                                        .email(user.getEmail())
                                        .firstName(user.getFirstName())
                                        .token(resetToken.getToken())
                                        .build());

                        log.info("Issued password reset token for user {}", user.getId());
                });
                // Always returns silently regardless of whether the email exists, to avoid user enumeration.
        }

        @Transactional
        public void resetPassword(ResetPasswordRequest request) {
                PasswordResetToken token = passwordResetTokenRepository.findByToken(request.getToken())
                                .orElseThrow(() -> new IllegalStateException("Invalid or expired reset token"));

                if (token.isUsed() || token.getExpiryDate().isBefore(Instant.now())) {
                        throw new IllegalStateException("Invalid or expired reset token");
                }

                User user = token.getUser();
                user.setPassword(passwordEncoder.encode(request.getNewPassword()));
                userRepository.save(user);

                token.setUsed(true);
                passwordResetTokenRepository.save(token);

                // Force re-login everywhere by revoking existing sessions.
                refreshTokenRepository.deleteByUser(user);

                log.info("Password reset for user {}", user.getId());
        }

        @Transactional
        @Observed(name = "user-login")
        public AuthResponse login(LoginRequest request) {
                log.info("Login attempt for email: {}", request.getEmail());

                try {
                        Authentication authentication = authenticationManager.authenticate(
                                        new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        loginSuccessCounter.increment();

                        User user = userRepository.findByEmail(request.getEmail())
                                        .orElseThrow(() -> new UsernameNotFoundException("User not found"));

                        MDC.put("userId", user.getId().toString());
                        log.info("Login successful for user ID: {}", user.getId());

                        // Record login history
                        sessionService.recordLoginHistory(user.getId(), user.getEmail(), httpServletRequest, true, null);

                        // Check MFA
                        if (user.isMfaEnabled()) {
                                String preferredMethod = mfaService.getPreferredMethod(user.getId());
                                String mfaPendingToken = jwtProvider.generateMfaPendingToken(user.getEmail());
                                log.info("MFA required for user {}, method: {}", user.getId(), preferredMethod);
                                return AuthResponse.builder()
                                                .mfaRequired(true)
                                                .mfaPendingToken(mfaPendingToken)
                                                .mfaMethod(preferredMethod)
                                                .email(user.getEmail())
                                                .build();
                        }

                        return buildFullAuthResponse(user, authentication);
                } catch (Exception e) {
                        loginFailureCounter.increment();
                        log.error("Login failed for email: {}", request.getEmail(), e);
                        try {
                                sessionService.recordLoginHistory(null, request.getEmail(), httpServletRequest, false, e.getMessage());
                        } catch (Exception ignore) {}
                        throw e;
                }
        }

        @Transactional
        public AuthResponse verifyMfa(MfaVerifyRequest request) {
                log.info("MFA verification attempt");

                if (!jwtProvider.isMfaPendingToken(request.getMfaPendingToken())) {
                        throw new IllegalArgumentException("Invalid or expired MFA token");
                }

                String email = jwtProvider.getEmailFromToken(request.getMfaPendingToken());
                User user = userRepository.findByEmail(email)
                                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

                boolean valid = false;
                String method = request.getMethod() != null ? request.getMethod().toUpperCase() : "TOTP";

                switch (method) {
                        case "TOTP" -> valid = mfaService.verifyTotpCode(user.getId(), request.getCode());
                        case "BACKUP_CODE" -> valid = mfaService.verifyBackupCode(user.getId(), request.getCode());
                        default -> throw new IllegalArgumentException("Unsupported MFA method: " + method);
                }

                if (!valid) {
                        sessionService.recordLoginHistory(user.getId(), user.getEmail(), httpServletRequest, false, "Invalid MFA code");
                        throw new IllegalArgumentException("Invalid MFA code");
                }

                if (request.isTrustDevice() && request.getDeviceFingerprint() != null) {
                        UserAgentShim ua = parseUa(httpServletRequest.getHeader("User-Agent"));
                        sessionService.trustDevice(user.getId(), request.getDeviceFingerprint(),
                                request.getDeviceName() != null ? request.getDeviceName() : ua.deviceName(),
                                ua.deviceType(), ua.os());
                }

                CustomUserDetails userDetails = (CustomUserDetails) customUserDetailsService.loadUserByUsername(email);
                sessionService.recordLoginHistory(user.getId(), user.getEmail(), httpServletRequest, true, null);
                return buildFullAuthResponse(user, userDetails);
        }

        private AuthResponse buildFullAuthResponse(User user, Object authPrincipal) {
                String jwt, refresh;
                if (authPrincipal instanceof Authentication auth) {
                        jwt = jwtProvider.generateAccessToken(auth);
                        refresh = jwtProvider.generateRefreshToken(auth);
                } else {
                        CustomUserDetails ud = (CustomUserDetails) authPrincipal;
                        jwt = jwtProvider.generateAccessToken(ud);
                        refresh = jwtProvider.generateRefreshToken(ud);
                }

                refreshTokenRepository.deleteByUser(user);
                RefreshToken refreshToken = RefreshToken.builder()
                                .user(user)
                                .token(refresh)
                                .expiryDate(Instant.now().plus(7, ChronoUnit.DAYS))
                                .revoked(false)
                                .build();
                refreshTokenRepository.save(refreshToken);

                sessionService.createSession(user.getId(), httpServletRequest, jwt);

                java.util.List<UserRole> userRoles = userRoleRepository.findByUser(user);
                String primaryRole = userRoles.isEmpty()
                        ? RoleName.READER.toString()
                        : userRoles.get(0).getRole().getName().toString();
                java.util.List<String> allRoles = userRoles.stream()
                        .map(ur -> ur.getRole().getName().toString())
                        .distinct()
                        .collect(java.util.stream.Collectors.toList());

                return AuthResponse.builder()
                                .accessToken(jwt)
                                .refreshToken(refresh)
                                .userId(user.getId())
                                .email(user.getEmail())
                                .firstName(user.getFirstName())
                                .lastName(user.getLastName())
                                .avatarUrl(user.getAvatarUrl())
                                .role(primaryRole)
                                .roles(allRoles)
                                .mustChangePassword(user.isMustChangePassword())
                                .emailVerified(user.isEmailVerified())
                                .build();
        }

        private record UserAgentShim(String deviceName, String deviceType, String os) {}
        private UserAgentShim parseUa(String ua) {
                if (ua == null) return new UserAgentShim("Unknown", "DESKTOP", "Unknown");
                String type = ua.contains("iPhone") ? "MOBILE" : ua.contains("iPad") ? "TABLET" : "DESKTOP";
                String name = ua.contains("iPhone") ? "iPhone" : ua.contains("iPad") ? "iPad"
                        : ua.contains("Macintosh") ? "MacBook" : ua.contains("Windows") ? "Windows PC" : "Desktop";
                String os = ua.contains("iPhone") || ua.contains("iPad") ? "iOS"
                        : ua.contains("Android") ? "Android" : ua.contains("Macintosh") ? "macOS"
                        : ua.contains("Windows") ? "Windows" : "Linux";
                return new UserAgentShim(name, type, os);
        }

        @Transactional
        public AuthResponse refreshToken(TokenRefreshRequest request) {
                RefreshToken refreshToken = refreshTokenRepository.findByToken(request.getRefreshToken())
                                .orElseThrow(() -> new IllegalStateException("Refresh token not found"));

                if (refreshToken.isRevoked() || refreshToken.getExpiryDate().isBefore(Instant.now())) {
                        throw new IllegalStateException("Refresh token is invalid or expired");
                }

                User user = refreshToken.getUser();
                CustomUserDetails userDetails = (CustomUserDetails) customUserDetailsService
                                .loadUserByUsername(user.getEmail());

                String newAccessToken = jwtProvider.generateAccessToken(userDetails);
                String newRefreshToken = jwtProvider.generateRefreshToken(userDetails);

                // Rotate refresh token
                refreshTokenRepository.delete(refreshToken);
                RefreshToken nextRefreshToken = RefreshToken.builder()
                                .user(user)
                                .token(newRefreshToken)
                                .expiryDate(Instant.now().plus(7, ChronoUnit.DAYS))
                                .revoked(false)
                                .build();
                refreshTokenRepository.save(nextRefreshToken);

                java.util.List<UserRole> refreshUserRoles = userRoleRepository.findByUser(user);
                String refreshPrimaryRole = refreshUserRoles.isEmpty()
                        ? RoleName.READER.toString()
                        : refreshUserRoles.get(0).getRole().getName().toString();
                java.util.List<String> refreshAllRoles = refreshUserRoles.stream()
                        .map(ur -> ur.getRole().getName().toString())
                        .distinct()
                        .collect(java.util.stream.Collectors.toList());

                return AuthResponse.builder()
                                .accessToken(newAccessToken)
                                .refreshToken(newRefreshToken)
                                .userId(user.getId())
                                .email(user.getEmail())
                                .firstName(user.getFirstName())
                                .lastName(user.getLastName())
                                .avatarUrl(user.getAvatarUrl())
                                .role(refreshPrimaryRole)
                                .roles(refreshAllRoles)
                                .mustChangePassword(user.isMustChangePassword())
                                .emailVerified(user.isEmailVerified())
                                .build();
        }

        @Transactional
        public AuthResponse changePassword(String email, PasswordChangeRequest request) {
                log.info("Password change requested for email: {}", email);

                User user = userRepository.findByEmail(email)
                                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

                if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
                        log.warn("Password change failed: current password mismatch for email: {}", email);
                        throw new WrongPasswordException();
                }

                user.setPassword(passwordEncoder.encode(request.getNewPassword()));
                user.setMustChangePassword(false);
                userRepository.save(user);

                log.info("Password changed successfully for user ID: {}", user.getId());

                // Issue a fresh token so the caller can replace the old one immediately
                java.util.List<UserRole> userRoles = userRoleRepository.findByUser(user);
                java.util.Set<String> roles = userRoles.stream()
                                .map(ur -> "ROLE_" + ur.getRole().getName().name())
                                .collect(java.util.stream.Collectors.toSet());
                java.util.Set<java.util.UUID> brandIds = userRoles.stream()
                                .map(ur -> ur.getBrand().getId())
                                .collect(java.util.stream.Collectors.toSet());
                CustomUserDetails ud = new CustomUserDetails(user, roles, brandIds);

                String newAccess  = jwtProvider.generateAccessToken(ud);
                String newRefresh = jwtProvider.generateRefreshToken(ud);

                refreshTokenRepository.deleteByUser(user);
                RefreshToken refreshToken = RefreshToken.builder()
                                .user(user)
                                .token(newRefresh)
                                .expiryDate(java.time.Instant.now().plus(7, java.time.temporal.ChronoUnit.DAYS))
                                .revoked(false)
                                .build();
                refreshTokenRepository.save(refreshToken);

                java.util.List<String> allRoles = userRoles.stream()
                                .map(ur -> ur.getRole().getName().toString())
                                .distinct()
                                .collect(java.util.stream.Collectors.toList());
                String primaryRole = allRoles.isEmpty() ? RoleName.READER.toString() : allRoles.get(0);

                return AuthResponse.builder()
                                .accessToken(newAccess)
                                .refreshToken(newRefresh)
                                .userId(user.getId())
                                .email(user.getEmail())
                                .firstName(user.getFirstName())
                                .lastName(user.getLastName())
                                .avatarUrl(user.getAvatarUrl())
                                .role(primaryRole)
                                .roles(allRoles)
                                .mustChangePassword(false)
                                .emailVerified(user.isEmailVerified())
                                .build();
        }

        @Transactional
        @Observed(name = "role-assignment")
        public void assignRole(AssignRoleRequest request) {
                log.info("Assigning role {} to user {} for brand {}", 
                        request.getRoleName(), request.getEmail(), request.getBrandCode());
                
                User user = userRepository.findByEmail(request.getEmail())
                                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

                Role role = roleRepository.findByName(request.getRoleName())
                                .orElseThrow(() -> new IllegalStateException("Role not found"));

                Brand brand = brandRepository.findByCode(request.getBrandCode())
                                .orElseThrow(() -> new IllegalStateException("Brand not found"));

                UserRole userRole = UserRole.builder()
                                .user(user)
                                .role(role)
                                .brand(brand)
                                .build();

                userRoleRepository.save(userRole);

                MDC.put("userId", user.getId().toString());
                log.info("Role {} assigned successfully to user {} for brand {}", 
                        request.getRoleName(), user.getId(), brand.getId());

                // Get all user roles for the event
                java.util.List<String> userRoles = userRoleRepository.findByUser(user)
                        .stream()
                        .map(ur -> ur.getRole().getName().toString())
                        .collect(java.util.stream.Collectors.toList());

                // Publish RoleAssigned event
                eventPublisherService.publishRoleAssignedEvent(user.getId(), user.getEmail(), userRoles, brand.getId());
        }

    @Transactional
    public UserDto updateProfileByEmail(String email, ProfileUpdateRequest request) {
        log.info("Updating profile for user email: {}", email);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));

        if (request.getFirstName() != null) user.setFirstName(request.getFirstName());
        if (request.getLastName() != null) user.setLastName(request.getLastName());
        if (request.getAvatarUrl() != null) user.setAvatarUrl(request.getAvatarUrl());
        if (request.getBio() != null) user.setBio(request.getBio());
        if (request.getContactInfo() != null) user.setContactInfo(request.getContactInfo());

        User updatedUser = userRepository.save(user);
        log.info("Profile updated successfully for user ID: {}", updatedUser.getId());
        
        UserDto dto = userMapper.toDto(updatedUser);
        populateRoles(updatedUser, dto);
        
        return dto;
    }

    @Transactional(readOnly = true)
    public UserDto getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
        UserDto dto = userMapper.toDto(user);
        populateRoles(user, dto);
        return dto;
    }

    @Transactional(readOnly = true)
    public UserDto getUserById(java.util.UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "User not found: " + id));
        UserDto dto = userMapper.toDto(user);
        populateRoles(user, dto);
        return dto;
    }

    @Transactional(readOnly = true)
    public java.util.List<UserDto> getUsers() {
                log.info("Fetching all users from repository");
                return userRepository.findAll().stream()
                        .map(user -> {
                                UserDto dto = userMapper.toDto(user);
                                populateRoles(user, dto);
                                return dto;
                        })
                        // Readers are self-service subscribers managed under Subscriptions, not
                        // staff — exclude users whose only role is READER from the Users directory.
                        .filter(dto -> !(dto.getRoles() != null && dto.getRoles().equals(java.util.List.of("READER"))))
                        .collect(java.util.stream.Collectors.toList());
        }

    // Used by @mention autocomplete in comments — any authenticated user can
    // call this (unlike getUsers(), which is the admin directory), so it only
    // ever returns the minimal public-facing fields in MentionUserDto.
    @Transactional(readOnly = true)
    public java.util.List<com.allocator.authservice.dto.MentionUserDto> searchUsersForMention(String query, int limit) {
        if (query == null || query.isBlank()) return java.util.List.of();
        org.springframework.data.domain.Pageable pageable =
                org.springframework.data.domain.PageRequest.of(0, Math.min(Math.max(limit, 1), 20));
        return userRepository
                .findByEnabledTrueAndFirstNameContainingIgnoreCaseOrEnabledTrueAndLastNameContainingIgnoreCase(
                        query, query, pageable)
                .stream()
                .map(user -> com.allocator.authservice.dto.MentionUserDto.builder()
                        .id(user.getId())
                        .name((user.getFirstName() + " " + user.getLastName()).trim())
                        .avatarUrl(user.getAvatarUrl())
                        .build())
                .collect(java.util.stream.Collectors.toList());
    }

    private void populateRoles(User user, UserDto dto) {
        java.util.List<UserRole> userRoles = userRoleRepository.findByUser(user);
        if (userRoles != null && !userRoles.isEmpty()) {
            dto.setRole(userRoles.get(0).getRole().getName().toString());
            dto.setRoles(userRoles.stream()
                .map(ur -> ur.getRole().getName().toString())
                .distinct()
                .collect(java.util.stream.Collectors.toList()));

            java.util.Map<Brand, java.util.List<String>> brandToRolesMap = new java.util.HashMap<>();
            for (UserRole ur : userRoles) {
                brandToRolesMap.computeIfAbsent(ur.getBrand(), k -> new java.util.ArrayList<>())
                    .add(ur.getRole().getName().toString());
            }

            java.util.List<UserDto.UserBrandRoleDto> mappings = new java.util.ArrayList<>();
            for (java.util.Map.Entry<Brand, java.util.List<String>> entry : brandToRolesMap.entrySet()) {
                Brand b = entry.getKey();
                UserDto.UserBrandRoleDto mappingDto = new UserDto.UserBrandRoleDto();
                mappingDto.setBrandCode(b.getCode());
                mappingDto.setBrandName(b.getName());
                mappingDto.setRoles(entry.getValue());
                mappings.add(mappingDto);
            }
            dto.setBrandRoleMappings(mappings);
        } else {
            dto.setRole(RoleName.READER.toString());
            dto.setRoles(java.util.Collections.singletonList(RoleName.READER.toString()));
            dto.setBrandRoleMappings(java.util.Collections.emptyList());
        }
    }
}
