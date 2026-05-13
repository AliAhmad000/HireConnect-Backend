package com.hireconnect.interview.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RescheduleInterviewRequest {

    @NotNull(message = "New scheduled date/time is required")
    @Future(message = "New interview time must be in the future")
    @Schema(example = "2026-05-12T14:00:00")
    private LocalDateTime newScheduledAt;

    @Schema(example = "Candidate requested a later slot due to a scheduling conflict.")
    private String rescheduleReason;
    @Schema(example = "https://meet.google.com/new-slot-link")
    private String meetLink;
    @Schema(example = "TechWave Bengaluru Office")
    private String location;
}
