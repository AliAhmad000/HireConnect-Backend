package com.hireconnect.application.service;

import com.hireconnect.application.dto.request.SubmitApplicationRequest;
import com.hireconnect.application.dto.request.RecruiterMessageRequest;
import com.hireconnect.application.dto.request.UpdateStatusRequest;
import com.hireconnect.application.dto.response.ApplicationResponse;
import com.hireconnect.application.entity.Application;
import com.hireconnect.application.enums.ApplicationStatus;
import com.hireconnect.application.exception.DuplicateApplicationException;
import com.hireconnect.application.exception.InvalidStatusTransitionException;
import com.hireconnect.application.exception.ResourceNotFoundException;
import com.hireconnect.application.mapper.ApplicationMapper;
import com.hireconnect.application.repository.ApplicationRepository;
import com.hireconnect.application.service.impl.ApplicationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ApplicationServiceImpl Tests")
class ApplicationServiceImplTest {

    @Mock private ApplicationRepository applicationRepository;
    @Mock private ApplicationMapper applicationMapper;
    @Mock private RabbitTemplate rabbitTemplate;

    @InjectMocks private ApplicationServiceImpl applicationService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(applicationService, "exchange", "hireconnect.exchange");
        ReflectionTestUtils.setField(applicationService, "notificationRoutingKey", "notification.routing.key");
        ReflectionTestUtils.setField(applicationService, "analyticsRoutingKey", "analytics.routing.key");
    }

    // ─── Fixtures ──────────────────────────────────────────────────────────────

    private Application buildApplication(Long id, ApplicationStatus status) {
        return Application.builder()
                .applicationId(id)
                .jobId(10L)
                .candidateId(1L)
                .recruiterId(2L)
                .jobTitle("Software Engineer")
                .companyName("TechCorp")
                .candidateName("John Doe")
                .candidateEmail("john@example.com")
                .resumeUrl("https://s3.example.com/resume.pdf")
                .status(status)
                .appliedAt(LocalDate.now())
                .isWithdrawn(false)
                .build();
    }

    private ApplicationResponse buildResponse(Application app) {
        return ApplicationResponse.builder()
                .applicationId(app.getApplicationId())
                .jobId(app.getJobId())
                .candidateId(app.getCandidateId())
                .status(app.getStatus())
                .isWithdrawn(app.getIsWithdrawn())
                .build();
    }

    // ─── submitApplication ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("submitApplication()")
    class SubmitApplicationTests {

        @Test
        @DisplayName("should submit application successfully when no duplicate exists")
        void shouldSubmitApplicationSuccessfully() {
            SubmitApplicationRequest request = new SubmitApplicationRequest();
            request.setJobId(10L);
            request.setRecruiterId(2L);
            request.setJobTitle("Software Engineer");
            request.setCompanyName("TechCorp");
            request.setResumeUrl("https://s3.example.com/resume.pdf");

            Application saved = buildApplication(100L, ApplicationStatus.APPLIED);
            ApplicationResponse expectedResponse = buildResponse(saved);

            when(applicationRepository.existsByJobIdAndCandidateId(10L, 1L)).thenReturn(false);
            when(applicationRepository.save(any(Application.class))).thenReturn(saved);
            when(applicationMapper.toResponse(saved)).thenReturn(expectedResponse);

            ApplicationResponse result = applicationService.submitApplication(
                    1L, "John Doe", "john@example.com", request);

            assertThat(result).isNotNull();
            assertThat(result.getApplicationId()).isEqualTo(100L);
            assertThat(result.getStatus()).isEqualTo(ApplicationStatus.APPLIED);

            verify(applicationRepository).save(any(Application.class));
            verify(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));
        }

        @Test
        @DisplayName("should throw DuplicateApplicationException when candidate already applied")
        void shouldThrowDuplicateExceptionWhenAlreadyApplied() {
            SubmitApplicationRequest request = new SubmitApplicationRequest();
            request.setJobId(10L);
            request.setRecruiterId(2L);
            request.setJobTitle("Software Engineer");
            request.setCompanyName("TechCorp");
            request.setResumeUrl("https://s3.example.com/resume.pdf");

            when(applicationRepository.existsByJobIdAndCandidateId(10L, 1L)).thenReturn(true);

            assertThatThrownBy(() ->
                    applicationService.submitApplication(1L, "John Doe", "john@example.com", request))
                    .isInstanceOf(DuplicateApplicationException.class)
                    .hasMessageContaining("already applied");

            verify(applicationRepository, never()).save(any());
        }
    }

    // ─── updateApplicationStatus ───────────────────────────────────────────────

    @Nested
    @DisplayName("updateApplicationStatus()")
    class UpdateStatusTests {

        @Test
        @DisplayName("should shortlist an APPLIED application")
        void shouldShortlistAppliedApplication() {
            Application app = buildApplication(1L, ApplicationStatus.APPLIED);
            UpdateStatusRequest request = new UpdateStatusRequest();
            request.setStatus(ApplicationStatus.SHORTLISTED);

            Application updated = buildApplication(1L, ApplicationStatus.SHORTLISTED);
            ApplicationResponse expectedResponse = buildResponse(updated);

            when(applicationRepository.findById(1L)).thenReturn(Optional.of(app));
            when(applicationRepository.save(any(Application.class))).thenReturn(updated);
            when(applicationMapper.toResponse(any(Application.class))).thenReturn(expectedResponse);

            ApplicationResponse result = applicationService.updateApplicationStatus(1L, request, 2L);

            assertThat(result.getStatus()).isEqualTo(ApplicationStatus.SHORTLISTED);
            verify(applicationRepository).save(any(Application.class));
        }

        @Test
        @DisplayName("should throw InvalidStatusTransitionException for invalid transition")
        void shouldThrowOnInvalidStatusTransition() {
            Application app = buildApplication(1L, ApplicationStatus.APPLIED);
            UpdateStatusRequest request = new UpdateStatusRequest();
            request.setStatus(ApplicationStatus.OFFERED); // APPLIED → OFFERED is invalid

            when(applicationRepository.findById(1L)).thenReturn(Optional.of(app));

            assertThatThrownBy(() ->
                    applicationService.updateApplicationStatus(1L, request, 2L))
                    .isInstanceOf(InvalidStatusTransitionException.class)
                    .hasMessageContaining("APPLIED")
                    .hasMessageContaining("OFFERED");
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when application not found")
        void shouldThrowWhenApplicationNotFound() {
            UpdateStatusRequest request = new UpdateStatusRequest();
            request.setStatus(ApplicationStatus.SHORTLISTED);

            when(applicationRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    applicationService.updateApplicationStatus(999L, request, 2L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("999");
        }

        @Test
        @DisplayName("should throw IllegalStateException when updating withdrawn application")
        void shouldThrowWhenApplicationIsWithdrawn() {
            Application app = buildApplication(1L, ApplicationStatus.WITHDRAWN);
            app.setIsWithdrawn(true);

            UpdateStatusRequest request = new UpdateStatusRequest();
            request.setStatus(ApplicationStatus.SHORTLISTED);

            when(applicationRepository.findById(1L)).thenReturn(Optional.of(app));

            assertThatThrownBy(() ->
                    applicationService.updateApplicationStatus(1L, request, 2L))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    // ─── withdrawApplication ───────────────────────────────────────────────────

    @Nested
    @DisplayName("sendMessageToCandidate()")
    class SendMessageToCandidateTests {

        @Test
        @DisplayName("should allow recruiter to message an interview-scheduled candidate")
        void shouldAllowMessageForInterviewScheduledCandidate() {
            Application app = buildApplication(1L, ApplicationStatus.INTERVIEW_SCHEDULED);
            RecruiterMessageRequest request = new RecruiterMessageRequest();
            request.setMessage("Please confirm your availability for the next round.");

            when(applicationRepository.findById(1L)).thenReturn(Optional.of(app));

            assertThatCode(() -> applicationService.sendMessageToCandidate(1L, request, 2L))
                    .doesNotThrowAnyException();

            verify(rabbitTemplate).convertAndSend(
                    eq("hireconnect.exchange"),
                    eq("notification.routing.key"),
                    ArgumentMatchers.<Object>argThat(event -> event instanceof java.util.Map<?, ?> map
                            && "CANDIDATE_MESSAGE".equals(map.get("eventType"))
                            && "Please confirm your availability for the next round.".equals(map.get("message"))));
        }

        @Test
        @DisplayName("should block recruiter message before candidate is shortlisted")
        void shouldBlockMessageForAppliedCandidate() {
            Application app = buildApplication(1L, ApplicationStatus.APPLIED);
            RecruiterMessageRequest request = new RecruiterMessageRequest();
            request.setMessage("Hello candidate.");

            when(applicationRepository.findById(1L)).thenReturn(Optional.of(app));

            assertThatThrownBy(() -> applicationService.sendMessageToCandidate(1L, request, 2L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("shortlisted, scheduled, or offered");

            verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
        }
    }

    @Nested
    @DisplayName("withdrawApplication()")
    class WithdrawApplicationTests {

        @Test
        @DisplayName("should withdraw application successfully")
        void shouldWithdrawApplicationSuccessfully() {
            Application app = buildApplication(1L, ApplicationStatus.APPLIED);

            when(applicationRepository.findById(1L)).thenReturn(Optional.of(app));
            when(applicationRepository.save(any(Application.class))).thenReturn(app);

            assertThatCode(() -> applicationService.withdrawApplication(1L, 1L))
                    .doesNotThrowAnyException();

            verify(applicationRepository).save(argThat(a -> a.getIsWithdrawn() == Boolean.TRUE));
        }

        @Test
        @DisplayName("should throw when trying to withdraw an OFFERED application")
        void shouldThrowWhenWithdrawingOfferedApplication() {
            Application app = buildApplication(1L, ApplicationStatus.OFFERED);

            when(applicationRepository.findById(1L)).thenReturn(Optional.of(app));

            assertThatThrownBy(() -> applicationService.withdrawApplication(1L, 1L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("OFFERED");
        }

        @Test
        @DisplayName("should throw when already withdrawn")
        void shouldThrowWhenAlreadyWithdrawn() {
            Application app = buildApplication(1L, ApplicationStatus.WITHDRAWN);
            app.setIsWithdrawn(true);

            when(applicationRepository.findById(1L)).thenReturn(Optional.of(app));

            assertThatThrownBy(() -> applicationService.withdrawApplication(1L, 1L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("already withdrawn");
        }
    }

    // ─── hasApplied / count ────────────────────────────────────────────────────

    @Test
    @DisplayName("hasApplied() should return true when application exists")
    void hasAppliedShouldReturnTrue() {
        when(applicationRepository.existsByJobIdAndCandidateId(10L, 1L)).thenReturn(true);
        assertThat(applicationService.hasApplied(10L, 1L)).isTrue();
    }

    @Test
    @DisplayName("hasApplied() should return false when no application exists")
    void hasAppliedShouldReturnFalse() {
        when(applicationRepository.existsByJobIdAndCandidateId(10L, 99L)).thenReturn(false);
        assertThat(applicationService.hasApplied(10L, 99L)).isFalse();
    }

    @Test
    @DisplayName("countApplicationsByJob() should return correct count")
    void countApplicationsByJobShouldReturnCount() {
        when(applicationRepository.countByJobId(10L)).thenReturn(42L);
        assertThat(applicationService.countApplicationsByJob(10L)).isEqualTo(42L);
    }
}
