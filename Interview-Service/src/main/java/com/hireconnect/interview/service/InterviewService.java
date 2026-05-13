package com.hireconnect.interview.service;

import com.hireconnect.interview.dto.request.RescheduleInterviewRequest;
import com.hireconnect.interview.dto.request.ScheduleInterviewRequest;
import com.hireconnect.interview.dto.response.InterviewResponse;
import com.hireconnect.interview.enums.InterviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface InterviewService {
    InterviewResponse scheduleInterview(Long recruiterId, ScheduleInterviewRequest request);
    InterviewResponse confirmInterview(Long interviewId, Long candidateId);
    InterviewResponse rescheduleInterview(Long interviewId, RescheduleInterviewRequest request, Long requesterId);
    InterviewResponse cancelInterview(Long interviewId, Long requesterId, String reason);
    InterviewResponse completeInterview(Long interviewId, Long recruiterId);
    InterviewResponse getInterviewById(Long interviewId);
    List<InterviewResponse> getInterviewsByApplication(Long applicationId);
    Page<InterviewResponse> getInterviewsByCandidate(Long candidateId, Pageable pageable);
    List<InterviewResponse> getUpcomingInterviewsByCandidate(Long candidateId);
    Page<InterviewResponse> getInterviewsByRecruiter(Long recruiterId, Pageable pageable);
    List<InterviewResponse> getUpcomingInterviewsByRecruiter(Long recruiterId);
    Page<InterviewResponse> getInterviewsByRecruiterAndStatus(Long recruiterId, InterviewStatus status, Pageable pageable);
    InterviewResponse requestReschedule(Long interviewId, Long candidateId, RescheduleInterviewRequest request);
    InterviewResponse rejectReschedule(Long interviewId, Long recruiterId, String reason);
}
