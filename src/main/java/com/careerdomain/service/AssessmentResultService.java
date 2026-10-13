package com.careerdomain.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.careerdomain.entity.AssessmentResult;
import com.careerdomain.repository.AssessmentResultRepository;

@Service
public class AssessmentResultService {

    private final AssessmentResultRepository assessmentResultRepository;

    public AssessmentResultService(
            AssessmentResultRepository assessmentResultRepository) {
        this.assessmentResultRepository = assessmentResultRepository;
    }

    public AssessmentResult createResult(AssessmentResult result) {
        return assessmentResultRepository.save(result);
    }

    public List<AssessmentResult> getAllResults() {
        return assessmentResultRepository.findAll();
    }

    public AssessmentResult getResultById(Long id) {
        return assessmentResultRepository.findById(id).orElse(null);
    }

    public AssessmentResult getResultByAssessmentId(Long assessmentId) {
        return assessmentResultRepository
                .findByAssessmentId(assessmentId)
                .orElse(null);
    }

    public AssessmentResult updateResult(Long id, AssessmentResult updatedResult) {

        AssessmentResult existingResult =
                assessmentResultRepository.findById(id).orElse(null);

        if (existingResult == null) {
            return null;
        }

        existingResult.setScore(updatedResult.getScore());
        existingResult.setResultSummary(updatedResult.getResultSummary());

        return assessmentResultRepository.save(existingResult);
    }

    public void deleteResult(Long id) {
        assessmentResultRepository.deleteById(id);
    }
}