package com.hireconnect.profile.service.impl;

import com.hireconnect.profile.dto.request.CandidateProfileRequest;
import com.hireconnect.profile.dto.request.RecruiterProfileRequest;
import com.hireconnect.profile.dto.response.AddressResponse;
import com.hireconnect.profile.dto.response.CandidateNotificationRecipientResponse;
import com.hireconnect.profile.dto.response.CandidateProfileResponse;
import com.hireconnect.profile.dto.response.RecruiterProfileResponse;
import com.hireconnect.profile.dto.response.ResumeUploadResponse;
import com.hireconnect.profile.entity.Address;
import com.hireconnect.profile.entity.CandidateProfile;
import com.hireconnect.profile.entity.RecruiterProfile;
import com.hireconnect.profile.exception.ProfileAlreadyExistsException;
import com.hireconnect.profile.exception.ResourceNotFoundException;
import com.hireconnect.profile.repository.CandidateProfileRepository;
import com.hireconnect.profile.repository.RecruiterProfileRepository;
import com.hireconnect.profile.service.ProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ProfileServiceImpl implements ProfileService {

    private final CandidateProfileRepository candidateRepo;
    private final RecruiterProfileRepository recruiterRepo;

    @Value("${app.upload.resume-dir:uploads/resumes}")
    private String resumeUploadDir;

    // ─── Candidate ────────────────────────────────────────────────────────────

    @Override
    public CandidateProfileResponse createCandidateProfile(Long userId, CandidateProfileRequest request) {
        if (candidateRepo.existsByUserId(userId)) {
            throw new ProfileAlreadyExistsException(userId);
        }
        CandidateProfile profile = CandidateProfile.builder()
                .userId(userId)
                .fullName(request.getFullName())
                .email(request.getEmail())
                .mobile(request.getMobile())
                .dob(request.getDob())
                .gender(request.getGender())
                .skills(request.getSkills() != null ? request.getSkills() : java.util.List.of())
                .experience(request.getExperience())
                .resumeUrl(request.getResumeUrl())
                .linkedinUrl(request.getLinkedinUrl())
                .githubUrl(request.getGithubUrl())
                .portfolioUrl(request.getPortfolioUrl())
                .summary(request.getSummary())
                .currentCompany(request.getCurrentCompany())
                .currentDesignation(request.getCurrentDesignation())
                .expectedSalary(request.getExpectedSalary())
                .noticePeriodDays(request.getNoticePeriodDays())
                .isOpenToRemote(request.getIsOpenToRemote() != null ? request.getIsOpenToRemote() : false)
                .preferredLocations(request.getPreferredLocations() != null ? request.getPreferredLocations() : java.util.List.of())
                .addresses(toAddresses(request))
                .build();
        CandidateProfile saved = candidateRepo.save(profile);
        log.info("Candidate profile created for userId={}", userId);
        return toCandidateResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public CandidateProfileResponse getCandidateProfile(Long userId) {
        return toCandidateResponse(findCandidateByUserId(userId));
    }

    @Override
    public CandidateProfileResponse updateCandidateProfile(Long userId, CandidateProfileRequest request) {
        CandidateProfile profile = findCandidateByUserId(userId);
        if (request.getFullName() != null)           profile.setFullName(request.getFullName());
        if (request.getEmail() != null)              profile.setEmail(request.getEmail());
        if (request.getMobile() != null)             profile.setMobile(request.getMobile());
        if (request.getDob() != null)                profile.setDob(request.getDob());
        if (request.getGender() != null)             profile.setGender(request.getGender());
        if (request.getSkills() != null)             profile.setSkills(request.getSkills());
        if (request.getExperience() != null)         profile.setExperience(request.getExperience());
        if (request.getResumeUrl() != null)          profile.setResumeUrl(request.getResumeUrl());
        if (request.getLinkedinUrl() != null)        profile.setLinkedinUrl(request.getLinkedinUrl());
        if (request.getGithubUrl() != null)          profile.setGithubUrl(request.getGithubUrl());
        if (request.getPortfolioUrl() != null)       profile.setPortfolioUrl(request.getPortfolioUrl());
        if (request.getSummary() != null)            profile.setSummary(request.getSummary());
        if (request.getCurrentCompany() != null)     profile.setCurrentCompany(request.getCurrentCompany());
        if (request.getCurrentDesignation() != null) profile.setCurrentDesignation(request.getCurrentDesignation());
        if (request.getExpectedSalary() != null)     profile.setExpectedSalary(request.getExpectedSalary());
        if (request.getNoticePeriodDays() != null)   profile.setNoticePeriodDays(request.getNoticePeriodDays());
        if (request.getIsOpenToRemote() != null)     profile.setIsOpenToRemote(request.getIsOpenToRemote());
        if (request.getPreferredLocations() != null) profile.setPreferredLocations(request.getPreferredLocations());
        if (request.getAddresses() != null) {
            if (profile.getAddresses() == null) {
                profile.setAddresses(new java.util.ArrayList<>());
            }
            profile.getAddresses().clear();
            profile.getAddresses().addAll(toAddresses(request));
        }
        return toCandidateResponse(candidateRepo.save(profile));
    }

    @Override
    public void deleteCandidateProfile(Long userId) {
        CandidateProfile profile = findCandidateByUserId(userId);
        candidateRepo.delete(profile);
        log.info("Candidate profile deleted for userId={}", userId);
    }

    @Override
    @Transactional(readOnly = true)
    public String getCandidateResumeUrl(Long userId) {
        return findCandidateByUserId(userId).getResumeUrl();
    }

    @Override
    public ResumeUploadResponse uploadCandidateResume(Long userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Resume file is required");
        }

        String originalName = StringUtils.cleanPath(file.getOriginalFilename() != null ? file.getOriginalFilename() : "resume.pdf");
        if (!originalName.toLowerCase().endsWith(".pdf")) {
            throw new IllegalArgumentException("Only PDF resumes are supported");
        }

        CandidateProfile profile = findCandidateByUserId(userId);
        String fileName = userId + "-" + System.currentTimeMillis() + "-" + originalName.replaceAll("[^a-zA-Z0-9._-]", "_");

        try {
            Path uploadPath = Path.of(resumeUploadDir).toAbsolutePath().normalize();
            Files.createDirectories(uploadPath);
            Files.copy(file.getInputStream(), uploadPath.resolve(fileName));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to store resume", e);
        }

        String resumeUrl = "/uploads/resumes/" + fileName;
        profile.setResumeUrl(resumeUrl);
        candidateRepo.save(profile);
        return ResumeUploadResponse.builder().resumeUrl(resumeUrl).build();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean candidateProfileExists(Long userId) {
        return candidateRepo.existsByUserId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CandidateNotificationRecipientResponse> getCandidateNotificationRecipients() {
        return candidateRepo.findByEmailIsNotNull().stream()
                .filter(profile -> profile.getEmail() != null && !profile.getEmail().isBlank())
                .map(profile -> new CandidateNotificationRecipientResponse(
                        profile.getUserId(),
                        profile.getFullName(),
                        profile.getEmail()))
                .toList();
    }

    // ─── Recruiter ────────────────────────────────────────────────────────────

    @Override
    public RecruiterProfileResponse createRecruiterProfile(Long userId, RecruiterProfileRequest request) {
        if (recruiterRepo.existsByUserId(userId)) {
            throw new ProfileAlreadyExistsException(userId);
        }
        RecruiterProfile profile = RecruiterProfile.builder()
                .userId(userId)
                .fullName(request.getFullName())
                .email(request.getEmail())
                .mobile(request.getMobile())
                .companyName(request.getCompanyName())
                .companySize(request.getCompanySize())
                .industry(request.getIndustry())
                .website(request.getWebsite())
                .companyDescription(request.getCompanyDescription())
                .linkedinUrl(request.getLinkedinUrl())
                .logoUrl(request.getLogoUrl())
                .designation(request.getDesignation())
                .build();
        RecruiterProfile saved = recruiterRepo.save(profile);
        log.info("Recruiter profile created for userId={}", userId);
        return toRecruiterResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public RecruiterProfileResponse getRecruiterProfile(Long userId) {
        return toRecruiterResponse(findRecruiterByUserId(userId));
    }

    @Override
    public RecruiterProfileResponse updateRecruiterProfile(Long userId, RecruiterProfileRequest request) {
        RecruiterProfile profile = findRecruiterByUserId(userId);
        if (request.getFullName() != null)          profile.setFullName(request.getFullName());
        if (request.getEmail() != null)             profile.setEmail(request.getEmail());
        if (request.getMobile() != null)            profile.setMobile(request.getMobile());
        if (request.getCompanyName() != null)       profile.setCompanyName(request.getCompanyName());
        if (request.getCompanySize() != null)       profile.setCompanySize(request.getCompanySize());
        if (request.getIndustry() != null)          profile.setIndustry(request.getIndustry());
        if (request.getWebsite() != null)           profile.setWebsite(request.getWebsite());
        if (request.getCompanyDescription() != null)profile.setCompanyDescription(request.getCompanyDescription());
        if (request.getLinkedinUrl() != null)       profile.setLinkedinUrl(request.getLinkedinUrl());
        if (request.getLogoUrl() != null)           profile.setLogoUrl(request.getLogoUrl());
        if (request.getDesignation() != null)       profile.setDesignation(request.getDesignation());
        return toRecruiterResponse(recruiterRepo.save(profile));
    }

    @Override
    public void deleteRecruiterProfile(Long userId) {
        RecruiterProfile profile = findRecruiterByUserId(userId);
        recruiterRepo.delete(profile);
        log.info("Recruiter profile deleted for userId={}", userId);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private CandidateProfile findCandidateByUserId(Long userId) {
        return candidateRepo.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate profile not found for userId: " + userId));
    }

    private RecruiterProfile findRecruiterByUserId(Long userId) {
        return recruiterRepo.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Recruiter profile not found for userId: " + userId));
    }

    private List<Address> toAddresses(CandidateProfileRequest request) {
        if (request.getAddresses() == null) {
            return java.util.List.of();
        }

        return request.getAddresses().stream()
                .filter(a -> a.getCity() != null && !a.getCity().isBlank()
                        && a.getState() != null && !a.getState().isBlank())
                .map(a -> Address.builder()
                        .houseNo(a.getHouseNo())
                        .street(a.getStreet())
                        .city(a.getCity())
                        .state(a.getState())
                        .country(a.getCountry() != null && !a.getCountry().isBlank() ? a.getCountry() : "India")
                        .pincode(a.getPincode())
                        .addressType(a.getAddressType() != null && !a.getAddressType().isBlank() ? a.getAddressType() : "HOME")
                        .build())
                .toList();
    }

    private AddressResponse toAddressResponse(Address address) {
        return AddressResponse.builder()
                .addressId(address.getAddressId())
                .houseNo(address.getHouseNo())
                .street(address.getStreet())
                .city(address.getCity())
                .state(address.getState())
                .country(address.getCountry())
                .pincode(address.getPincode())
                .addressType(address.getAddressType())
                .build();
    }

    private CandidateProfileResponse toCandidateResponse(CandidateProfile p) {
        return CandidateProfileResponse.builder()
                .profileId(p.getProfileId()).userId(p.getUserId())
                .fullName(p.getFullName()).email(p.getEmail()).mobile(p.getMobile())
                .dob(p.getDob()).gender(p.getGender()).skills(p.getSkills())
                .experience(p.getExperience()).resumeUrl(p.getResumeUrl())
                .linkedinUrl(p.getLinkedinUrl()).githubUrl(p.getGithubUrl())
                .portfolioUrl(p.getPortfolioUrl()).summary(p.getSummary())
                .currentCompany(p.getCurrentCompany()).currentDesignation(p.getCurrentDesignation())
                .expectedSalary(p.getExpectedSalary()).noticePeriodDays(p.getNoticePeriodDays())
                .isOpenToRemote(p.getIsOpenToRemote()).preferredLocations(p.getPreferredLocations())
                .addresses(p.getAddresses() != null
                        ? p.getAddresses().stream().map(this::toAddressResponse).toList()
                        : java.util.List.of())
                .createdAt(p.getCreatedAt()).updatedAt(p.getUpdatedAt())
                .build();
    }

    private RecruiterProfileResponse toRecruiterResponse(RecruiterProfile p) {
        return RecruiterProfileResponse.builder()
                .profileId(p.getProfileId()).userId(p.getUserId())
                .fullName(p.getFullName()).email(p.getEmail()).mobile(p.getMobile())
                .companyName(p.getCompanyName()).companySize(p.getCompanySize())
                .industry(p.getIndustry()).website(p.getWebsite())
                .companyDescription(p.getCompanyDescription()).linkedinUrl(p.getLinkedinUrl())
                .logoUrl(p.getLogoUrl()).designation(p.getDesignation())
                .createdAt(p.getCreatedAt()).updatedAt(p.getUpdatedAt())
                .build();
    }
}
