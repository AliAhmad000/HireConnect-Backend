package com.hireconnect.interview.client;

import com.hireconnect.interview.dto.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

/**
 * Feign client to communicate with application-service.
 * When an interview is scheduled, application status is updated to INTERVIEW_SCHEDULED.
 */
@FeignClient(name = "application-service", path = "/api/v1/applications")
public interface ApplicationServiceClient {

    @GetMapping("/{applicationId}")
    ApiResponse<Map<String, Object>> getApplication(@PathVariable Long applicationId);

    @PatchMapping("/{applicationId}/status")
    ApiResponse<Map<String, Object>> updateApplicationStatus(
            @PathVariable Long applicationId,
            @RequestBody Map<String, String> statusRequest);
}
