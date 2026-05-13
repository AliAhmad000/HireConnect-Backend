package com.hireconnect.profile.controller;

import com.hireconnect.profile.dto.request.CandidateProfileRequest;
import com.hireconnect.profile.dto.request.RecruiterProfileRequest;
import com.hireconnect.profile.dto.response.ApiResponse;
import com.hireconnect.profile.dto.response.CandidateNotificationRecipientResponse;
import com.hireconnect.profile.dto.response.CandidateProfileResponse;
import com.hireconnect.profile.dto.response.RecruiterProfileResponse;
import com.hireconnect.profile.dto.response.ResumeUploadResponse;
import com.hireconnect.profile.service.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/profiles")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Profiles", description = "Candidate and Recruiter profile management")
@SecurityRequirement(name = "bearerAuth")
public class ProfileController {

    private final ProfileService profileService;

    // ─── Candidate Endpoints ──────────────────────────────────────────────────

    @Operation(summary = "Create candidate profile", description = "Creates a new profile for a registered candidate.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Profile created"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Profile already exists")
    })
    @PostMapping("/candidate")
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<ApiResponse<CandidateProfileResponse>> createCandidateProfile(
            @Valid @RequestBody CandidateProfileRequest request,
            @Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId) {
        log.info("POST /profiles/candidate - userId={}", userId);
        CandidateProfileResponse response = profileService.createCandidateProfile(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Candidate profile created successfully", response));
    }

    @Operation(summary = "Get candidate profile by userId")
    @GetMapping("/candidate/{userId}")
    @PreAuthorize("hasAnyRole('CANDIDATE', 'RECRUITER', 'ADMIN')")
    public ResponseEntity<ApiResponse<CandidateProfileResponse>> getCandidateProfile(
            @PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.success(profileService.getCandidateProfile(userId)));
    }

    @Operation(summary = "Get own candidate profile (current user)")
    @GetMapping("/candidate/me")
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<ApiResponse<CandidateProfileResponse>> getMyProfile(
            @Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(ApiResponse.success(profileService.getCandidateProfile(userId)));
    }

    @Operation(summary = "Update candidate profile")
    @PutMapping("/candidate")
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<ApiResponse<CandidateProfileResponse>> updateCandidateProfile(
            @Valid @RequestBody CandidateProfileRequest request,
            @Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId) {
        CandidateProfileResponse response = profileService.updateCandidateProfile(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Candidate profile updated successfully", response));
    }

    @Operation(summary = "Upload candidate resume")
    @PostMapping(value = "/candidate/resume", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<ApiResponse<ResumeUploadResponse>> uploadCandidateResume(
            @RequestPart("resume") MultipartFile resume,
            @Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId) {
        ResumeUploadResponse response = profileService.uploadCandidateResume(userId, resume);
        return ResponseEntity.ok(ApiResponse.success("Resume uploaded successfully", response));
    }

    @Operation(summary = "Delete candidate profile")
    @DeleteMapping("/candidate")
    @PreAuthorize("hasAnyRole('CANDIDATE', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteCandidateProfile(
            @Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId) {
        profileService.deleteCandidateProfile(userId);
        return ResponseEntity.ok(ApiResponse.success("Candidate profile deleted successfully", null));
    }

    @Operation(summary = "Get candidate resume URL (internal use by application-service)")
    @GetMapping("/candidate/{userId}/resume-url")
    public ResponseEntity<ApiResponse<String>> getCandidateResumeUrl(@PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.success(profileService.getCandidateResumeUrl(userId)));
    }

    @Operation(summary = "Check if candidate profile exists")
    @GetMapping("/candidate/{userId}/exists")
    public ResponseEntity<ApiResponse<Boolean>> candidateExists(@PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.success(profileService.candidateProfileExists(userId)));
    }

    @Operation(summary = "List candidate notification recipients (internal)")
    @GetMapping("/internal/candidates/notification-recipients")
    public ResponseEntity<ApiResponse<List<CandidateNotificationRecipientResponse>>> getCandidateNotificationRecipients() {
        return ResponseEntity.ok(ApiResponse.success(profileService.getCandidateNotificationRecipients()));
    }

    // ─── Recruiter Endpoints ──────────────────────────────────────────────────

    @Operation(summary = "Create recruiter profile", description = "Creates a new company profile for a recruiter.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Profile created"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Profile already exists")
    })
    @PostMapping("/recruiter")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<ApiResponse<RecruiterProfileResponse>> createRecruiterProfile(
            @Valid @RequestBody RecruiterProfileRequest request,
            @Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId) {
        log.info("POST /profiles/recruiter - userId={}", userId);
        RecruiterProfileResponse response = profileService.createRecruiterProfile(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Recruiter profile created successfully", response));
    }

    @Operation(summary = "Get recruiter profile by userId")
    @GetMapping("/recruiter/{userId}")
    @PreAuthorize("hasAnyRole('CANDIDATE', 'RECRUITER', 'ADMIN')")
    public ResponseEntity<ApiResponse<RecruiterProfileResponse>> getRecruiterProfile(
            @PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.success(profileService.getRecruiterProfile(userId)));
    }

    @Operation(summary = "Get own recruiter profile (current user)")
    @GetMapping("/recruiter/me")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<ApiResponse<RecruiterProfileResponse>> getMyRecruiterProfile(
            @Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(ApiResponse.success(profileService.getRecruiterProfile(userId)));
    }

    @Operation(summary = "Update recruiter profile")
    @PutMapping("/recruiter")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<ApiResponse<RecruiterProfileResponse>> updateRecruiterProfile(
            @Valid @RequestBody RecruiterProfileRequest request,
            @Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId) {
        RecruiterProfileResponse response = profileService.updateRecruiterProfile(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Recruiter profile updated successfully", response));
    }

    @Operation(summary = "Delete recruiter profile")
    @DeleteMapping("/recruiter")
    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteRecruiterProfile(
            @Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId) {
        profileService.deleteRecruiterProfile(userId);
        return ResponseEntity.ok(ApiResponse.success("Recruiter profile deleted successfully", null));
    }
}
