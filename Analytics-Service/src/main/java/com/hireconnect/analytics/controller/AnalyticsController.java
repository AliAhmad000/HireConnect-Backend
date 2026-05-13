package com.hireconnect.analytics.controller;

import com.hireconnect.analytics.dto.response.ApiResponse;
import org.springframework.security.core.Authentication;
import com.hireconnect.analytics.dto.response.PlatformAnalyticsResponse;
import com.hireconnect.analytics.dto.response.RecruiterAnalyticsResponse;
import com.hireconnect.analytics.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@Slf4j
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    /** Recruiter dashboard analytics */
    @GetMapping("/recruiter")
    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
    public ResponseEntity<ApiResponse<RecruiterAnalyticsResponse>> getRecruiterStats(
            Authentication authentication) {

        Long recruiterId = Long.parseLong(authentication.getName());

        log.info("GET /analytics/recruiter/{}", recruiterId);

        return ResponseEntity.ok(
            ApiResponse.success(
                analyticsService.getRecruiterStats(recruiterId)
            )
        );
    }

    /** Platform-wide admin analytics */
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PlatformAnalyticsResponse>> getPlatformStats() {
        log.info("GET /analytics/admin");
        return ResponseEntity.ok(ApiResponse.success(analyticsService.getPlatformStats()));
    }

    /** Job view count */
    @GetMapping("/jobs/{jobId}/views")
    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Long>> getJobViewCount(@PathVariable Long jobId) {
        return ResponseEntity.ok(ApiResponse.success(analyticsService.getJobViewCount(jobId)));
    }

    /** Application count for a job */
    @GetMapping("/jobs/{jobId}/applications/count")
    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Long>> getApplicationCount(@PathVariable Long jobId) {
        return ResponseEntity.ok(ApiResponse.success(analyticsService.getApplicationCountByJob(jobId)));
    }

    /** View-to-apply ratio for a job */
    @GetMapping("/jobs/{jobId}/view-to-apply")
    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Double>> getViewToApplyRatio(@PathVariable Long jobId) {
        return ResponseEntity.ok(ApiResponse.success(analyticsService.getViewToApplyRatio(jobId)));
    }

    /** Record a job view (called by job-service or API gateway) */
    @PostMapping("/jobs/{jobId}/view")
    public ResponseEntity<ApiResponse<Void>> recordJobView(
            @PathVariable Long jobId,
            @RequestParam(required = false) Long recruiterId,
            @RequestParam(required = false) Long viewerId,
            @RequestParam(required = false) String viewerRole) {
        analyticsService.recordJobView(jobId, recruiterId, viewerId, viewerRole);
        return ResponseEntity.ok(ApiResponse.success("View recorded", null));
    }
}
