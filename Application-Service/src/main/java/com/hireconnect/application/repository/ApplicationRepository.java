package com.hireconnect.application.repository;

import com.hireconnect.application.entity.Application;
import com.hireconnect.application.enums.ApplicationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {

    List<Application> findByCandidateIdOrderByAppliedAtDesc(Long candidateId);

    Page<Application> findByCandidateId(Long candidateId, Pageable pageable);

    List<Application> findByJobIdOrderByAppliedAtDesc(Long jobId);

    Page<Application> findByJobId(Long jobId, Pageable pageable);

    List<Application> findByStatus(ApplicationStatus status);

    Page<Application> findByJobIdAndStatus(Long jobId, ApplicationStatus status, Pageable pageable);

    Optional<Application> findByJobIdAndCandidateId(Long jobId, Long candidateId);

    boolean existsByJobIdAndCandidateId(Long jobId, Long candidateId);

    long countByJobId(Long jobId);

    long countByStatus(ApplicationStatus status);

    long countByJobIdAndStatus(Long jobId, ApplicationStatus status);

    long countByCandidateId(Long candidateId);

    List<Application> findByRecruiterIdOrderByAppliedAtDesc(Long recruiterId);

    Page<Application> findByRecruiterId(Long recruiterId, Pageable pageable);

    @Query("SELECT a FROM Application a WHERE a.appliedAt BETWEEN :startDate AND :endDate")
    List<Application> findByAppliedAtBetween(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("SELECT a FROM Application a WHERE a.jobId = :jobId AND a.appliedAt BETWEEN :startDate AND :endDate")
    List<Application> findByJobIdAndAppliedAtBetween(
            @Param("jobId") Long jobId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("SELECT COUNT(a) FROM Application a WHERE a.recruiterId = :recruiterId AND a.status = :status")
    long countByRecruiterIdAndStatus(@Param("recruiterId") Long recruiterId, @Param("status") ApplicationStatus status);

    @Query("SELECT a FROM Application a WHERE a.isWithdrawn = false AND a.candidateId = :candidateId")
    List<Application> findActiveApplicationsByCandidate(@Param("candidateId") Long candidateId);
}
