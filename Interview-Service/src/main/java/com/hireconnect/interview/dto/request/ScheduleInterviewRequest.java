package com.hireconnect.interview.dto.request;

import com.hireconnect.interview.enums.InterviewMode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ScheduleInterviewRequest {

    @NotNull(message = "Application ID is required")
    @Positive(message = "Application ID must be positive")
    @Schema(example = "501")
    private Long applicationId;

    @NotNull(message = "Candidate ID is required")
    @Schema(example = "21")
    private Long candidateId;

    @NotNull(message = "Job ID is required")
    @Schema(example = "101")
    private Long jobId;

    @Schema(example = "Senior Java Developer")
    private String jobTitle;
    @Schema(example = "Aarav Sharma")
    private String candidateName;
    @Schema(example = "aarav.sharma@example.com")
    private String candidateEmail;
    @Schema(example = "TechWave Solutions")
    private String companyName;

    @NotNull(message = "Scheduled date/time is required")
    @Future(message = "Interview must be scheduled in the future")
    @Schema(example = "2026-05-10T10:30:00")
    private LocalDateTime scheduledAt;

    @NotNull(message = "Interview mode is required")
    @Schema(example = "ONLINE")
    private InterviewMode mode;

    @Schema(example = "60")
    private Integer durationMinutes = 60;
    @Schema(example = "https://meet.google.com/abc-defg-hij")
    private String meetLink;
    @Schema(example = "TechWave Bengaluru Office")
    private String location;
    @Schema(example = "Technical round focused on Java, Spring Boot, and system design.")
    private String notes;
    @Schema(example = "Priya Mehta")
    private String interviewerName;
    @Schema(example = "1")
    private Integer roundNumber = 1;
}
