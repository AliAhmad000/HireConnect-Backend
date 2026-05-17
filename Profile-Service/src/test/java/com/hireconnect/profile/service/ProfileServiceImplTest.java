package com.hireconnect.profile.service;

import com.hireconnect.profile.dto.request.AddressRequest;
import com.hireconnect.profile.dto.request.CandidateProfileRequest;
import com.hireconnect.profile.dto.request.RecruiterProfileRequest;
import com.hireconnect.profile.dto.response.CandidateProfileResponse;
import com.hireconnect.profile.dto.response.RecruiterProfileResponse;
import com.hireconnect.profile.entity.Address;
import com.hireconnect.profile.entity.CandidatePreferredLocation;
import com.hireconnect.profile.entity.CandidateProfile;
import com.hireconnect.profile.entity.CandidateSkill;
import com.hireconnect.profile.entity.RecruiterProfile;
import com.hireconnect.profile.exception.ProfileAlreadyExistsException;
import com.hireconnect.profile.exception.ResourceNotFoundException;
import com.hireconnect.profile.repository.CandidateProfileRepository;
import com.hireconnect.profile.repository.RecruiterProfileRepository;
import com.hireconnect.profile.service.impl.ProfileServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProfileServiceImpl Tests")
class ProfileServiceImplTest {

    @Mock private CandidateProfileRepository candidateRepo;
    @Mock private RecruiterProfileRepository recruiterRepo;

    @InjectMocks private ProfileServiceImpl profileService;

    // ─── Fixtures ──────────────────────────────────────────────────────────────

    private CandidateProfile buildCandidateProfile(Long userId) {
        return CandidateProfile.builder()
                .profileId(1L)
                .userId(userId)
                .fullName("Alice Smith")
                .email("alice@example.com")
                .mobile("9876543210")
                .experience(3)

                .skills(List.of(
                        CandidateSkill.builder().skill("Java").build(),
                        CandidateSkill.builder().skill("Spring Boot").build()
                ))

                .resumeUrl("https://s3.example.com/alice-resume.pdf")
                .isOpenToRemote(true)

                .preferredLocations(
                        List.of(
                                CandidatePreferredLocation.builder()
                                        .location("Remote")
                                        .build()
                        )
                )

                .addresses(
                        List.of(
                                Address.builder()
                                        .addressId(10L)
                                        .houseNo("12B")
                                        .street("MG Road")
                                        .city("Bengaluru")
                                        .state("Karnataka")
                                        .country("India")
                                        .pincode(560001)
                                        .addressType("HOME")
                                        .build()
                        )
                )

                .build();
    }

    private RecruiterProfile buildRecruiterProfile(Long userId) {
        return RecruiterProfile.builder()
                .profileId(1L).userId(userId)
                .fullName("Bob Recruiter").email("bob@techcorp.com")
                .companyName("TechCorp").industry("Software").companySize("51-200")
                .website("https://techcorp.com").designation("HR Manager")
                .build();
    }

    private CandidateProfileRequest buildCandidateRequest() {
        CandidateProfileRequest r = new CandidateProfileRequest();
        r.setFullName("Alice Smith"); r.setEmail("alice@example.com");
        r.setMobile("9876543210"); r.setExperience(3);
        r.setSkills(List.of("Java", "Spring Boot"));
        r.setResumeUrl("https://s3.example.com/alice-resume.pdf");
        r.setIsOpenToRemote(true);
        return r;
    }

    private AddressRequest buildAddressRequest() {
        AddressRequest address = new AddressRequest();
        address.setHouseNo("12B");
        address.setStreet("MG Road");
        address.setCity("Bengaluru");
        address.setState("Karnataka");
        address.setPincode(560001);
        return address;
    }

    private RecruiterProfileRequest buildRecruiterRequest() {
        RecruiterProfileRequest r = new RecruiterProfileRequest();
        r.setFullName("Bob Recruiter"); r.setEmail("bob@techcorp.com");
        r.setCompanyName("TechCorp"); r.setIndustry("Software");
        r.setCompanySize("51-200"); r.setWebsite("https://techcorp.com");
        r.setDesignation("HR Manager");
        return r;
    }

    // ─── Candidate Tests ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("createCandidateProfile()")
    class CreateCandidateTests {

        @Test
        @DisplayName("should create profile successfully for new user")
        void shouldCreateProfileSuccessfully() {
            CandidateProfile saved = buildCandidateProfile(1L);
            when(candidateRepo.existsByUserId(1L)).thenReturn(false);
            when(candidateRepo.save(any(CandidateProfile.class))).thenReturn(saved);

            CandidateProfileResponse result = profileService.createCandidateProfile(1L, buildCandidateRequest());

            assertThat(result.getUserId()).isEqualTo(1L);
            assertThat(result.getFullName()).isEqualTo("Alice Smith");
            assertThat(result.getResumeUrl()).isEqualTo("https://s3.example.com/alice-resume.pdf");
            verify(candidateRepo).save(any(CandidateProfile.class));
        }

        @Test
        @DisplayName("should throw ProfileAlreadyExistsException for duplicate userId")
        void shouldThrowForDuplicateProfile() {
            when(candidateRepo.existsByUserId(1L)).thenReturn(true);

            assertThatThrownBy(() -> profileService.createCandidateProfile(1L, buildCandidateRequest()))
                    .isInstanceOf(ProfileAlreadyExistsException.class)
                    .hasMessageContaining("1");

            verify(candidateRepo, never()).save(any());
        }
    }

    @Nested
    @DisplayName("getCandidateProfile()")
    class GetCandidateTests {

        @Test
        @DisplayName("should return profile for existing userId")
        void shouldReturnExistingProfile() {
            CandidateProfile profile = buildCandidateProfile(1L);
            when(candidateRepo.findByUserId(1L)).thenReturn(Optional.of(profile));

            CandidateProfileResponse result = profileService.getCandidateProfile(1L);

            assertThat(result.getEmail()).isEqualTo("alice@example.com");
            assertThat(result.getSkills()).contains("Java", "Spring Boot");
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException for non-existent userId")
        void shouldThrowForNonExistentUser() {
            when(candidateRepo.findByUserId(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> profileService.getCandidateProfile(999L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("999");
        }
    }

    @Test
    @DisplayName("getCandidateResumeUrl() should return resume URL")
    void shouldReturnResumeUrl() {
        CandidateProfile profile = buildCandidateProfile(1L);
        when(candidateRepo.findByUserId(1L)).thenReturn(Optional.of(profile));

        String url = profileService.getCandidateResumeUrl(1L);
        assertThat(url).isEqualTo("https://s3.example.com/alice-resume.pdf");
    }

    @Test
    @DisplayName("candidateProfileExists() should return true when profile exists")
    void shouldReturnTrueWhenExists() {
        when(candidateRepo.existsByUserId(1L)).thenReturn(true);
        assertThat(profileService.candidateProfileExists(1L)).isTrue();
    }

    @Test
    @DisplayName("updateCandidateProfile() should update provided fields and addresses")
    void shouldUpdateCandidateProfile() {
        CandidateProfile profile = buildCandidateProfile(1L);
        CandidateProfileRequest request = buildCandidateRequest();
        request.setFullName("Alice Updated");
        request.setAddresses(List.of(buildAddressRequest()));

        when(candidateRepo.findByUserId(1L)).thenReturn(Optional.of(profile));
        when(candidateRepo.save(profile)).thenReturn(profile);

        CandidateProfileResponse response = profileService.updateCandidateProfile(1L, request);

        assertThat(response.getFullName()).isEqualTo("Alice Updated");
        assertThat(response.getAddresses()).hasSize(1);
        assertThat(response.getAddresses().get(0).getCountry()).isEqualTo("India");
        assertThatCode(() -> profile.getSkills().add(CandidateSkill.builder().skill("SQL").build()))
                .doesNotThrowAnyException();
        assertThatCode(() -> profile.getPreferredLocations().add(
                CandidatePreferredLocation.builder().location("Pune").build()))
                .doesNotThrowAnyException();
        assertThatCode(() -> profile.getAddresses().add(Address.builder().city("Mysuru").state("Karnataka").build()))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("deleteCandidateProfile() should delete existing profile")
    void shouldDeleteCandidateProfile() {
        CandidateProfile profile = buildCandidateProfile(1L);
        when(candidateRepo.findByUserId(1L)).thenReturn(Optional.of(profile));

        profileService.deleteCandidateProfile(1L);

        verify(candidateRepo).delete(profile);
    }

    @Test
    @DisplayName("uploadCandidateResume() should store pdf and update profile")
    void shouldUploadCandidateResume() throws Exception {
        Path uploadDir = Files.createTempDirectory("hireconnect-resumes");
        ReflectionTestUtils.setField(profileService, "resumeUploadDir", uploadDir.toString());
        CandidateProfile profile = buildCandidateProfile(1L);
        MockMultipartFile file = new MockMultipartFile(
                "file", "Alice Resume.pdf", "application/pdf", "pdf-content".getBytes());

        when(candidateRepo.findByUserId(1L)).thenReturn(Optional.of(profile));

        var response = profileService.uploadCandidateResume(1L, file);

        assertThat(response.getResumeUrl()).startsWith("/uploads/resumes/1-");
        assertThat(profile.getResumeUrl()).isEqualTo(response.getResumeUrl());
        verify(candidateRepo).save(profile);
    }

    @Test
    @DisplayName("uploadCandidateResume() should reject non-pdf files")
    void shouldRejectNonPdfResume() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "resume.txt", "text/plain", "text".getBytes());

        assertThatThrownBy(() -> profileService.uploadCandidateResume(1L, file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("PDF");
    }

    @Test
    @DisplayName("getCandidateNotificationRecipients() should return profiles with email")
    void shouldReturnCandidateNotificationRecipients() {
        CandidateProfile withEmail = buildCandidateProfile(1L);
        CandidateProfile withoutEmail = buildCandidateProfile(2L);
        withoutEmail.setEmail(" ");
        when(candidateRepo.findByEmailIsNotNull()).thenReturn(List.of(withEmail, withoutEmail));

        var recipients = profileService.getCandidateNotificationRecipients();

        assertThat(recipients).hasSize(1);
        assertThat(recipients.get(0).getEmail()).isEqualTo("alice@example.com");
    }

    // ─── Recruiter Tests ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("createRecruiterProfile()")
    class CreateRecruiterTests {

        @Test
        @DisplayName("should create recruiter profile successfully")
        void shouldCreateRecruiterProfile() {
            RecruiterProfile saved = buildRecruiterProfile(2L);
            when(recruiterRepo.existsByUserId(2L)).thenReturn(false);
            when(recruiterRepo.save(any(RecruiterProfile.class))).thenReturn(saved);

            RecruiterProfileResponse result = profileService.createRecruiterProfile(2L, buildRecruiterRequest());

            assertThat(result.getUserId()).isEqualTo(2L);
            assertThat(result.getCompanyName()).isEqualTo("TechCorp");
            assertThat(result.getIndustry()).isEqualTo("Software");
            verify(recruiterRepo).save(any(RecruiterProfile.class));
        }

        @Test
        @DisplayName("should throw when recruiter profile already exists")
        void shouldThrowForDuplicateRecruiter() {
            when(recruiterRepo.existsByUserId(2L)).thenReturn(true);

            assertThatThrownBy(() -> profileService.createRecruiterProfile(2L, buildRecruiterRequest()))
                    .isInstanceOf(ProfileAlreadyExistsException.class);

            verify(recruiterRepo, never()).save(any());
        }
    }

    @Nested
    @DisplayName("getRecruiterProfile()")
    class GetRecruiterTests {

        @Test
        @DisplayName("should return recruiter profile")
        void shouldReturnRecruiterProfile() {
            RecruiterProfile profile = buildRecruiterProfile(2L);
            when(recruiterRepo.findByUserId(2L)).thenReturn(Optional.of(profile));

            RecruiterProfileResponse result = profileService.getRecruiterProfile(2L);
            assertThat(result.getCompanyName()).isEqualTo("TechCorp");
            assertThat(result.getDesignation()).isEqualTo("HR Manager");
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException for non-existent recruiter")
        void shouldThrowForNonExistentRecruiter() {
            when(recruiterRepo.findByUserId(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> profileService.getRecruiterProfile(999L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Test
    @DisplayName("deleteRecruiterProfile() should call repository delete")
    void shouldDeleteRecruiterProfile() {
        RecruiterProfile profile = buildRecruiterProfile(2L);
        when(recruiterRepo.findByUserId(2L)).thenReturn(Optional.of(profile));

        assertThatCode(() -> profileService.deleteRecruiterProfile(2L)).doesNotThrowAnyException();
        verify(recruiterRepo).delete(profile);
    }

    @Test
    @DisplayName("updateRecruiterProfile() should update provided fields")
    void shouldUpdateRecruiterProfile() {
        RecruiterProfile profile = buildRecruiterProfile(2L);
        RecruiterProfileRequest request = buildRecruiterRequest();
        request.setCompanyName("NewTech");
        request.setDesignation("Talent Lead");

        when(recruiterRepo.findByUserId(2L)).thenReturn(Optional.of(profile));
        when(recruiterRepo.save(profile)).thenReturn(profile);

        RecruiterProfileResponse response = profileService.updateRecruiterProfile(2L, request);

        assertThat(response.getCompanyName()).isEqualTo("NewTech");
        assertThat(response.getDesignation()).isEqualTo("Talent Lead");
    }
}
