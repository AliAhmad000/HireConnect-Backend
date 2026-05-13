package com.hireconnect.analytics.client;

import com.hireconnect.analytics.dto.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Feign client to query application-service for live counts
 * when analytics cache needs refreshing.
 */
@FeignClient(name = "application-service", path = "/api/v1/applications")
public interface ApplicationServiceClient {

    @GetMapping("/job/{jobId}/count")
    ApiResponse<Long> countByJob(@PathVariable Long jobId);

    @GetMapping("/check")
    ApiResponse<Boolean> hasApplied(@RequestParam Long jobId, @RequestParam Long candidateId);
}
