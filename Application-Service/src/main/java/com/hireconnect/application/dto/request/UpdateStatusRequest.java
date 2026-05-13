package com.hireconnect.application.dto.request;

import com.hireconnect.application.enums.ApplicationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateStatusRequest {

    @NotNull(message = "Status is required")
    @Schema(example = "SHORTLISTED")
    private ApplicationStatus status;

    @Schema(example = "Candidate does not meet the required cloud deployment experience.")
    private String rejectionReason;
}
