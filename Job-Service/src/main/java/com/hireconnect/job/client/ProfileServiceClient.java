package com.hireconnect.job.client;

import com.hireconnect.job.dto.response.ApiResponse;
import com.hireconnect.job.dto.response.CandidateNotificationRecipientResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@FeignClient(name = "profile-service", path = "/api/v1/profiles")
public interface ProfileServiceClient {

    @GetMapping("/internal/candidates/notification-recipients")
    ApiResponse<List<CandidateNotificationRecipientResponse>> getCandidateNotificationRecipients();
}
