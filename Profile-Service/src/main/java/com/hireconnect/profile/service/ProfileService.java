package com.hireconnect.profile.service;

import com.hireconnect.profile.dto.request.CandidateProfileRequest;
import com.hireconnect.profile.dto.request.RecruiterProfileRequest;
import com.hireconnect.profile.dto.response.CandidateProfileResponse;
import com.hireconnect.profile.dto.response.CandidateNotificationRecipientResponse;
import com.hireconnect.profile.dto.response.ResumeUploadResponse;
import com.hireconnect.profile.dto.response.RecruiterProfileResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ProfileService {

    // Candidate
    CandidateProfileResponse createCandidateProfile(Long userId, CandidateProfileRequest request);
    CandidateProfileResponse getCandidateProfile(Long userId);
    CandidateProfileResponse updateCandidateProfile(Long userId, CandidateProfileRequest request);
    void deleteCandidateProfile(Long userId);
    String getCandidateResumeUrl(Long userId);
    ResumeUploadResponse uploadCandidateResume(Long userId, MultipartFile file);
    boolean candidateProfileExists(Long userId);
    List<CandidateNotificationRecipientResponse> getCandidateNotificationRecipients();

    // Recruiter
    RecruiterProfileResponse createRecruiterProfile(Long userId, RecruiterProfileRequest request);
    RecruiterProfileResponse getRecruiterProfile(Long userId);
    RecruiterProfileResponse updateRecruiterProfile(Long userId, RecruiterProfileRequest request);
    void deleteRecruiterProfile(Long userId);
}
