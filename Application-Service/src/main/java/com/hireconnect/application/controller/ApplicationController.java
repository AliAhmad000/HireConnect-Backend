package com.hireconnect.application.controller;

import com.hireconnect.application.dto.request.SubmitApplicationRequest;
import com.hireconnect.application.dto.request.RecruiterMessageRequest;
import org.springframework.security.core.Authentication;
import com.hireconnect.application.dto.request.UpdateStatusRequest;
import com.hireconnect.application.dto.response.ApiResponse;
import com.hireconnect.application.dto.response.ApplicationResponse;
import com.hireconnect.application.enums.ApplicationStatus;
import com.hireconnect.application.service.ApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/applications")
@RequiredArgsConstructor
@Slf4j
public class ApplicationController {

    private final ApplicationService applicationService;
    @GetMapping("/my")
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<ApiResponse<List<ApplicationResponse>>> getMyApplications(
            Authentication authentication) {

        Long candidateId =
                Long.parseLong(authentication.getName());

        List<ApplicationResponse> result =
                applicationService.getAllApplicationsByCandidate(candidateId);

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * Submit a new application — Candidate only
     */
    @PostMapping
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<ApiResponse<ApplicationResponse>> submitApplication(
            @Valid @RequestBody SubmitApplicationRequest request,
            @RequestHeader("X-User-Id") Long candidateId,
            @RequestHeader(value = "X-User-Name", required = false) String candidateName,
            @RequestHeader(value = "X-User-Email", required = false) String candidateEmail) {
        log.info("POST /api/v1/applications - candidateId={}", candidateId);
        ApplicationResponse response = applicationService.submitApplication(
                candidateId, candidateName, candidateEmail, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Application submitted successfully", response));
    }

    /**
     * Get application by ID — Candidate (own) or Recruiter
     */
    @GetMapping("/{applicationId}")
    @PreAuthorize("hasAnyRole('CANDIDATE', 'RECRUITER', 'ADMIN')")
    public ResponseEntity<ApiResponse<ApplicationResponse>> getApplicationById(
            @PathVariable Long applicationId) {
        ApplicationResponse response = applicationService.getApplicationById(applicationId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Get all applications for a candidate (paginated)
     */
    @GetMapping("/candidate/{candidateId}")
    @PreAuthorize("hasAnyRole('CANDIDATE', 'ADMIN')")
    public ResponseEntity<ApiResponse<Page<ApplicationResponse>>> getApplicationsByCandidate(
            @PathVariable Long candidateId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "appliedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<ApplicationResponse> result = applicationService.getApplicationsByCandidate(candidateId, pageable);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * Get all applications for a candidate (list — for dashboard)
     */
    @GetMapping("/candidate/{candidateId}/all")
    @PreAuthorize("hasAnyRole('CANDIDATE', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<ApplicationResponse>>> getAllApplicationsByCandidate(
            @PathVariable Long candidateId) {
        List<ApplicationResponse> result = applicationService.getAllApplicationsByCandidate(candidateId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * Get applications for a job — Recruiter only
     */
    @GetMapping("/job/{jobId}")
    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Page<ApplicationResponse>>> getApplicationsByJob(
            @PathVariable Long jobId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) ApplicationStatus status) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("appliedAt").descending());
        Page<ApplicationResponse> result;
        if (status != null) {
            result = applicationService.getApplicationsByJobAndStatus(jobId, status, pageable);
        } else {
            result = applicationService.getApplicationsByJob(jobId, pageable);
        }
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * Get applications for a recruiter — Recruiter only
     */
    @GetMapping("/recruiter/{recruiterId}")
    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Page<ApplicationResponse>>> getApplicationsByRecruiter(
            @PathVariable Long recruiterId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("appliedAt").descending());
        Page<ApplicationResponse> result = applicationService.getApplicationsByRecruiter(recruiterId, pageable);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * Update application status — Recruiter only
     */
    @PatchMapping("/{applicationId}/status")
    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
    public ResponseEntity<ApiResponse<ApplicationResponse>> updateApplicationStatus(
            @PathVariable Long applicationId,
            @Valid @RequestBody UpdateStatusRequest request,
            @RequestHeader("X-User-Id") Long recruiterId) {
        log.info("PATCH /api/v1/applications/{}/status - recruiter={}", applicationId, recruiterId);
        ApplicationResponse response = applicationService.updateApplicationStatus(applicationId, request, recruiterId);
        return ResponseEntity.ok(ApiResponse.success("Application status updated successfully", response));
    }

    /**
     * Send a portal message to a candidate in shortlisted or later active stages — Recruiter only
     */
    @PostMapping("/{applicationId}/messages")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<ApiResponse<Void>> sendMessageToCandidate(
            @PathVariable Long applicationId,
            @Valid @RequestBody RecruiterMessageRequest request,
            @RequestHeader("X-User-Id") Long recruiterId) {
        log.info("POST /api/v1/applications/{}/messages - recruiter={}", applicationId, recruiterId);
        applicationService.sendMessageToCandidate(applicationId, request, recruiterId);
        return ResponseEntity.ok(ApiResponse.success("Message sent to candidate", null));
    }

    /**
     * Withdraw application — Candidate only
     */
    @DeleteMapping("/{applicationId}/withdraw")
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<ApiResponse<Void>> withdrawApplication(
            @PathVariable Long applicationId,
            @RequestHeader("X-User-Id") Long candidateId) {
        log.info("DELETE /api/v1/applications/{}/withdraw - candidateId={}", applicationId, candidateId);
        applicationService.withdrawApplication(applicationId, candidateId);
        return ResponseEntity.ok(ApiResponse.success("Application withdrawn successfully", null));
    }

    /**
     * Check if candidate has applied for a job
     */
    @GetMapping("/check")
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<ApiResponse<Boolean>> hasApplied(
            @RequestParam Long jobId,
            @RequestParam Long candidateId) {
        boolean applied = applicationService.hasApplied(jobId, candidateId);
        return ResponseEntity.ok(ApiResponse.success(applied));
    }

    /**
     * Get application count for a job
     */
    @GetMapping("/job/{jobId}/count")
    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Long>> countApplicationsByJob(@PathVariable Long jobId) {
        long count = applicationService.countApplicationsByJob(jobId);
        return ResponseEntity.ok(ApiResponse.success(count));
    }
}
