package com.hireconnect.analytics.service.impl;

import com.hireconnect.analytics.dto.response.PlatformAnalyticsResponse;
import com.hireconnect.analytics.dto.response.RecruiterAnalyticsResponse;
import com.hireconnect.analytics.entity.ApplicationEvent;
import com.hireconnect.analytics.entity.JobViewEvent;
import com.hireconnect.analytics.repository.ApplicationEventRepository;
import com.hireconnect.analytics.repository.JobViewEventRepository;
import com.hireconnect.analytics.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AnalyticsServiceImpl implements AnalyticsService {

    private final JobViewEventRepository jobViewEventRepository;
    private final ApplicationEventRepository applicationEventRepository;

    @Override
    @Transactional(readOnly = true)
    public RecruiterAnalyticsResponse getRecruiterStats(Long recruiterId) {
        log.info("Fetching analytics for recruiter {}", recruiterId);

        long totalViews        = jobViewEventRepository.countByRecruiterId(recruiterId);
        long totalApplications = applicationEventRepository.countByRecruiterId(recruiterId);
        long shortlisted       = applicationEventRepository.countByRecruiterIdAndNewStatus(recruiterId, "SHORTLISTED");
        long interviewScheduled= applicationEventRepository.countByRecruiterIdAndNewStatus(recruiterId, "INTERVIEW_SCHEDULED");
        long offered           = applicationEventRepository.countByRecruiterIdAndNewStatus(recruiterId, "OFFERED");
        long rejected          = applicationEventRepository.countByRecruiterIdAndNewStatus(recruiterId, "REJECTED");

        // Pipeline stats map
        List<Object[]> pipelineRaw = applicationEventRepository.findPipelineStatsByRecruiter(recruiterId);
        Map<String, Long> pipeline = new HashMap<>();
        for (Object[] row : pipelineRaw) {
            pipeline.put((String) row[0], (Long) row[1]);
        }

        // View-to-apply ratio
        double viewToApplyRatio = totalViews > 0 ? (double) totalApplications / totalViews : 0.0;

        // Top jobs by applications
        Map<Long, Long> topJobs = new HashMap<>();
        jobViewEventRepository.findTopJobsByViewsForRecruiter(recruiterId)
                .forEach(row -> topJobs.put((Long) row[0], (Long) row[1]));

        return RecruiterAnalyticsResponse.builder()
                .recruiterId(recruiterId)
                .totalJobViews(totalViews)
                .totalApplicationsReceived(totalApplications)
                .shortlistedCount(shortlisted)
                .interviewScheduledCount(interviewScheduled)
                .offeredCount(offered)
                .rejectedCount(rejected)
                .viewToApplyRatio(Math.round(viewToApplyRatio * 100.0) / 100.0)
                .avgTimeToHireDays(0.0) // would compute from interview/offer timestamps in production
                .applicationsByStatus(pipeline)
                .topJobsByApplications(topJobs)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PlatformAnalyticsResponse getPlatformStats() {
        log.info("Fetching platform-wide analytics");

        long totalApplicationEvents = applicationEventRepository.countTotalApplications();
        long totalJobViews          = jobViewEventRepository.count();

        List<Object[]> globalPipeline = applicationEventRepository.findGlobalPipelineStats();
        Map<String, Long> statusMap   = new HashMap<>();
        long shortlisted = 0, offered = 0, rejected = 0;
        for (Object[] row : globalPipeline) {
            String status = (String) row[0];
            long count    = (Long) row[1];
            statusMap.put(status, count);
            if ("SHORTLISTED".equals(status))          shortlisted = count;
            else if ("OFFERED".equals(status))         offered = count;
            else if ("REJECTED".equals(status))        rejected = count;
        }

        return PlatformAnalyticsResponse.builder()
                .totalApplicationEvents(totalApplicationEvents)
                .totalJobViews(totalJobViews)
                .applicationsByStatus(statusMap)
                .totalShortlisted(shortlisted)
                .totalOffered(offered)
                .totalRejected(rejected)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public long getJobViewCount(Long jobId) {
        return jobViewEventRepository.countByJobId(jobId);
    }

    @Override
    @Transactional(readOnly = true)
    public long getApplicationCountByJob(Long jobId) {
        return applicationEventRepository.countByJobId(jobId);
    }

    @Override
    @Transactional(readOnly = true)
    public double getViewToApplyRatio(Long jobId) {
        long views = jobViewEventRepository.countByJobId(jobId);
        long apps  = applicationEventRepository.countByJobId(jobId);
        return views > 0 ? Math.round((double) apps / views * 100.0) / 100.0 : 0.0;
    }

    @Override
    public void recordJobView(Long jobId, Long recruiterId, Long viewerId, String viewerRole) {
        JobViewEvent event = JobViewEvent.builder()
                .jobId(jobId)
                .recruiterId(recruiterId)
                .viewerId(viewerId)
                .viewerRole(viewerRole)
                .build();
        jobViewEventRepository.save(event);
    }

    @Override
    public void recordApplicationEvent(Long applicationId, Long jobId, Long recruiterId,
                                       Long candidateId, String eventType,
                                       String oldStatus, String newStatus) {
        ApplicationEvent event = ApplicationEvent.builder()
                .applicationId(applicationId)
                .jobId(jobId)
                .recruiterId(recruiterId)
                .candidateId(candidateId)
                .eventType(eventType)
                .oldStatus(oldStatus)
                .newStatus(newStatus)
                .build();
        applicationEventRepository.save(event);
        log.debug("Recorded analytics event: {} for application {}", eventType, applicationId);
    }
}
