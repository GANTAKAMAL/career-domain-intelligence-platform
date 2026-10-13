package com.careerdomain.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.careerdomain.entity.Assessment;
import com.careerdomain.repository.AssessmentRepository;

@Service
public class AssessmentService {

    private final AssessmentRepository assessmentRepository;

    public AssessmentService(AssessmentRepository assessmentRepository) {
        this.assessmentRepository = assessmentRepository;
    }

    public Assessment createAssessment(Assessment assessment) {
        return assessmentRepository.save(assessment);
    }

    public List<Assessment> getAllAssessments() {
        return assessmentRepository.findAll();
    }

    public List<Assessment> getAssessmentsByJobId(Long jobId) {
        return assessmentRepository.findByJobDescriptionId(jobId);
    }

    public Assessment getAssessmentById(Long id) {
        return assessmentRepository.findById(id).orElse(null);
    }

    public void deleteAssessment(Long id) {
        assessmentRepository.deleteById(id);
    }
}