package com.hireconnect.auth.service.impl;

import com.hireconnect.auth.client.ProfileServiceClient;
import com.hireconnect.auth.config.security.JwtService;
import com.hireconnect.auth.dto.request.CandidateProfileRequest;
import com.hireconnect.auth.dto.request.ChangePasswordRequest;
import com.hireconnect.auth.dto.request.ForgotPasswordRequest;
import com.hireconnect.auth.dto.request.LoginRequest;
import com.hireconnect.auth.dto.request.RefreshTokenRequest;
import com.hireconnect.auth.dto.request.RegisterRequest;
import com.hireconnect.auth.dto.request.RecruiterProfileRequest;
import com.hireconnect.auth.dto.request.ResetPasswordRequest;
import com.hireconnect.auth.dto.response.AuthResponse;
import com.hireconnect.auth.dto.response.ForgotPasswordResponse;
import com.hireconnect.auth.dto.response.UserInfoResponse;
import com.hireconnect.auth.entity.UserCredential;
import com.hireconnect.auth.enums.AuthProvider;
import com.hireconnect.auth.enums.UserRole;
import com.hireconnect.auth.exception.InvalidCredentialsException;
import com.hireconnect.auth.exception.InvalidTokenException;
import com.hireconnect.auth.exception.ResourceNotFoundException;
import com.hireconnect.auth.exception.UserAlreadyExistsException;
import com.hireconnect.auth.repository.UserCredentialRepository;
import com.hireconnect.auth.service.AuthService;
import com.hireconnect.auth.service.WelcomeEmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AuthServiceImpl implements AuthService {

    private final UserCredentialRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final ProfileServiceClient profileServiceClient;
    private final RabbitTemplate rabbitTemplate;
    private final WelcomeEmailService welcomeEmailService;

    @Value("${app.rabbitmq.exchange}")
    private String exchange;

    @Value("${app.rabbitmq.routing-key.notification}")
    private String notificationRoutingKey;

    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    @Override
    public AuthResponse register(RegisterRequest request) {
        log.info("Registering new user: email={}, role={}", request.getEmail(), request.getRole());

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException(request.getEmail());
        }

        UserCredential user = UserCredential.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .role(request.getRole())
                .provider(AuthProvider.LOCAL)
                .isActive(true)
                .isEmailVerified(false)
                .build();

        UserCredential saved = userRepository.save(user);
        createProfile(saved);
        welcomeEmailService.sendWelcomeEmail(saved);
        publishWelcomeEvent(saved);
        log.info("User registered successfully: userId={}", saved.getUserId());

        return buildAuthResponse(saved);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        log.info("Login attempt for email: {}", request.getEmail());

        UserCredential user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(InvalidCredentialsException::new);

        if (!user.getIsActive()) {
            throw new InvalidCredentialsException("Account is deactivated. Please contact support.");
        }

        if (user.getProvider() != AuthProvider.LOCAL) {
            throw new InvalidCredentialsException(
                    "This account uses " + user.getProvider() + " login. Please use OAuth.");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        // Update last login
        userRepository.updateLastLogin(user.getUserId(), LocalDateTime.now());
        log.info("User logged in successfully: userId={}", user.getUserId());

        return buildAuthResponse(user);
    }

    @Override
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        UserCredential user = userRepository.findByRefreshToken(request.getRefreshToken())
                .orElseThrow(() -> new InvalidTokenException("Invalid or expired refresh token"));

        if (user.getRefreshTokenExpiry() == null ||
                user.getRefreshTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new InvalidTokenException("Refresh token has expired. Please login again.");
        }

        log.info("Refreshing token for userId={}", user.getUserId());
        return buildAuthResponse(user);
    }

    @Override
    public void logout(Long userId) {
        userRepository.updateRefreshToken(userId, null, null);
        log.info("User logged out: userId={}", userId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean validateToken(String token) {
        return jwtService.validateToken(token);
    }

    @Override
    @Transactional(readOnly = true)
    public UserInfoResponse getUserInfo(Long userId) {
        UserCredential user = findUserById(userId);
        return toUserInfoResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserInfoResponse> getUsers(Pageable pageable, String role, Boolean active, String search) {
        UserRole parsedRole = role == null || role.isBlank() || role.equalsIgnoreCase("ALL")
                ? null
                : UserRole.valueOf(role.toUpperCase());
        return userRepository.searchUsers(parsedRole, active, search, pageable)
                .map(this::toUserInfoResponse);
    }

    @Override
    public UserInfoResponse updateUserActiveStatus(Long userId, boolean active) {
        UserCredential user = findUserById(userId);
        user.setIsActive(active);
        return toUserInfoResponse(userRepository.save(user));
    }

    @Override
    public void deleteUser(Long userId) {
        UserCredential user = findUserById(userId);
        userRepository.delete(user);
        log.info("User deleted by admin: userId={}", userId);
    }

    @Override
    public ForgotPasswordResponse forgotPassword(ForgotPasswordRequest request) {
        UserCredential user = userRepository.findByEmail(request.getEmail()).orElse(null);

        if (user == null || user.getProvider() != AuthProvider.LOCAL || !Boolean.TRUE.equals(user.getIsActive())) {
            log.info("Password reset requested for non-resettable account: email={}", request.getEmail());
            return ForgotPasswordResponse.builder().build();
        }

        String token = UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
        user.setPasswordResetToken(token);
        user.setPasswordResetTokenExpiry(LocalDateTime.now().plusMinutes(30));
        userRepository.save(user);
        String resetUrl = frontendUrl.replaceAll("/$", "") + "/auth/reset-password?token=" + token;
        welcomeEmailService.sendPasswordResetEmail(user, token, resetUrl);

        log.info("Password reset token generated for userId={}", user.getUserId());
        return ForgotPasswordResponse.builder().build();
    }

    @Override
    public void resetPassword(ResetPasswordRequest request) {
        UserCredential user = userRepository.findByPasswordResetToken(request.getToken())
                .orElseThrow(() -> new InvalidTokenException("Invalid or expired password reset token"));

        if (user.getPasswordResetTokenExpiry() == null ||
                user.getPasswordResetTokenExpiry().isBefore(LocalDateTime.now())) {
            user.setPasswordResetToken(null);
            user.setPasswordResetTokenExpiry(null);
            userRepository.save(user);
            throw new InvalidTokenException("Invalid or expired password reset token");
        }

        if (user.getProvider() != AuthProvider.LOCAL) {
            throw new IllegalStateException("Password cannot be reset for OAuth accounts.");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setPasswordResetToken(null);
        user.setPasswordResetTokenExpiry(null);
        user.setRefreshToken(null);
        user.setRefreshTokenExpiry(null);
        userRepository.save(user);
        log.info("Password reset completed for userId={}", user.getUserId());
    }

    private UserInfoResponse toUserInfoResponse(UserCredential user) {
        return UserInfoResponse.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .provider(user.getProvider())
                .isActive(user.getIsActive())
                .isEmailVerified(user.getIsEmailVerified())
                .lastLogin(user.getLastLogin())
                .createdAt(user.getCreatedAt())
                .build();
    }

    @Override
    public void changePassword(Long userId, ChangePasswordRequest request) {
        UserCredential user = findUserById(userId);

        if (user.getProvider() != AuthProvider.LOCAL) {
            throw new IllegalStateException("Password cannot be changed for OAuth accounts.");
        }

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Current password is incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        log.info("Password changed for userId={}", userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> getActiveAdminIds() {
        return userRepository.findByRoleAndIsActiveTrue(UserRole.ADMIN)
                .stream()
                .map(UserCredential::getUserId)
                .toList();
    }

    @Override
    public AuthResponse handleOAuthLogin(String email, String fullName, String providerId,
                                          AuthProvider provider, UserRole role) {
        UserCredential user = userRepository.findByProviderIdAndProvider(providerId, provider)
                .orElseGet(() -> {
                    // Check if email exists (same person, different provider)
                    if (userRepository.existsByEmail(email)) {
                        throw new UserAlreadyExistsException(
                                "Email already registered with a different login method: " + email);
                    }
                    UserCredential newUser = UserCredential.builder()
                            .email(email)
                            .fullName(fullName)
                            .role(role != null ? role : UserRole.CANDIDATE)
                            .provider(provider)
                            .providerId(providerId)
                            .isActive(true)
                            .isEmailVerified(true)  // OAuth emails are pre-verified
                            .build();
                    UserCredential savedUser = userRepository.save(newUser);
                    createProfile(savedUser);
                    welcomeEmailService.sendWelcomeEmail(savedUser);
                    publishWelcomeEvent(savedUser);
                    return savedUser;
                });

        userRepository.updateLastLogin(user.getUserId(), LocalDateTime.now());
        return buildAuthResponse(user);
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    private AuthResponse buildAuthResponse(UserCredential user) {
        String accessToken  = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        // Persist refresh token
        userRepository.updateRefreshToken(
                user.getUserId(),
                refreshToken,
                LocalDateTime.now().plusSeconds(jwtService.getRefreshExpirationMs() / 1000)
        );

        return AuthResponse.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .provider(user.getProvider())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtService.getExpirationMs() / 1000)
                .tokenType("Bearer")
                .build();
    }

    private void createProfile(UserCredential user) {
        String authorization = "Bearer " + jwtService.generateAccessToken(user);

        if (user.getRole() == UserRole.CANDIDATE) {
            CandidateProfileRequest profileRequest = CandidateProfileRequest.builder()
                    .fullName(user.getFullName())
                    .email(user.getEmail())
                    .build();
            profileServiceClient.createCandidateProfile(profileRequest, user.getUserId(), authorization);
            log.info("Candidate profile created for userId={}", user.getUserId());
            return;
        }

        if (user.getRole() == UserRole.RECRUITER) {
            RecruiterProfileRequest profileRequest = RecruiterProfileRequest.builder()
                    .fullName(user.getFullName())
                    .email(user.getEmail())
                    .companyName("Not Provided")
                    .build();
            profileServiceClient.createRecruiterProfile(profileRequest, user.getUserId(), authorization);
            log.info("Recruiter profile created for userId={}", user.getUserId());
            return;
        }

        log.info("Skipping profile creation for userId={} with role={}", user.getUserId(), user.getRole());
    }

    private void publishWelcomeEvent(UserCredential user) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("eventType", "USER_REGISTERED");
            event.put("userId", user.getUserId());
            event.put("userEmail", user.getEmail());
            event.put("fullName", user.getFullName());
            event.put("role", user.getRole() != null ? user.getRole().name() : "");
            event.put("provider", user.getProvider() != null ? user.getProvider().name() : "");

            rabbitTemplate.convertAndSend(exchange, notificationRoutingKey, event);
            log.debug("Published welcome email event for userId={}", user.getUserId());
        } catch (Exception e) {
            log.warn("Unable to publish welcome email event for userId={}: {}",
                    user.getUserId(), e.getMessage());
        }
    }

    private UserCredential findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
    }
}
