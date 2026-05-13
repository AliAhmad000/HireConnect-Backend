package com.hireconnect.interview.repository;

import com.hireconnect.interview.entity.Interview;
import com.hireconnect.interview.enums.InterviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface InterviewRepository extends JpaRepository<Interview, Long> {

    List<Interview> findByApplicationId(Long applicationId);

    List<Interview> findByCandidateIdOrderByScheduledAtDesc(Long candidateId);

    Page<Interview> findByCandidateId(Long candidateId, Pageable pageable);

    List<Interview> findByRecruiterIdOrderByScheduledAtDesc(Long recruiterId);

    Page<Interview> findByRecruiterId(Long recruiterId, Pageable pageable);

    List<Interview> findByStatus(InterviewStatus status);

    Page<Interview> findByRecruiterIdAndStatus(Long recruiterId, InterviewStatus status, Pageable pageable);

    Optional<Interview> findByApplicationIdAndStatus(Long applicationId, InterviewStatus status);

    @Query("SELECT i FROM Interview i WHERE i.scheduledAt BETWEEN :start AND :end")
    List<Interview> findByScheduledAtBetween(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Query("SELECT i FROM Interview i WHERE i.candidateId = :candidateId AND i.scheduledAt > :now " +
           "AND i.status IN ('SCHEDULED', 'CONFIRMED', 'RESCHEDULE_REQUESTED', 'RESCHEDULED') ORDER BY i.scheduledAt ASC")
    List<Interview> findUpcomingByCandidate(
            @Param("candidateId") Long candidateId,
            @Param("now") LocalDateTime now
    );

    @Query("SELECT i FROM Interview i WHERE i.recruiterId = :recruiterId AND i.scheduledAt > :now " +
           "AND i.status IN ('SCHEDULED', 'CONFIRMED', 'RESCHEDULE_REQUESTED', 'RESCHEDULED') ORDER BY i.scheduledAt ASC")
    List<Interview> findUpcomingByRecruiter(
            @Param("recruiterId") Long recruiterId,
            @Param("now") LocalDateTime now
    );

    long countByRecruiterIdAndStatus(Long recruiterId, InterviewStatus status);

    void deleteByInterviewId(Long interviewId);
}
