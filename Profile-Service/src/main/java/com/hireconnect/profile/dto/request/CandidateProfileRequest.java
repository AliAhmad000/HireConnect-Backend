package com.hireconnect.profile.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class CandidateProfileRequest {
    @NotBlank(message = "Full name is required")
    @Schema(example = "Aarav Sharma")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email")
    @Schema(example = "aarav.sharma@example.com")
    private String email;

    @Pattern(regexp = "^$|^\\+?[0-9\\s-]{7,15}$", message = "Invalid mobile number")
    @Schema(example = "+91 9876543210")
    private String mobile;

    @Schema(example = "1998-08-15")
    private LocalDate dob;
    @Schema(example = "MALE")
    private String gender;
    @Schema(example = "[\"Java\", \"Spring Boot\", \"Angular\"]")
    private List<String> skills;

    @Min(value = 0, message = "Experience cannot be negative")
    @Schema(example = "3")
    private Integer experience;

    @Schema(example = "/uploads/resumes/aarav-resume.pdf")
    private String resumeUrl;
    @Schema(example = "https://www.linkedin.com/in/aarav-sharma")
    private String linkedinUrl;
    @Schema(example = "https://github.com/aaravsharma")
    private String githubUrl;
    @Schema(example = "https://aaravsharma.dev")
    private String portfolioUrl;
    @Schema(example = "Full-stack developer with experience building Java and Angular applications.")
    private String summary;
    @Schema(example = "Infosys")
    private String currentCompany;
    @Schema(example = "Software Engineer")
    private String currentDesignation;
    @Schema(example = "1200000")
    private Double expectedSalary;
    @Schema(example = "30")
    private Integer noticePeriodDays;
    @Schema(example = "true")
    private Boolean isOpenToRemote;
    @Schema(example = "[\"Bengaluru\", \"Pune\", \"Remote\"]")
    private List<String> preferredLocations;
    private List<AddressRequest> addresses;
}
