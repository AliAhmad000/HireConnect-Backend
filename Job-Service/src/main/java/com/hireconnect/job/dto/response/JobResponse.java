package com.hireconnect.job.dto.response;

import com.hireconnect.job.enums.JobStatus;
import com.hireconnect.job.enums.JobType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class JobResponse {
    private Long jobId;
    private String title;
    private String category;
    private JobType jobType;
    private String location;
    private Double salaryMin;
    private Double salaryMax;
    private String description;
    private List<String> skills;
    private Integer experienceRequired;
    private Integer vacancies;
    private Long postedBy;
    private String companyName;
    private JobStatus status;
    private LocalDate postedAt;
    private LocalDate expiresAt;
    private Boolean isRemote;
    private Long viewCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
