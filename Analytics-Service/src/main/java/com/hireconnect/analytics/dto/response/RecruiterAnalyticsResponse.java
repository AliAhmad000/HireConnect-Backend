package com.hireconnect.analytics.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class RecruiterAnalyticsResponse {
    private Long recruiterId;
    private long totalJobsPosted;
    private long totalApplicationsReceived;
    private long totalJobViews;
    private long shortlistedCount;
    private long interviewScheduledCount;
    private long offeredCount;
    private long rejectedCount;
    private double viewToApplyRatio;
    private double avgTimeToHireDays;
    private Map<String, Long> applicationsByStatus;   // pipeline breakdown
    private Map<Long, Long> topJobsByApplications;    // jobId -> count
}
