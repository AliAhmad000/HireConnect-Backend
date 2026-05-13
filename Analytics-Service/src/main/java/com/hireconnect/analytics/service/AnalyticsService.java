package com.hireconnect.analytics.service;

import com.hireconnect.analytics.dto.response.PlatformAnalyticsResponse;
import com.hireconnect.analytics.dto.response.RecruiterAnalyticsResponse;

public interface AnalyticsService {
    RecruiterAnalyticsResponse getRecruiterStats(Long recruiterId);
    PlatformAnalyticsResponse getPlatformStats();
    long getJobViewCount(Long jobId);
    long getApplicationCountByJob(Long jobId);
    double getViewToApplyRatio(Long jobId);
    void recordJobView(Long jobId, Long recruiterId, Long viewerId, String viewerRole);
    void recordApplicationEvent(Long applicationId, Long jobId, Long recruiterId,
                                Long candidateId, String eventType, String oldStatus, String newStatus);
}
