package com.careerdomain.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.careerdomain.entity.Assessment;
import com.careerdomain.entity.JobDescription;

@Repository
public interface AssessmentRepository extends JpaRepository<Assessment, Long> {

    List<Assessment> findByJobDescription(JobDescription jobDescription);

    List<Assessment> findByJobDescriptionId(Long jobDescriptionId);
}