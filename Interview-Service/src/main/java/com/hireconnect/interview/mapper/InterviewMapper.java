package com.hireconnect.interview.mapper;

import com.hireconnect.interview.dto.response.InterviewResponse;
import com.hireconnect.interview.entity.Interview;
import org.springframework.stereotype.Component;

@Component
public class InterviewMapper {

    public InterviewResponse toResponse(Interview interview) {
        if (interview == null) {
            return null;
        }

        return InterviewResponse.builder()
                .interviewId(interview.getInterviewId())
                .applicationId(interview.getApplicationId())
                .candidateId(interview.getCandidateId())
                .recruiterId(interview.getRecruiterId())
                .jobId(interview.getJobId())
                .jobTitle(interview.getJobTitle())
                .candidateName(interview.getCandidateName())
                .candidateEmail(interview.getCandidateEmail())
                .companyName(interview.getCompanyName())
                .scheduledAt(interview.getScheduledAt())
                .durationMinutes(interview.getDurationMinutes())
                .mode(interview.getMode())
                .meetLink(interview.getMeetLink())
                .location(interview.getLocation())
                .status(interview.getStatus())
                .notes(interview.getNotes())
                .cancellationReason(interview.getCancellationReason())
                .rescheduleReason(interview.getRescheduleReason())
                .interviewerName(interview.getInterviewerName())
                .roundNumber(interview.getRoundNumber())
                .createdAt(interview.getCreatedAt())
                .updatedAt(interview.getUpdatedAt())
                .build();
    }
}
