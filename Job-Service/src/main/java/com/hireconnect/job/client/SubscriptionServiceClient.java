package com.hireconnect.job.client;

import com.hireconnect.job.dto.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "subscription-service", path = "/api/v1/subscriptions")
public interface SubscriptionServiceClient {

    @GetMapping("/internal/recruiters/{recruiterId}/max-posts")
    ApiResponse<Integer> getMaxJobPosts(@PathVariable Long recruiterId);
}
