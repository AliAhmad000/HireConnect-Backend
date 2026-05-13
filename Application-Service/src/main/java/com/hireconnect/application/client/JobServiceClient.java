package com.hireconnect.application.client;

import com.hireconnect.application.dto.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Feign client to communicate with job-service.
 * Used to validate a job exists before accepting an application.
 */
@FeignClient(name = "job-service", path = "/api/v1/jobs")
public interface JobServiceClient {

    @GetMapping("/{jobId}/exists")
    ApiResponse<Boolean> jobExists(@PathVariable Long jobId);

    @GetMapping("/{jobId}/recruiter")
    ApiResponse<Long> getRecruiterIdByJob(@PathVariable Long jobId);
}
