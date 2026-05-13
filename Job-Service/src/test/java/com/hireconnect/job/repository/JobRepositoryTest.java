package com.hireconnect.job.repository;

import com.hireconnect.job.entity.Job;
import com.hireconnect.job.enums.JobStatus;
import com.hireconnect.job.enums.JobType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("JobRepository Tests")
class JobRepositoryTest {

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private TestEntityManager entityManager;

    @BeforeEach
    void setUp() {
        jobRepository.deleteAll();

        // Recruiter 1 - 2 jobs
        jobRepository.save(Job.builder()
                .title("Java Backend Developer").category("Engineering")
                .jobType(JobType.FULL_TIME).location("Bangalore")
                .salaryMin(80000.0).salaryMax(120000.0)
                .description("Looking for experienced Java developer with Spring Boot.")
                .skills(List.of("Java", "Spring Boot")).experienceRequired(3)
                .vacancies(1).postedBy(1L).companyName("TechCorp")
                .status(JobStatus.ACTIVE).isRemote(false).viewCount(0L).build());

        jobRepository.save(Job.builder()
                .title("React Frontend Developer").category("Engineering")
                .jobType(JobType.REMOTE).location("Remote")
                .salaryMin(70000.0).salaryMax(100000.0)
                .description("Looking for experienced React developer with TypeScript skills.")
                .skills(List.of("React", "TypeScript")).experienceRequired(2)
                .vacancies(2).postedBy(1L).companyName("TechCorp")
                .status(JobStatus.ACTIVE).isRemote(true).viewCount(0L).build());

        // Recruiter 2 - 1 paused job
        jobRepository.save(Job.builder()
                .title("Data Scientist").category("Data Science")
                .jobType(JobType.FULL_TIME).location("Mumbai")
                .salaryMin(100000.0).salaryMax(160000.0)
                .description("Looking for data scientist with ML experience and Python skills.")
                .skills(List.of("Python", "ML")).experienceRequired(4)
                .vacancies(1).postedBy(2L).companyName("DataCorp")
                .status(JobStatus.PAUSED).isRemote(false).viewCount(0L).build());
    }

    @Test
    @DisplayName("findByStatus(ACTIVE) should return only active jobs")
    void shouldReturnOnlyActiveJobs() {
        Page<Job> active = jobRepository.findByStatus(
                JobStatus.ACTIVE, PageRequest.of(0, 10));
        assertThat(active.getContent()).hasSize(2);
        assertThat(active.getContent()).allMatch(j -> j.getStatus() == JobStatus.ACTIVE);
    }

    @Test
    @DisplayName("findByPostedBy() should return jobs for specific recruiter")
    void shouldReturnJobsForRecruiter() {
        Page<Job> jobs = jobRepository.findByPostedBy(1L, PageRequest.of(0, 10));
        assertThat(jobs.getContent()).hasSize(2);
        assertThat(jobs.getContent()).allMatch(j -> j.getPostedBy().equals(1L));
    }

    @Test
    @DisplayName("searchJobs() should filter by title keyword")
    void shouldFilterByTitle() {
        Page<Job> result = jobRepository.searchJobs(
                "Java", null, null, null, null, null, null,
                PageRequest.of(0, 10, Sort.by("postedAt").descending()));
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).contains("Java");
    }

    @Test
    @DisplayName("searchJobs() should filter by salary range")
    void shouldFilterBySalaryRange() {
        Page<Job> result = jobRepository.searchJobs(
                null, null, null, null, null, 75000.0, 130000.0,
                PageRequest.of(0, 10, Sort.by("postedAt").descending()));
        // Query filters jobs fully inside the requested salary range.
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).contains("Java");
    }

    @Test
    @DisplayName("searchJobs() should filter by job type")
    void shouldFilterByJobType() {
        Page<Job> result = jobRepository.searchJobs(
                null, null, null, JobType.REMOTE, null, null, null,
                PageRequest.of(0, 10, Sort.by("postedAt").descending()));
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getJobType()).isEqualTo(JobType.REMOTE);
    }

    @Test
    @DisplayName("searchJobs() should filter by location")
    void shouldFilterByLocation() {
        Page<Job> result = jobRepository.searchJobs(
                null, "Bangalore", null, null, null, null, null,
                PageRequest.of(0, 10, Sort.by("postedAt").descending()));
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getLocation()).isEqualTo("Bangalore");
    }

    @Test
    @DisplayName("incrementViewCount() should increment by 1")
    void shouldIncrementViewCount() {
        Job job = jobRepository.findAll().get(0);
        Long jobId = job.getJobId();
        long initialCount = job.getViewCount();

        jobRepository.incrementViewCount(jobId);
        entityManager.flush();
        entityManager.clear();

        Job updated = jobRepository.findById(jobId).orElseThrow();
        assertThat(updated.getViewCount()).isEqualTo(initialCount + 1);
    }

    @Test
    @DisplayName("countByPostedBy() should return correct count")
    void shouldCountByPostedBy() {
        assertThat(jobRepository.countByPostedBy(1L)).isEqualTo(2L);
        assertThat(jobRepository.countByPostedBy(2L)).isEqualTo(1L);
        assertThat(jobRepository.countByPostedBy(99L)).isEqualTo(0L);
    }

    @Test
    @DisplayName("existsByJobIdAndPostedBy() should return true for correct owner")
    void shouldReturnTrueForCorrectOwner() {
        Job job = jobRepository.findAll().get(0);
        assertThat(jobRepository.existsByJobIdAndPostedBy(job.getJobId(), job.getPostedBy())).isTrue();
        assertThat(jobRepository.existsByJobIdAndPostedBy(job.getJobId(), 999L)).isFalse();
    }
}
