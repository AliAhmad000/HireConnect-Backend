package com.hireconnect.profile.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RecruiterProfileRequest {
    @NotBlank(message = "Full name is required")
    @Schema(example = "Priya Mehta")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email")
    @Schema(example = "priya.mehta@techwave.in")
    private String email;

    @Schema(example = "+91 9988776655")
    private String mobile;

    @NotBlank(message = "Company name is required")
    @Schema(example = "TechWave Solutions")
    private String companyName;

    @Schema(example = "51-200")
    private String companySize;
    @Schema(example = "Information Technology")
    private String industry;
    @Schema(example = "https://techwave.in")
    private String website;
    @Schema(example = "A product engineering company hiring Java and cloud talent.")
    private String companyDescription;
    @Schema(example = "https://www.linkedin.com/company/techwave-solutions")
    private String linkedinUrl;
    @Schema(example = "https://techwave.in/logo.png")
    private String logoUrl;
    @Schema(example = "Talent Acquisition Manager")
    private String designation;
}
