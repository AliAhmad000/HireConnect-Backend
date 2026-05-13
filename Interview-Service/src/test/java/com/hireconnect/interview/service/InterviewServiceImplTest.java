package com.hireconnect.interview.service;

import com.hireconnect.interview.dto.request.RescheduleInterviewRequest;
import com.hireconnect.interview.dto.request.ScheduleInterviewRequest;
import com.hireconnect.interview.dto.response.InterviewResponse;
import com.hireconnect.interview.entity.Interview;
import com.hireconnect.interview.enums.InterviewMode;
import com.hireconnect.interview.enums.InterviewStatus;
import com.hireconnect.interview.exception.ResourceNotFoundException;
import com.hireconnect.interview.mapper.InterviewMapper;
import com.hireconnect.interview.repository.InterviewRepository;
import com.hireconnect.interview.service.impl.InterviewServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("InterviewServiceImpl Tests")
class InterviewServiceImplTest {

    @Mock private InterviewRepository interviewRepository;
    @Mock private InterviewMapper interviewMapper;
    @Mock private RabbitTemplate rabbitTemplate;

    @InjectMocks private InterviewServiceImpl interviewService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(interviewService, "exchange", "hireconnect.exchange");
        ReflectionTestUtils.setField(interviewService, "notificationRoutingKey", "notification.routing.key");
    }

    private Interview buildInterview(Long id, InterviewStatus status) {
        return Interview.builder()
                .interviewId(id)
                .applicationId(100L)
                .candidateId(1L)
                .recruiterId(2L)
                .jobId(10L)
                .jobTitle("Software Engineer")
                .candidateName("Alice")
                .candidateEmail("alice@example.com")
                .companyName("TechCorp")
                .scheduledAt(LocalDateTime.now().plusDays(3))
                .mode(InterviewMode.ONLINE)
                .meetLink("https://meet.google.com/abc-def")
                .durationMinutes(60)
                .roundNumber(1)
                .status(status)
                .build();
    }

    private InterviewResponse buildResponse(Interview i) {
        return InterviewResponse.builder()
                .interviewId(i.getInterviewId())
                .status(i.getStatus())
                .candidateId(i.getCandidateId())
                .recruiterId(i.getRecruiterId())
                .build();
    }

    @Nested
    @DisplayName("scheduleInterview()")
    class ScheduleTests {

        @Test
        @DisplayName("should schedule interview successfully")
        void shouldScheduleSuccessfully() {
            ScheduleInterviewRequest request = new ScheduleInterviewRequest();
            request.setApplicationId(100L);
            request.setCandidateId(1L);
            request.setJobId(10L);
            request.setJobTitle("Software Engineer");
            request.setCandidateName("Alice");
            request.setCandidateEmail("alice@example.com");
            request.setCompanyName("TechCorp");
            request.setScheduledAt(LocalDateTime.now().plusDays(3));
            request.setMode(InterviewMode.ONLINE);
            request.setMeetLink("https://meet.google.com/abc-def");

            Interview saved = buildInterview(1L, InterviewStatus.SCHEDULED);
            InterviewResponse response = buildResponse(saved);

            when(interviewRepository.save(any(Interview.class))).thenReturn(saved);
            when(interviewMapper.toResponse(saved)).thenReturn(response);

            InterviewResponse result = interviewService.scheduleInterview(2L, request);

            assertThat(result).isNotNull();
            assertThat(result.getInterviewId()).isEqualTo(1L);
            verify(interviewRepository).save(any(Interview.class));
            verify(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));
        }
    }

    @Nested
    @DisplayName("confirmInterview()")
    class ConfirmTests {

        @Test
        @DisplayName("should confirm a SCHEDULED interview")
        void shouldConfirmScheduledInterview() {
            Interview interview = buildInterview(1L, InterviewStatus.SCHEDULED);
            Interview confirmed = buildInterview(1L, InterviewStatus.CONFIRMED);
            InterviewResponse response = buildResponse(confirmed);

            when(interviewRepository.findById(1L)).thenReturn(Optional.of(interview));
            when(interviewRepository.save(any())).thenReturn(confirmed);
            when(interviewMapper.toResponse(any())).thenReturn(response);

            InterviewResponse result = interviewService.confirmInterview(1L, 1L);

            assertThat(result.getStatus()).isEqualTo(InterviewStatus.CONFIRMED);
        }

        @Test
        @DisplayName("should throw AccessDeniedException when wrong candidate tries to confirm")
        void shouldThrowWhenWrongCandidateConfirms() {
            Interview interview = buildInterview(1L, InterviewStatus.SCHEDULED);
            when(interviewRepository.findById(1L)).thenReturn(Optional.of(interview));

            assertThatThrownBy(() -> interviewService.confirmInterview(1L, 99L))
                    .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        @DisplayName("should throw IllegalStateException when confirming cancelled interview")
        void shouldThrowWhenConfirmingCancelledInterview() {
            Interview interview = buildInterview(1L, InterviewStatus.CANCELLED);
            when(interviewRepository.findById(1L)).thenReturn(Optional.of(interview));

            assertThatThrownBy(() -> interviewService.confirmInterview(1L, 1L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("CANCELLED");
        }
    }

    @Nested
    @DisplayName("cancelInterview()")
    class CancelTests {

        @Test
        @DisplayName("should cancel a SCHEDULED interview as recruiter")
        void shouldCancelScheduledInterviewAsRecruiter() {
            Interview interview = buildInterview(1L, InterviewStatus.SCHEDULED);
            Interview cancelled = buildInterview(1L, InterviewStatus.CANCELLED);
            InterviewResponse response = buildResponse(cancelled);

            when(interviewRepository.findById(1L)).thenReturn(Optional.of(interview));
            when(interviewRepository.save(any())).thenReturn(cancelled);
            when(interviewMapper.toResponse(any())).thenReturn(response);

            InterviewResponse result = interviewService.cancelInterview(1L, 2L, "Recruiter cancelled");

            assertThat(result.getStatus()).isEqualTo(InterviewStatus.CANCELLED);
        }

        @Test
        @DisplayName("should throw when cancelling a COMPLETED interview")
        void shouldThrowWhenCancellingCompletedInterview() {
            Interview interview = buildInterview(1L, InterviewStatus.COMPLETED);
            when(interviewRepository.findById(1L)).thenReturn(Optional.of(interview));

            assertThatThrownBy(() -> interviewService.cancelInterview(1L, 2L, "reason"))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("rescheduleInterview()")
    class RescheduleTests {

        @Test
        @DisplayName("should reschedule a CONFIRMED interview as recruiter")
        void shouldRescheduleConfirmedInterview() {
            Interview interview = buildInterview(1L, InterviewStatus.CONFIRMED);
            Interview rescheduled = buildInterview(1L, InterviewStatus.RESCHEDULED);
            InterviewResponse response = buildResponse(rescheduled);

            RescheduleInterviewRequest request = new RescheduleInterviewRequest();
            request.setNewScheduledAt(LocalDateTime.now().plusDays(5));
            request.setRescheduleReason("Candidate requested");

            when(interviewRepository.findById(1L)).thenReturn(Optional.of(interview));
            when(interviewRepository.save(any())).thenReturn(rescheduled);
            when(interviewMapper.toResponse(any())).thenReturn(response);

            InterviewResponse result = interviewService.rescheduleInterview(1L, request, 2L);

            assertThat(result.getStatus()).isEqualTo(InterviewStatus.RESCHEDULED);
        }

        @Test
        @DisplayName("should store candidate reschedule request with requested date")
        void shouldRequestRescheduleWithRequestedDate() {
            Interview interview = buildInterview(1L, InterviewStatus.CONFIRMED);
            Interview requested = buildInterview(1L, InterviewStatus.RESCHEDULE_REQUESTED);
            LocalDateTime requestedAt = LocalDateTime.now().plusDays(5);
            InterviewResponse response = buildResponse(requested);

            RescheduleInterviewRequest request = new RescheduleInterviewRequest();
            request.setNewScheduledAt(requestedAt);
            request.setRescheduleReason("Candidate requested a different time");

            when(interviewRepository.findById(1L)).thenReturn(Optional.of(interview));
            when(interviewRepository.save(any())).thenReturn(requested);
            when(interviewMapper.toResponse(any())).thenReturn(response);

            InterviewResponse result = interviewService.requestReschedule(1L, 1L, request);

            assertThat(result.getStatus()).isEqualTo(InterviewStatus.RESCHEDULE_REQUESTED);
        }

        @Test
        @DisplayName("should throw when unauthorized user tries to reschedule")
        void shouldThrowWhenUnauthorizedUserReschedules() {
            Interview interview = buildInterview(1L, InterviewStatus.SCHEDULED);
            when(interviewRepository.findById(1L)).thenReturn(Optional.of(interview));

            RescheduleInterviewRequest request = new RescheduleInterviewRequest();
            request.setNewScheduledAt(LocalDateTime.now().plusDays(5));

            assertThatThrownBy(() -> interviewService.rescheduleInterview(1L, request, 999L))
                    .isInstanceOf(AccessDeniedException.class);
        }
    }

    @Test
    @DisplayName("getInterviewById() should throw when not found")
    void shouldThrowWhenInterviewNotFound() {
        when(interviewRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> interviewService.getInterviewById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    @DisplayName("completeInterview() should complete confirmed interview for recruiter")
    void shouldCompleteInterview() {
        Interview interview = buildInterview(1L, InterviewStatus.CONFIRMED);
        Interview completed = buildInterview(1L, InterviewStatus.COMPLETED);
        InterviewResponse response = buildResponse(completed);

        when(interviewRepository.findById(1L)).thenReturn(Optional.of(interview));
        when(interviewRepository.save(any())).thenReturn(completed);
        when(interviewMapper.toResponse(any())).thenReturn(response);

        assertThat(interviewService.completeInterview(1L, 2L).getStatus())
                .isEqualTo(InterviewStatus.COMPLETED);
    }

    @Test
    @DisplayName("query methods should map interview lists and pages")
    void shouldMapQueryMethods() {
        Interview interview = buildInterview(1L, InterviewStatus.SCHEDULED);
        InterviewResponse response = buildResponse(interview);
        var pageable = PageRequest.of(0, 10);

        when(interviewMapper.toResponse(interview)).thenReturn(response);
        when(interviewRepository.findByApplicationId(100L)).thenReturn(List.of(interview));
        when(interviewRepository.findByCandidateId(1L, pageable)).thenReturn(new PageImpl<>(List.of(interview)));
        when(interviewRepository.findUpcomingByCandidate(eq(1L), any())).thenReturn(List.of(interview));
        when(interviewRepository.findByRecruiterId(2L, pageable)).thenReturn(new PageImpl<>(List.of(interview)));
        when(interviewRepository.findUpcomingByRecruiter(eq(2L), any())).thenReturn(List.of(interview));
        when(interviewRepository.findByRecruiterIdAndStatus(2L, InterviewStatus.SCHEDULED, pageable))
                .thenReturn(new PageImpl<>(List.of(interview)));

        assertThat(interviewService.getInterviewsByApplication(100L)).hasSize(1);
        assertThat(interviewService.getInterviewsByCandidate(1L, pageable)).hasSize(1);
        assertThat(interviewService.getUpcomingInterviewsByCandidate(1L)).hasSize(1);
        assertThat(interviewService.getInterviewsByRecruiter(2L, pageable)).hasSize(1);
        assertThat(interviewService.getUpcomingInterviewsByRecruiter(2L)).hasSize(1);
        assertThat(interviewService.getInterviewsByRecruiterAndStatus(2L, InterviewStatus.SCHEDULED, pageable)).hasSize(1);
    }

    @Test
    @DisplayName("rejectReschedule() should restore previous status")
    void shouldRejectReschedule() {
        Interview interview = buildInterview(1L, InterviewStatus.CONFIRMED);
        LocalDateTime requestedAt = LocalDateTime.now().plusDays(4);
        interview.setRescheduleReason("Requested date: " + requestedAt + "\nPrevious status: CONFIRMED\nNeed another slot");
        InterviewResponse response = buildResponse(interview);

        when(interviewRepository.findById(1L)).thenReturn(Optional.of(interview));
        when(interviewRepository.save(interview)).thenReturn(interview);
        when(interviewMapper.toResponse(interview)).thenReturn(response);

        InterviewResponse result = interviewService.rejectReschedule(1L, 2L, "Not available");

        assertThat(result.getStatus()).isEqualTo(InterviewStatus.CONFIRMED);
        assertThat(interview.getRescheduleReason()).isNull();
        assertThat(interview.getNotes()).contains("Reschedule rejected");
    }
}
