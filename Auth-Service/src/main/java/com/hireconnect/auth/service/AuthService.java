package com.hireconnect.auth.service;

import com.hireconnect.auth.dto.request.ChangePasswordRequest;
import com.hireconnect.auth.dto.request.ForgotPasswordRequest;
import com.hireconnect.auth.dto.request.LoginRequest;
import com.hireconnect.auth.dto.request.RefreshTokenRequest;
import com.hireconnect.auth.dto.request.RegisterRequest;
import com.hireconnect.auth.dto.request.ResetPasswordRequest;
import com.hireconnect.auth.dto.response.AuthResponse;
import com.hireconnect.auth.dto.response.ForgotPasswordResponse;
import com.hireconnect.auth.dto.response.UserInfoResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    AuthResponse refreshToken(RefreshTokenRequest request);
    void logout(Long userId);
    boolean validateToken(String token);
    UserInfoResponse getUserInfo(Long userId);
    Page<UserInfoResponse> getUsers(Pageable pageable, String role, Boolean active, String search);
    UserInfoResponse updateUserActiveStatus(Long userId, boolean active);
    void deleteUser(Long userId);
    ForgotPasswordResponse forgotPassword(ForgotPasswordRequest request);
    void resetPassword(ResetPasswordRequest request);
    void changePassword(Long userId, ChangePasswordRequest request);
    List<Long> getActiveAdminIds();
    AuthResponse handleOAuthLogin(String email, String fullName, String providerId,
                                  com.hireconnect.auth.enums.AuthProvider provider,
                                  com.hireconnect.auth.enums.UserRole role);
}
