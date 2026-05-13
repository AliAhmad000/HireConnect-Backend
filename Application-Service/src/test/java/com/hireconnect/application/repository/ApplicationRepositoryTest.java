package com.hireconnect.application.repository;

import com.hireconnect.application.entity.Application;
import com.hireconnect.application.enums.ApplicationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("ApplicationRepository Tests")
class ApplicationRepositoryTest {

    @Autowired
    private ApplicationRepository applicationRepository;

    private Application savedApp;

    @BeforeEach
    void setUp() {
        applicationRepository.deleteAll();
        savedApp = applicationRepository.save(Application.builder()
                .jobId(10L)
                .candidateId(1L)
                .recruiterId(2L)
                .jobTitle("Backend Developer")
                .companyName("TechCorp")
                .candidateName("Alice Smith")
                .candidateEmail("alice@example.com")
                .resumeUrl("https://s3.example.com/resume.pdf")
                .status(ApplicationStatus.APPLIED)
                .appliedAt(LocalDate.now())
                .isWithdrawn(false)
                .build());
    }

    @Test
    @DisplayName("existsByJobIdAndCandidateId() should return true for existing application")
    void shouldReturnTrueForExistingApplication() {
        boolean exists = applicationRepository.existsByJobIdAndCandidateId(10L, 1L);
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("existsByJobIdAndCandidateId() should return false for non-existing combination")
    void shouldReturnFalseForNonExistingCombination() {
        boolean exists = applicationRepository.existsByJobIdAndCandidateId(10L, 99L);
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("findByJobIdAndCandidateId() should return correct application")
    void shouldFindByJobIdAndCandidateId() {
        Optional<Application> found = applicationRepository.findByJobIdAndCandidateId(10L, 1L);
        assertThat(found).isPresent();
        assertThat(found.get().getCandidateName()).isEqualTo("Alice Smith");
    }

    @Test
    @DisplayName("findByCandidateIdOrderByAppliedAtDesc() should return candidate's applications")
    void shouldReturnApplicationsForCandidate() {
        List<Application> apps = applicationRepository.findByCandidateIdOrderByAppliedAtDesc(1L);
        assertThat(apps).hasSize(1);
        assertThat(apps.get(0).getJobTitle()).isEqualTo("Backend Developer");
    }

    @Test
    @DisplayName("countByJobId() should return correct count")
    void shouldCountApplicationsByJob() {
        // Add another application for same job
        applicationRepository.save(Application.builder()
                .jobId(10L)
                .candidateId(2L)
                .recruiterId(2L)
                .jobTitle("Backend Developer")
                .companyName("TechCorp")
                .candidateName("Bob Jones")
                .candidateEmail("bob@example.com")
                .resumeUrl("https://s3.example.com/resume2.pdf")
                .status(ApplicationStatus.APPLIED)
                .appliedAt(LocalDate.now())
                .isWithdrawn(false)
                .build());

        long count = applicationRepository.countByJobId(10L);
        assertThat(count).isEqualTo(2L);
    }

    @Test
    @DisplayName("findByStatus() should return applications with given status")
    void shouldFindByStatus() {
        List<Application> applied = applicationRepository.findByStatus(ApplicationStatus.APPLIED);
        assertThat(applied).hasSize(1);

        List<Application> shortlisted = applicationRepository.findByStatus(ApplicationStatus.SHORTLISTED);
        assertThat(shortlisted).isEmpty();
    }

    @Test
    @DisplayName("findActiveApplicationsByCandidate() should exclude withdrawn applications")
    void shouldExcludeWithdrawnApplications() {
        // Withdraw the existing app
        savedApp.setIsWithdrawn(true);
        applicationRepository.save(savedApp);

        List<Application> active = applicationRepository.findActiveApplicationsByCandidate(1L);
        assertThat(active).isEmpty();
    }
}
