package com.hireconnect.auth.dto.response;

import com.hireconnect.auth.enums.AuthProvider;
import com.hireconnect.auth.enums.UserRole;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthResponse {
    private Long userId;
    private String email;
    private String fullName;
    private UserRole role;
    private AuthProvider provider;
    private String accessToken;
    private String refreshToken;
    private long expiresIn;          // seconds
    private String tokenType;
}
