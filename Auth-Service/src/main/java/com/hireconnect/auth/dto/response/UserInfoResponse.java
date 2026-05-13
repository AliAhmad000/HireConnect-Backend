package com.hireconnect.auth.dto.response;

import com.hireconnect.auth.enums.AuthProvider;
import com.hireconnect.auth.enums.UserRole;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class UserInfoResponse {
    private Long userId;
    private String email;
    private String fullName;
    private UserRole role;
    private AuthProvider provider;
    private Boolean isActive;
    private Boolean isEmailVerified;
    private LocalDateTime lastLogin;
    private LocalDateTime createdAt;
}
