package com.hireconnect.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RefreshTokenRequest {
    @NotBlank(message = "Refresh token is required")
    @Schema(example = "eyJhbGciOiJIUzI1NiJ9.refresh-token")
    private String refreshToken;
}
