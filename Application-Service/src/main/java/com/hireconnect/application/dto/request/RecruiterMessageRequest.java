package com.hireconnect.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RecruiterMessageRequest {

    @NotBlank(message = "Message is required")
    @Size(max = 1000, message = "Message must be 1000 characters or fewer")
    @Schema(example = "Congratulations, you have been shortlisted. Please share your availability for the next interview round.")
    private String message;
}
