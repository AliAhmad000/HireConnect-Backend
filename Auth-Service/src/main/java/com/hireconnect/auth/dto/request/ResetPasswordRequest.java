package com.hireconnect.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class ResetPasswordRequest {

    @NotBlank(message = "Reset token is required")
    @Schema(example = "eyJhbGciOiJIUzI1NiJ9.reset-token")
    private String token;

    @NotBlank(message = "New password is required")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,}$",
            message = "New password must be at least 8 characters and include uppercase, lowercase, number, and special symbol")
    @Schema(example = "NewStrong@123")
    private String newPassword;
}
