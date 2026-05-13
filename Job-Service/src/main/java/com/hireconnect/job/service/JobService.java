package com.hireconnect.job.service;

import com.hireconnect.job.dto.request.JobRequest;
import com.hireconnect.job.dto.response.JobResponse;
import com.hireconnect.job.enums.JobStatus;
import com.hireconnect.job.enums.JobType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface JobService {
    JobResponse createJob(Long recruiterId, String companyName, JobRequest request);
    JobResponse getJobById(Long jobId, Long viewerId, String viewerRole);
    Page<JobResponse> getAllActiveJobs(Pageable pageable);
    Page<JobResponse> getAllJobs(Pageable pageable, JobStatus status);
    Page<JobResponse> searchJobs(String title, String location, String category,
                                  JobType jobType, Integer experience,
                                  Double minSalary, Double maxSalary, Pageable pageable);
    Page<JobResponse> getJobsByRecruiter(Long recruiterId, Pageable pageable);
    Page<JobResponse> getJobsByRecruiterAndStatus(Long recruiterId, JobStatus status, Pageable pageable);
    List<JobResponse> getAllJobsByRecruiter(Long recruiterId);
    JobResponse updateJob(Long jobId, Long recruiterId, JobRequest request);
    JobResponse updateJobStatus(Long jobId, Long recruiterId, JobStatus status);
    JobResponse updateJobStatusAsAdmin(Long jobId, JobStatus status);
    void deleteJob(Long jobId, Long recruiterId);
    void deleteJobAsAdmin(Long jobId);
    boolean jobExists(Long jobId);
    Long getRecruiterIdByJob(Long jobId);
    long countJobsByRecruiter(Long recruiterId);
}
