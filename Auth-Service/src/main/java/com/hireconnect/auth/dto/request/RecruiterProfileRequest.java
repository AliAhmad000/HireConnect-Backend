package com.hireconnect.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RecruiterProfileRequest {
    @Schema(example = "Priya Mehta")
    private String fullName;
    @Schema(example = "priya.mehta@techwave.in")
    private String email;
    @Schema(example = "TechWave Solutions")
    private String companyName;
}
