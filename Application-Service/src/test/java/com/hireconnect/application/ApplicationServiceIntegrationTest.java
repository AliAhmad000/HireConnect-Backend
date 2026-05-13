package com.hireconnect.application;

import com.hireconnect.application.dto.request.SubmitApplicationRequest;
import com.hireconnect.application.dto.request.UpdateStatusRequest;
import com.hireconnect.application.dto.response.ApplicationResponse;
import com.hireconnect.application.enums.ApplicationStatus;
import com.hireconnect.application.repository.ApplicationRepository;
import com.hireconnect.application.service.ApplicationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.*;

/**
 * Full Spring context integration test — uses H2 in-memory DB.
 * RabbitMQ is mocked via autoconfigure exclude in test application.yml.
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Application Service — Integration Tests")
class ApplicationServiceIntegrationTest {

    @Autowired private ApplicationService applicationService;
    @Autowired private ApplicationRepository applicationRepository;

    @BeforeEach
    @AfterEach
    void cleanup() {
        applicationRepository.deleteAll();
    }

    private SubmitApplicationRequest buildRequest() {
        SubmitApplicationRequest r = new SubmitApplicationRequest();
        r.setJobId(10L);
        r.setRecruiterId(2L);
        r.setJobTitle("Software Engineer");
        r.setCompanyName("TechCorp");
        r.setResumeUrl("https://s3.example.com/resume.pdf");
        r.setCoverLetter("I am a great fit!");
        return r;
    }

    @Test
    @DisplayName("Full lifecycle: submit → shortlist → interview → offer")
    void shouldHandleFullApplicationLifecycle() {
        // 1. Submit
        ApplicationResponse submitted = applicationService.submitApplication(
                1L, "Alice", "alice@example.com", buildRequest());

        assertThat(submitted.getApplicationId()).isNotNull();
        assertThat(submitted.getStatus()).isEqualTo(ApplicationStatus.APPLIED);
        assertThat(submitted.getCandidateName()).isEqualTo("Alice");

        Long appId = submitted.getApplicationId();

        // 2. Shortlist
        UpdateStatusRequest shortlist = new UpdateStatusRequest();
        shortlist.setStatus(ApplicationStatus.SHORTLISTED);
        ApplicationResponse shortlisted = applicationService.updateApplicationStatus(appId, shortlist, 2L);
        assertThat(shortlisted.getStatus()).isEqualTo(ApplicationStatus.SHORTLISTED);

        // 3. Schedule interview
        UpdateStatusRequest scheduleInterview = new UpdateStatusRequest();
        scheduleInterview.setStatus(ApplicationStatus.INTERVIEW_SCHEDULED);
        ApplicationResponse interviewScheduled = applicationService.updateApplicationStatus(appId, scheduleInterview, 2L);
        assertThat(interviewScheduled.getStatus()).isEqualTo(ApplicationStatus.INTERVIEW_SCHEDULED);

        // 4. Offer
        UpdateStatusRequest offer = new UpdateStatusRequest();
        offer.setStatus(ApplicationStatus.OFFERED);
        ApplicationResponse offered = applicationService.updateApplicationStatus(appId, offer, 2L);
        assertThat(offered.getStatus()).isEqualTo(ApplicationStatus.OFFERED);

        // Verify persisted
        assertThat(applicationRepository.findById(appId))
                .isPresent()
                .get()
                .satisfies(a -> assertThat(a.getStatus()).isEqualTo(ApplicationStatus.OFFERED));
    }

    @Test
    @DisplayName("Should prevent duplicate applications")
    void shouldPreventDuplicateApplications() {
        applicationService.submitApplication(1L, "Alice", "alice@example.com", buildRequest());

        assertThatThrownBy(() ->
                applicationService.submitApplication(1L, "Alice", "alice@example.com", buildRequest()))
                .hasMessageContaining("already applied");
    }

    @Test
    @DisplayName("Should track hasApplied correctly")
    void shouldTrackHasApplied() {
        assertThat(applicationService.hasApplied(10L, 1L)).isFalse();

        applicationService.submitApplication(1L, "Alice", "alice@example.com", buildRequest());

        assertThat(applicationService.hasApplied(10L, 1L)).isTrue();
        assertThat(applicationService.hasApplied(10L, 99L)).isFalse();
    }

    @Test
    @DisplayName("Should withdraw application and prevent further status updates")
    void shouldWithdrawAndBlockFurtherUpdates() {
        ApplicationResponse submitted = applicationService.submitApplication(
                1L, "Alice", "alice@example.com", buildRequest());

        Long appId = submitted.getApplicationId();

        // Withdraw
        applicationService.withdrawApplication(appId, 1L);

        // Verify withdrawn
        assertThat(applicationRepository.findById(appId))
                .isPresent()
                .get()
                .satisfies(a -> {
                    assertThat(a.getIsWithdrawn()).isTrue();
                    assertThat(a.getStatus()).isEqualTo(ApplicationStatus.WITHDRAWN);
                });

        // Further update should fail
        UpdateStatusRequest update = new UpdateStatusRequest();
        update.setStatus(ApplicationStatus.SHORTLISTED);
        assertThatThrownBy(() -> applicationService.updateApplicationStatus(appId, update, 2L))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Should count applications correctly")
    void shouldCountApplicationsCorrectly() {
        // No applications yet
        assertThat(applicationService.countApplicationsByJob(10L)).isEqualTo(0L);

        // Submit from candidate 1
        applicationService.submitApplication(1L, "Alice", "alice@example.com", buildRequest());
        assertThat(applicationService.countApplicationsByJob(10L)).isEqualTo(1L);

        // Submit from candidate 2
        applicationService.submitApplication(2L, "Bob", "bob@example.com", buildRequest());
        assertThat(applicationService.countApplicationsByJob(10L)).isEqualTo(2L);

        // Count by candidate
        assertThat(applicationService.countApplicationsByCandidate(1L)).isEqualTo(1L);
        assertThat(applicationService.countApplicationsByCandidate(2L)).isEqualTo(1L);
    }
}
