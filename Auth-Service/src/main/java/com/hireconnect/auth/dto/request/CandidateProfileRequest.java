package com.hireconnect.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CandidateProfileRequest {
    @Schema(example = "Aarav Sharma")
    private String fullName;
    @Schema(example = "aarav.sharma@example.com")
    private String email;
}
