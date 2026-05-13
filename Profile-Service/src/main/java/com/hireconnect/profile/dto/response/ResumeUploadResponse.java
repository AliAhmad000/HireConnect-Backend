package com.hireconnect.profile.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ResumeUploadResponse {
    private String resumeUrl;
}
