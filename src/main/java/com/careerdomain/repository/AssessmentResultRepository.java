package com.careerdomain.repository;

import java.util.Optional;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.careerdomain.entity.AssessmentResult;

@Repository
public interface AssessmentResultRepository
        extends JpaRepository<AssessmentResult, Long> {

    Optional<AssessmentResult> findByAssessmentId(Long assessmentId);
    List<AssessmentResult> findByUserIdOrderByIdDesc(Long userId);
}
