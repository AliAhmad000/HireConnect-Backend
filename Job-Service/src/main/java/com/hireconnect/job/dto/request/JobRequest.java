package com.hireconnect.job.dto.request;

import com.hireconnect.job.enums.JobStatus;
import com.hireconnect.job.enums.JobType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class JobRequest {

    @Schema(example = "Senior Java Developer")
    private String title;

    @Schema(example = "Software Development")
    private String category;

    @Schema(example = "FULL_TIME")
    private JobType jobType;

    @Schema(example = "Bengaluru")
    private String location;

    @PositiveOrZero(message = "Minimum salary must be non-negative")
    @Schema(example = "1200000")
    private Double salaryMin;

    @PositiveOrZero(message = "Maximum salary must be non-negative")
    @Schema(example = "1800000")
    private Double salaryMax;

    @Schema(example = "We are looking for a Senior Java Developer with strong Spring Boot, REST API, SQL, and cloud deployment experience.")
    private String description;

    @Schema(example = "[\"Java\", \"Spring Boot\", \"PostgreSQL\", \"AWS\"]")
    private List<String> skills;

    @Min(value = 0, message = "Experience cannot be negative")
    @Schema(example = "5")
    private Integer experienceRequired;

    @Positive(message = "Vacancies must be at least 1")
    @Schema(example = "3")
    private Integer vacancies;

    @Schema(example = "TechWave Solutions")
    private String companyName;

    @Schema(example = "true")
    private Boolean isRemote;

    @Future(message = "Expiry date must be in the future")
    @Schema(example = "2026-06-30")
    private LocalDate expiresAt;

    @Schema(example = "ACTIVE")
    private JobStatus status;
}
