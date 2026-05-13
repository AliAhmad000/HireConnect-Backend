package com.hireconnect.application.client;

import com.hireconnect.application.dto.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Feign client to communicate with profile-service.
 * Used to fetch candidate profile details (name, email, resumeUrl) at submission.
 */
@FeignClient(name = "profile-service", path = "/api/v1/profiles")
public interface ProfileServiceClient {

    @GetMapping("/candidate/{candidateId}/resume-url")
    ApiResponse<String> getCandidateResumeUrl(@PathVariable Long candidateId);
}
