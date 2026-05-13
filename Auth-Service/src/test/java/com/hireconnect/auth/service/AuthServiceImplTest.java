package com.hireconnect.auth.service;

import com.hireconnect.auth.client.ProfileServiceClient;
import com.hireconnect.auth.config.security.JwtService;
import com.hireconnect.auth.dto.request.ForgotPasswordRequest;
import com.hireconnect.auth.dto.request.ChangePasswordRequest;
import com.hireconnect.auth.dto.request.LoginRequest;
import com.hireconnect.auth.dto.request.RefreshTokenRequest;
import com.hireconnect.auth.dto.request.RegisterRequest;
import com.hireconnect.auth.dto.request.ResetPasswordRequest;
import com.hireconnect.auth.dto.response.AuthResponse;
import com.hireconnect.auth.dto.response.ForgotPasswordResponse;
import com.hireconnect.auth.dto.response.UserInfoResponse;
import com.hireconnect.auth.entity.UserCredential;
import com.hireconnect.auth.enums.AuthProvider;
import com.hireconnect.auth.enums.UserRole;
import com.hireconnect.auth.exception.InvalidCredentialsException;
import com.hireconnect.auth.exception.InvalidTokenException;
import com.hireconnect.auth.exception.UserAlreadyExistsException;
import com.hireconnect.auth.repository.UserCredentialRepository;
import com.hireconnect.auth.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthServiceImpl Tests")
class AuthServiceImplTest {

    @Mock private UserCredentialRepository userRepository;
    @Mock private JwtService jwtService;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private ProfileServiceClient profileServiceClient;
    @Mock private RabbitTemplate rabbitTemplate;
    @Mock private WelcomeEmailService welcomeEmailService;

    @InjectMocks private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "exchange", "hireconnect.exchange");
        ReflectionTestUtils.setField(authService, "notificationRoutingKey", "notification.routing.key");
        ReflectionTestUtils.setField(authService, "frontendUrl", "http://localhost:4200");
    }

    private UserCredential buildUser(Long id, String email, UserRole role) {
        return UserCredential.builder()
                .userId(id)
                .email(email)
                .passwordHash("$2a$12$hashedpassword")
                .fullName("Test User")
                .role(role)
                .provider(AuthProvider.LOCAL)
                .isActive(true)
                .isEmailVerified(false)
                .refreshTokenExpiry(LocalDateTime.now().plusDays(7))
                .build();
    }

    private void stubTokenGeneration(UserCredential user) {
        when(jwtService.generateAccessToken(user)).thenReturn("access-token");
        when(jwtService.generateRefreshToken(user)).thenReturn("refresh-token");
        when(jwtService.getExpirationMs()).thenReturn(86400000L);
        when(jwtService.getRefreshExpirationMs()).thenReturn(604800000L);
    }

    @Nested
    @DisplayName("register()")
    class RegisterTests {

        @Test
        @DisplayName("should register new candidate successfully")
        void shouldRegisterCandidate() {
            RegisterRequest req = new RegisterRequest();
            req.setEmail("alice@example.com");
            req.setPassword("password123");
            req.setFullName("Alice Smith");
            req.setRole(UserRole.CANDIDATE);

            UserCredential saved = buildUser(1L, "alice@example.com", UserRole.CANDIDATE);

            when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
            when(passwordEncoder.encode("password123")).thenReturn("$2a$12$hashedpassword");
            when(userRepository.save(any(UserCredential.class))).thenReturn(saved);
            when(jwtService.generateAccessToken(any())).thenReturn("access-token");
            when(jwtService.generateRefreshToken(any())).thenReturn("refresh-token");
            when(jwtService.getExpirationMs()).thenReturn(86400000L);
            when(jwtService.getRefreshExpirationMs()).thenReturn(604800000L);

            AuthResponse result = authService.register(req);

            assertThat(result.getEmail()).isEqualTo("alice@example.com");
            assertThat(result.getRole()).isEqualTo(UserRole.CANDIDATE);
            assertThat(result.getAccessToken()).isEqualTo("access-token");
            assertThat(result.getTokenType()).isEqualTo("Bearer");
            verify(userRepository).save(any(UserCredential.class));
            verify(profileServiceClient).createCandidateProfile(any(), eq(1L), eq("Bearer access-token"));
            verify(welcomeEmailService).sendWelcomeEmail(saved);
            verify(rabbitTemplate).convertAndSend(
                    eq("hireconnect.exchange"),
                    eq("notification.routing.key"),
                    ArgumentMatchers.<Object>argThat(event -> ((Map<?, ?>) event).get("eventType").equals("USER_REGISTERED")
                            && ((Map<?, ?>) event).get("userEmail").equals("alice@example.com"))
            );
        }

        @Test
        @DisplayName("should not fail registration when welcome event publish fails")
        void shouldIgnoreWelcomeEventPublishFailure() {
            RegisterRequest req = new RegisterRequest();
            req.setEmail("alice@example.com");
            req.setPassword("password123");
            req.setFullName("Alice Smith");
            req.setRole(UserRole.CANDIDATE);

            UserCredential saved = buildUser(1L, "alice@example.com", UserRole.CANDIDATE);

            when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
            when(passwordEncoder.encode("password123")).thenReturn("$2a$12$hashedpassword");
            when(userRepository.save(any(UserCredential.class))).thenReturn(saved);
            when(jwtService.generateAccessToken(any())).thenReturn("access-token");
            when(jwtService.generateRefreshToken(any())).thenReturn("refresh-token");
            when(jwtService.getExpirationMs()).thenReturn(86400000L);
            when(jwtService.getRefreshExpirationMs()).thenReturn(604800000L);
            doThrow(new RuntimeException("rabbit down")).when(rabbitTemplate)
                    .convertAndSend(anyString(), anyString(), any(Object.class));

            AuthResponse result = authService.register(req);

            assertThat(result.getEmail()).isEqualTo("alice@example.com");
            verify(welcomeEmailService).sendWelcomeEmail(saved);
        }

        @Test
        @DisplayName("should throw UserAlreadyExistsException for duplicate email")
        void shouldThrowForDuplicateEmail() {
            RegisterRequest req = new RegisterRequest();
            req.setEmail("alice@example.com");
            req.setPassword("password123");
            req.setFullName("Alice Smith");
            req.setRole(UserRole.CANDIDATE);

            when(userRepository.existsByEmail("alice@example.com")).thenReturn(true);

            assertThatThrownBy(() -> authService.register(req))
                    .isInstanceOf(UserAlreadyExistsException.class)
                    .hasMessageContaining("alice@example.com");

            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("login()")
    class LoginTests {

        @Test
        @DisplayName("should login with valid credentials")
        void shouldLoginSuccessfully() {
            LoginRequest req = new LoginRequest();
            req.setEmail("alice@example.com");
            req.setPassword("password123");

            UserCredential user = buildUser(1L, "alice@example.com", UserRole.CANDIDATE);

            when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("password123", user.getPasswordHash())).thenReturn(true);
            when(jwtService.generateAccessToken(user)).thenReturn("access-token");
            when(jwtService.generateRefreshToken(user)).thenReturn("refresh-token");
            when(jwtService.getExpirationMs()).thenReturn(86400000L);
            when(jwtService.getRefreshExpirationMs()).thenReturn(604800000L);

            AuthResponse result = authService.login(req);

            assertThat(result.getAccessToken()).isNotBlank();
            assertThat(result.getUserId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("should throw InvalidCredentialsException for wrong password")
        void shouldThrowForWrongPassword() {
            LoginRequest req = new LoginRequest();
            req.setEmail("alice@example.com");
            req.setPassword("wrongpassword");

            UserCredential user = buildUser(1L, "alice@example.com", UserRole.CANDIDATE);

            when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("wrongpassword", user.getPasswordHash())).thenReturn(false);

            assertThatThrownBy(() -> authService.login(req))
                    .isInstanceOf(InvalidCredentialsException.class);
        }

        @Test
        @DisplayName("should throw for non-existent email")
        void shouldThrowForNonExistentEmail() {
            LoginRequest req = new LoginRequest();
            req.setEmail("nobody@example.com");
            req.setPassword("password123");

            when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.login(req))
                    .isInstanceOf(InvalidCredentialsException.class);
        }

        @Test
        @DisplayName("should throw for deactivated account")
        void shouldThrowForDeactivatedAccount() {
            LoginRequest req = new LoginRequest();
            req.setEmail("alice@example.com");
            req.setPassword("password123");

            UserCredential user = buildUser(1L, "alice@example.com", UserRole.CANDIDATE);
            user.setIsActive(false);

            when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> authService.login(req))
                    .isInstanceOf(InvalidCredentialsException.class)
                    .hasMessageContaining("deactivated");
        }
    }

    @Nested
    @DisplayName("logout()")
    class LogoutTests {

        @Test
        @DisplayName("should clear refresh token on logout")
        void shouldClearRefreshToken() {
            assertThatCode(() -> authService.logout(1L)).doesNotThrowAnyException();
            verify(userRepository).updateRefreshToken(eq(1L), isNull(), isNull());
        }
    }

    @Nested
    @DisplayName("account management")
    class AccountManagementTests {

        @Test
        @DisplayName("refreshToken() should issue new tokens for a valid refresh token")
        void shouldRefreshValidToken() {
            RefreshTokenRequest req = new RefreshTokenRequest();
            req.setRefreshToken("old-refresh");
            UserCredential user = buildUser(1L, "alice@example.com", UserRole.CANDIDATE);

            when(userRepository.findByRefreshToken("old-refresh")).thenReturn(Optional.of(user));
            stubTokenGeneration(user);

            AuthResponse response = authService.refreshToken(req);

            assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
            verify(userRepository).updateRefreshToken(eq(1L), eq("refresh-token"), any(LocalDateTime.class));
        }

        @Test
        @DisplayName("refreshToken() should reject expired refresh tokens")
        void shouldRejectExpiredRefreshToken() {
            RefreshTokenRequest req = new RefreshTokenRequest();
            req.setRefreshToken("expired-refresh");
            UserCredential user = buildUser(1L, "alice@example.com", UserRole.CANDIDATE);
            user.setRefreshTokenExpiry(LocalDateTime.now().minusMinutes(1));

            when(userRepository.findByRefreshToken("expired-refresh")).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> authService.refreshToken(req))
                    .isInstanceOf(InvalidTokenException.class);
        }

        @Test
        @DisplayName("getUserInfo() should map user details")
        void shouldGetUserInfo() {
            UserCredential user = buildUser(1L, "alice@example.com", UserRole.CANDIDATE);
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));

            UserInfoResponse response = authService.getUserInfo(1L);

            assertThat(response.getEmail()).isEqualTo("alice@example.com");
            assertThat(response.getRole()).isEqualTo(UserRole.CANDIDATE);
        }

        @Test
        @DisplayName("getUsers() should parse role filters and map pages")
        void shouldSearchUsersWithRoleFilter() {
            UserCredential user = buildUser(1L, "admin@example.com", UserRole.ADMIN);
            when(userRepository.searchUsers(eq(UserRole.ADMIN), eq(true), eq("admin"), any()))
                    .thenReturn(new PageImpl<>(List.of(user)));

            var page = authService.getUsers(PageRequest.of(0, 10), "ADMIN", true, "admin");

            assertThat(page.getContent()).hasSize(1);
            assertThat(page.getContent().get(0).getRole()).isEqualTo(UserRole.ADMIN);
        }

        @Test
        @DisplayName("updateUserActiveStatus() should save active flag changes")
        void shouldUpdateUserActiveStatus() {
            UserCredential user = buildUser(1L, "alice@example.com", UserRole.CANDIDATE);
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(userRepository.save(user)).thenReturn(user);

            UserInfoResponse response = authService.updateUserActiveStatus(1L, false);

            assertThat(response.getIsActive()).isFalse();
        }

        @Test
        @DisplayName("deleteUser() should delete existing user")
        void shouldDeleteUser() {
            UserCredential user = buildUser(1L, "alice@example.com", UserRole.CANDIDATE);
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));

            authService.deleteUser(1L);

            verify(userRepository).delete(user);
        }
    }

    @Nested
    @DisplayName("forgotPassword()")
    class ForgotPasswordTests {

        @Test
        @DisplayName("should generate token, send email, and not expose token in response")
        void shouldSendResetEmailWithoutExposingToken() {
            ForgotPasswordRequest req = new ForgotPasswordRequest();
            req.setEmail("alice@example.com");
            UserCredential user = buildUser(1L, "alice@example.com", UserRole.CANDIDATE);

            when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));

            ForgotPasswordResponse response = authService.forgotPassword(req);

            assertThat(response.getResetToken()).isNull();
            assertThat(user.getPasswordResetToken()).isNotBlank();
            assertThat(user.getPasswordResetTokenExpiry()).isAfter(LocalDateTime.now());
            verify(userRepository).save(user);
            verify(welcomeEmailService).sendPasswordResetEmail(
                    eq(user),
                    eq(user.getPasswordResetToken()),
                    contains("/auth/reset-password?token=")
            );
        }

        @Test
        @DisplayName("should not reveal whether email exists")
        void shouldNotRevealMissingEmail() {
            ForgotPasswordRequest req = new ForgotPasswordRequest();
            req.setEmail("missing@example.com");

            when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

            ForgotPasswordResponse response = authService.forgotPassword(req);

            assertThat(response.getResetToken()).isNull();
            verify(welcomeEmailService, never()).sendPasswordResetEmail(any(), anyString(), anyString());
        }
    }

    @Test
    @DisplayName("validateToken() should return true for valid token")
    void shouldReturnTrueForValidToken() {
        when(jwtService.validateToken("valid-token")).thenReturn(true);
        assertThat(authService.validateToken("valid-token")).isTrue();
    }

    @Test
    @DisplayName("validateToken() should return false for invalid token")
    void shouldReturnFalseForInvalidToken() {
        when(jwtService.validateToken("bad-token")).thenReturn(false);
        assertThat(authService.validateToken("bad-token")).isFalse();
    }

    @Test
    @DisplayName("resetPassword() should update password and clear reset fields")
    void shouldResetPassword() {
        ResetPasswordRequest req = new ResetPasswordRequest();
        req.setToken("reset-token");
        req.setNewPassword("NewStrong@123");
        UserCredential user = buildUser(1L, "alice@example.com", UserRole.CANDIDATE);
        user.setPasswordResetToken("reset-token");
        user.setPasswordResetTokenExpiry(LocalDateTime.now().plusMinutes(10));

        when(userRepository.findByPasswordResetToken("reset-token")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("NewStrong@123")).thenReturn("new-hash");

        authService.resetPassword(req);

        assertThat(user.getPasswordHash()).isEqualTo("new-hash");
        assertThat(user.getPasswordResetToken()).isNull();
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("changePassword() should verify current password before saving")
    void shouldChangePassword() {
        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setCurrentPassword("OldStrong@123");
        req.setNewPassword("NewStrong@123");
        UserCredential user = buildUser(1L, "alice@example.com", UserRole.CANDIDATE);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("OldStrong@123", user.getPasswordHash())).thenReturn(true);
        when(passwordEncoder.encode("NewStrong@123")).thenReturn("new-hash");

        authService.changePassword(1L, req);

        assertThat(user.getPasswordHash()).isEqualTo("new-hash");
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("getActiveAdminIds() should return ids for active admins")
    void shouldReturnActiveAdminIds() {
        when(userRepository.findByRoleAndIsActiveTrue(UserRole.ADMIN))
                .thenReturn(List.of(buildUser(9L, "admin@example.com", UserRole.ADMIN)));

        assertThat(authService.getActiveAdminIds()).containsExactly(9L);
    }

    @Test
    @DisplayName("handleOAuthLogin() should create new OAuth users")
    void shouldCreateOAuthUser() {
        UserCredential saved = buildUser(1L, "oauth@example.com", UserRole.CANDIDATE);
        saved.setProvider(AuthProvider.GITHUB);
        saved.setProviderId("github-123");

        when(userRepository.findByProviderIdAndProvider("github-123", AuthProvider.GITHUB)).thenReturn(Optional.empty());
        when(userRepository.existsByEmail("oauth@example.com")).thenReturn(false);
        when(userRepository.save(any(UserCredential.class))).thenReturn(saved);
        stubTokenGeneration(saved);

        AuthResponse response = authService.handleOAuthLogin(
                "oauth@example.com", "OAuth User", "github-123", AuthProvider.GITHUB, UserRole.CANDIDATE);

        assertThat(response.getProvider()).isEqualTo(AuthProvider.GITHUB);
        verify(profileServiceClient).createCandidateProfile(any(), eq(1L), eq("Bearer access-token"));
        verify(welcomeEmailService).sendWelcomeEmail(saved);
    }
}
