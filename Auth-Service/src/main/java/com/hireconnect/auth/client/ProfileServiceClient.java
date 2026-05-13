package com.hireconnect.auth.client;

import com.hireconnect.auth.dto.request.CandidateProfileRequest;
import com.hireconnect.auth.dto.request.RecruiterProfileRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "profile-service", path = "/api/v1/profiles")
public interface ProfileServiceClient {

    @PostMapping("/candidate")
    void createCandidateProfile(
            @RequestBody CandidateProfileRequest request,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("Authorization") String authorization);

    @PostMapping("/recruiter")
    void createRecruiterProfile(
            @RequestBody RecruiterProfileRequest request,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("Authorization") String authorization);
}
