package com.hireconnect.analytics.repository;

import com.hireconnect.analytics.entity.ApplicationEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ApplicationEventRepository extends JpaRepository<ApplicationEvent, Long> {

    long countByJobId(Long jobId);

    long countByRecruiterId(Long recruiterId);

    long countByRecruiterIdAndNewStatus(Long recruiterId, String newStatus);

    long countByJobIdAndNewStatus(Long jobId, String newStatus);

    List<ApplicationEvent> findByJobIdOrderByCreatedAtDesc(Long jobId);

    List<ApplicationEvent> findByRecruiterIdOrderByCreatedAtDesc(Long recruiterId);

    List<ApplicationEvent> findByEventTypeOrderByCreatedAtDesc(String eventType);

    @Query("SELECT ae FROM ApplicationEvent ae WHERE ae.recruiterId = :rid AND ae.createdAt BETWEEN :start AND :end")
    List<ApplicationEvent> findByRecruiterIdAndDateRange(
            @Param("rid") Long recruiterId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Query("SELECT ae.newStatus, COUNT(ae) FROM ApplicationEvent ae WHERE ae.recruiterId = :rid " +
           "AND ae.eventType = 'APPLICATION_STATUS_CHANGED' GROUP BY ae.newStatus")
    List<Object[]> findPipelineStatsByRecruiter(@Param("rid") Long recruiterId);

    @Query("SELECT COUNT(ae) FROM ApplicationEvent ae WHERE ae.eventType = 'APPLICATION_SUBMITTED'")
    long countTotalApplications();

    @Query("SELECT ae.newStatus, COUNT(ae) FROM ApplicationEvent ae " +
           "WHERE ae.eventType = 'APPLICATION_STATUS_CHANGED' GROUP BY ae.newStatus")
    List<Object[]> findGlobalPipelineStats();
}
