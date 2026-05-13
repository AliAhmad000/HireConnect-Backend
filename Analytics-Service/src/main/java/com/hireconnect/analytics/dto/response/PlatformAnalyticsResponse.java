package com.hireconnect.analytics.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class PlatformAnalyticsResponse {
    private long totalApplicationEvents;
    private long totalJobViews;
    private Map<String, Long> applicationsByStatus;
    private long totalShortlisted;
    private long totalOffered;
    private long totalRejected;
}
