package com.hireconnect.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Schema(example = "recruiter@hireconnect.com")
    private String email;

    @NotBlank(message = "Password is required")
    @Schema(example = "Strong@123")
    private String password;
}
