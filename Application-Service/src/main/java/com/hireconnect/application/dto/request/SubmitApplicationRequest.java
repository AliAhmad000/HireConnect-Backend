package com.hireconnect.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class SubmitApplicationRequest {

    @NotNull(message = "Job ID is required")
    @Positive(message = "Job ID must be positive")
    @Schema(example = "101")
    private Long jobId;

    @Positive(message = "Recruiter ID must be positive")
    @Schema(example = "12")
    private Long recruiterId;

    @Schema(example = "Senior Java Developer")
    private String jobTitle;

    @Schema(example = "TechWave Solutions")
    private String companyName;

    @Schema(example = "I have 5 years of Java and Spring Boot experience and would be excited to join this team.")
    private String coverLetter;

    @NotBlank(message = "Resume URL is required")
    @Schema(example = "/uploads/resumes/aarav-resume.pdf")
    private String resumeUrl;
}
