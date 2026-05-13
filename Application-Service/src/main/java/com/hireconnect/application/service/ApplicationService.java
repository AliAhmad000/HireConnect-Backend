package com.hireconnect.application.service;

import com.hireconnect.application.dto.request.SubmitApplicationRequest;
import com.hireconnect.application.dto.request.RecruiterMessageRequest;
import com.hireconnect.application.dto.request.UpdateStatusRequest;
import com.hireconnect.application.dto.response.ApplicationResponse;
import com.hireconnect.application.enums.ApplicationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ApplicationService {

    ApplicationResponse submitApplication(Long candidateId, String candidateName,
                                          String candidateEmail, SubmitApplicationRequest request);

    ApplicationResponse getApplicationById(Long applicationId);

    Page<ApplicationResponse> getApplicationsByCandidate(Long candidateId, Pageable pageable);

    List<ApplicationResponse> getAllApplicationsByCandidate(Long candidateId);

    Page<ApplicationResponse> getApplicationsByJob(Long jobId, Pageable pageable);

    Page<ApplicationResponse> getApplicationsByJobAndStatus(Long jobId, ApplicationStatus status, Pageable pageable);

    Page<ApplicationResponse> getApplicationsByRecruiter(Long recruiterId, Pageable pageable);

    ApplicationResponse updateApplicationStatus(Long applicationId, UpdateStatusRequest request, Long recruiterId);

    void sendMessageToCandidate(Long applicationId, RecruiterMessageRequest request, Long recruiterId);

    void withdrawApplication(Long applicationId, Long candidateId);

    boolean hasApplied(Long jobId, Long candidateId);

    long countApplicationsByJob(Long jobId);

    long countApplicationsByCandidate(Long candidateId);
}
