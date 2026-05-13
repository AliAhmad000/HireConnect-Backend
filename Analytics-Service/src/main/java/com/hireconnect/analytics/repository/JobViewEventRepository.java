package com.hireconnect.analytics.repository;

import com.hireconnect.analytics.entity.JobViewEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface JobViewEventRepository extends JpaRepository<JobViewEvent, Long> {

    long countByJobId(Long jobId);

    long countByRecruiterId(Long recruiterId);

    long countByJobIdAndCreatedAtBetween(Long jobId, LocalDateTime start, LocalDateTime end);

    @Query("SELECT j.jobId, COUNT(j) FROM JobViewEvent j WHERE j.recruiterId = :rid GROUP BY j.jobId ORDER BY COUNT(j) DESC")
    List<Object[]> findTopJobsByViewsForRecruiter(@Param("rid") Long recruiterId);

    @Query("SELECT COUNT(DISTINCT j.jobId) FROM JobViewEvent j WHERE j.recruiterId = :rid")
    long countDistinctJobsByRecruiterId(@Param("rid") Long recruiterId);
}
