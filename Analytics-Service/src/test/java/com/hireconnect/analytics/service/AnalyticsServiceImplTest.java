package com.hireconnect.analytics.service;

import com.hireconnect.analytics.dto.response.PlatformAnalyticsResponse;
import com.hireconnect.analytics.dto.response.RecruiterAnalyticsResponse;
import com.hireconnect.analytics.entity.ApplicationEvent;
import com.hireconnect.analytics.entity.JobViewEvent;
import com.hireconnect.analytics.event.AnalyticsEventListener;
import com.hireconnect.analytics.repository.ApplicationEventRepository;
import com.hireconnect.analytics.repository.JobViewEventRepository;
import com.hireconnect.analytics.service.AnalyticsService;
import com.hireconnect.analytics.service.impl.AnalyticsServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AnalyticsServiceImpl Tests")
class AnalyticsServiceImplTest {

    @Mock private JobViewEventRepository jobViewEventRepository;
    @Mock private ApplicationEventRepository applicationEventRepository;

    @InjectMocks private AnalyticsServiceImpl analyticsService;

    @Test
    @DisplayName("AnalyticsEventListener should route supported events")
    void shouldRouteSupportedAnalyticsEvents() {
        AnalyticsService analyticsServiceMock = mock(AnalyticsService.class);
        AnalyticsEventListener listener = new AnalyticsEventListener(analyticsServiceMock);

        listener.handleAnalyticsEvent(Map.of(
                "eventType", "APPLICATION_SUBMITTED",
                "applicationId", 100,
                "jobId", 10L,
                "recruiterId", "2",
                "candidateId", 1L));
        listener.handleAnalyticsEvent(Map.of(
                "eventType", "APPLICATION_STATUS_CHANGED",
                "applicationId", 100L,
                "jobId", 10L,
                "recruiterId", 2L,
                "candidateId", 1L,
                "oldStatus", "APPLIED",
                "newStatus", "SHORTLISTED"));
        listener.handleAnalyticsEvent(Map.of(
                "eventType", "JOB_VIEWED",
                "jobId", 10L,
                "recruiterId", 2L,
                "viewerId", 1,
                "viewerRole", "CANDIDATE"));
        listener.handleAnalyticsEvent(Map.of("eventType", "UNKNOWN"));
        listener.handleAnalyticsEvent(Map.of("eventType", "JOB_VIEWED", "jobId", "bad-id"));

        verify(analyticsServiceMock).recordApplicationEvent(100L, 10L, 2L, 1L,
                "APPLICATION_SUBMITTED", null, "APPLIED");
        verify(analyticsServiceMock).recordApplicationEvent(100L, 10L, 2L, 1L,
                "APPLICATION_STATUS_CHANGED", "APPLIED", "SHORTLISTED");
        verify(analyticsServiceMock, times(2)).recordJobView(any(), any(), any(), any());
    }

    // ─── getRecruiterStats() ───────────────────────────────────────────────────

    @Nested
    @DisplayName("getRecruiterStats()")
    class RecruiterStatsTests {

        @Test
        @DisplayName("should return correct recruiter analytics")
        void shouldReturnCorrectRecruiterAnalytics() {
            when(jobViewEventRepository.countByRecruiterId(1L)).thenReturn(200L);
            when(applicationEventRepository.countByRecruiterId(1L)).thenReturn(40L);
            when(applicationEventRepository.countByRecruiterIdAndNewStatus(1L, "SHORTLISTED")).thenReturn(20L);
            when(applicationEventRepository.countByRecruiterIdAndNewStatus(1L, "INTERVIEW_SCHEDULED")).thenReturn(10L);
            when(applicationEventRepository.countByRecruiterIdAndNewStatus(1L, "OFFERED")).thenReturn(5L);
            when(applicationEventRepository.countByRecruiterIdAndNewStatus(1L, "REJECTED")).thenReturn(15L);
            when(applicationEventRepository.findPipelineStatsByRecruiter(1L)).thenReturn(List.of());
            when(jobViewEventRepository.findTopJobsByViewsForRecruiter(1L)).thenReturn(List.of());

            RecruiterAnalyticsResponse result = analyticsService.getRecruiterStats(1L);

            assertThat(result).isNotNull();
            assertThat(result.getRecruiterId()).isEqualTo(1L);
            assertThat(result.getTotalJobViews()).isEqualTo(200L);
            assertThat(result.getTotalApplicationsReceived()).isEqualTo(40L);
            assertThat(result.getShortlistedCount()).isEqualTo(20L);
            assertThat(result.getInterviewScheduledCount()).isEqualTo(10L);
            assertThat(result.getOfferedCount()).isEqualTo(5L);
            assertThat(result.getRejectedCount()).isEqualTo(15L);
        }

        @Test
        @DisplayName("should compute view-to-apply ratio correctly")
        void shouldComputeViewToApplyRatioCorrectly() {
            when(jobViewEventRepository.countByRecruiterId(1L)).thenReturn(100L);
            when(applicationEventRepository.countByRecruiterId(1L)).thenReturn(25L);
            when(applicationEventRepository.countByRecruiterIdAndNewStatus(any(), any())).thenReturn(0L);
            when(applicationEventRepository.findPipelineStatsByRecruiter(1L)).thenReturn(List.of());
            when(jobViewEventRepository.findTopJobsByViewsForRecruiter(1L)).thenReturn(List.of());

            RecruiterAnalyticsResponse result = analyticsService.getRecruiterStats(1L);

            // 25 apps / 100 views = 0.25
            assertThat(result.getViewToApplyRatio()).isEqualTo(0.25);
        }

        @Test
        @DisplayName("should return 0.0 ratio when no views")
        void shouldReturnZeroRatioWhenNoViews() {
            when(jobViewEventRepository.countByRecruiterId(1L)).thenReturn(0L);
            when(applicationEventRepository.countByRecruiterId(1L)).thenReturn(0L);
            when(applicationEventRepository.countByRecruiterIdAndNewStatus(any(), any())).thenReturn(0L);
            when(applicationEventRepository.findPipelineStatsByRecruiter(1L)).thenReturn(List.of());
            when(jobViewEventRepository.findTopJobsByViewsForRecruiter(1L)).thenReturn(List.of());

            RecruiterAnalyticsResponse result = analyticsService.getRecruiterStats(1L);

            assertThat(result.getViewToApplyRatio()).isEqualTo(0.0);
        }
    }

    // ─── getPlatformStats() ────────────────────────────────────────────────────

    @Nested
    @DisplayName("getPlatformStats()")
    class PlatformStatsTests {

        @Test
        @DisplayName("should return platform-wide analytics")
        void shouldReturnPlatformAnalytics() {
            when(applicationEventRepository.countTotalApplications()).thenReturn(1500L);
            when(jobViewEventRepository.count()).thenReturn(10000L);
            when(applicationEventRepository.findGlobalPipelineStats()).thenReturn(List.of(
                    new Object[]{"SHORTLISTED", 400L},
                    new Object[]{"OFFERED", 100L},
                    new Object[]{"REJECTED", 600L}
            ));

            PlatformAnalyticsResponse result = analyticsService.getPlatformStats();

            assertThat(result.getTotalApplicationEvents()).isEqualTo(1500L);
            assertThat(result.getTotalJobViews()).isEqualTo(10000L);
            assertThat(result.getTotalShortlisted()).isEqualTo(400L);
            assertThat(result.getTotalOffered()).isEqualTo(100L);
            assertThat(result.getTotalRejected()).isEqualTo(600L);
            assertThat(result.getApplicationsByStatus()).containsKeys("SHORTLISTED", "OFFERED", "REJECTED");
        }
    }

    // ─── getJobViewCount() ─────────────────────────────────────────────────────

    @Test
    @DisplayName("getJobViewCount() should return correct count")
    void shouldReturnJobViewCount() {
        when(jobViewEventRepository.countByJobId(10L)).thenReturn(350L);
        assertThat(analyticsService.getJobViewCount(10L)).isEqualTo(350L);
    }

    // ─── getApplicationCountByJob() ────────────────────────────────────────────

    @Test
    @DisplayName("getApplicationCountByJob() should return correct count")
    void shouldReturnApplicationCountByJob() {
        when(applicationEventRepository.countByJobId(10L)).thenReturn(75L);
        assertThat(analyticsService.getApplicationCountByJob(10L)).isEqualTo(75L);
    }

    // ─── getViewToApplyRatio() ─────────────────────────────────────────────────

    @Nested
    @DisplayName("getViewToApplyRatio()")
    class ViewToApplyTests {

        @Test
        @DisplayName("should return correct ratio for a job")
        void shouldReturnCorrectRatioForJob() {
            when(jobViewEventRepository.countByJobId(10L)).thenReturn(200L);
            when(applicationEventRepository.countByJobId(10L)).thenReturn(50L);

            double ratio = analyticsService.getViewToApplyRatio(10L);

            assertThat(ratio).isEqualTo(0.25); // 50/200
        }

        @Test
        @DisplayName("should return 0.0 when no views for job")
        void shouldReturnZeroWhenNoViews() {
            when(jobViewEventRepository.countByJobId(10L)).thenReturn(0L);
            when(applicationEventRepository.countByJobId(10L)).thenReturn(0L);

            assertThat(analyticsService.getViewToApplyRatio(10L)).isEqualTo(0.0);
        }
    }

    // ─── recordJobView() ───────────────────────────────────────────────────────

    @Test
    @DisplayName("recordJobView() should save a JobViewEvent")
    void shouldSaveJobViewEvent() {
        when(jobViewEventRepository.save(any(JobViewEvent.class)))
                .thenReturn(JobViewEvent.builder().id(1L).jobId(10L).build());

        assertThatCode(() -> analyticsService.recordJobView(10L, 2L, 1L, "CANDIDATE"))
                .doesNotThrowAnyException();

        verify(jobViewEventRepository).save(argThat(e ->
                e.getJobId().equals(10L) && e.getViewerRole().equals("CANDIDATE")));
    }

    // ─── recordApplicationEvent() ──────────────────────────────────────────────

    @Test
    @DisplayName("recordApplicationEvent() should save an ApplicationEvent")
    void shouldSaveApplicationEvent() {
        when(applicationEventRepository.save(any(ApplicationEvent.class)))
                .thenReturn(ApplicationEvent.builder().id(1L).build());

        assertThatCode(() -> analyticsService.recordApplicationEvent(
                100L, 10L, 2L, 1L,
                "APPLICATION_STATUS_CHANGED", "APPLIED", "SHORTLISTED"))
                .doesNotThrowAnyException();

        verify(applicationEventRepository).save(argThat(e ->
                e.getEventType().equals("APPLICATION_STATUS_CHANGED") &&
                e.getOldStatus().equals("APPLIED") &&
                e.getNewStatus().equals("SHORTLISTED")));
    }
}
