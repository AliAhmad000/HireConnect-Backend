package com.hireconnect.job.repository;

import com.hireconnect.job.entity.Job;
import com.hireconnect.job.enums.JobStatus;
import com.hireconnect.job.enums.JobType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JobRepository extends JpaRepository<Job, Long> {

    Page<Job> findByStatus(JobStatus status, Pageable pageable);

    Page<Job> findByPostedBy(Long postedBy, Pageable pageable);

    List<Job> findByPostedByOrderByCreatedAtDesc(Long postedBy);

    Page<Job> findByPostedByAndStatus(Long postedBy, JobStatus status, Pageable pageable);

    @Query("""
            SELECT j FROM Job j WHERE j.status = 'ACTIVE'
              AND (:title    IS NULL OR LOWER(j.title)    LIKE LOWER(CONCAT('%', :title,    '%')))
              AND (:location IS NULL OR LOWER(j.location) LIKE LOWER(CONCAT('%', :location, '%')))
              AND (:category IS NULL OR LOWER(j.category) LIKE LOWER(CONCAT('%', :category, '%')))
              AND (:jobType  IS NULL OR j.jobType = :jobType)
              AND (:minExp   IS NULL OR j.experienceRequired <= :minExp)
              AND (:minSal   IS NULL OR j.salaryMin >= :minSal)
              AND (:maxSal   IS NULL OR j.salaryMax <= :maxSal)
            """)
    Page<Job> searchJobs(
            @Param("title")    String title,
            @Param("location") String location,
            @Param("category") String category,
            @Param("jobType")  JobType jobType,
            @Param("minExp")   Integer minExp,
            @Param("minSal")   Double minSal,
            @Param("maxSal")   Double maxSal,
            Pageable pageable
    );

    @Modifying
    @Query("UPDATE Job j SET j.viewCount = j.viewCount + 1 WHERE j.jobId = :jobId")
    void incrementViewCount(@Param("jobId") Long jobId);

    long countByPostedBy(Long postedBy);

    long countByPostedByAndStatus(Long postedBy, JobStatus status);

    boolean existsByJobIdAndPostedBy(Long jobId, Long postedBy);
}
