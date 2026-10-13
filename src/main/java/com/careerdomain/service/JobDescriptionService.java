package com.careerdomain.service;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;

import com.careerdomain.entity.ExtractedSkill;
import com.careerdomain.entity.JobDescription;
import com.careerdomain.entity.Assessment;
import com.careerdomain.repository.AssessmentRepository;
import com.careerdomain.repository.JobDescriptionRepository;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JobDescriptionService {

    private final JobDescriptionRepository jobDescriptionRepository;
    private final SkillExtractionService skillExtractionService;
    private final SkillGraphService skillGraphService;
    private final AssessmentRepository assessmentRepository;
    private final AssessmentQuestionFactory questionFactory;

    public JobDescriptionService(
            JobDescriptionRepository jobDescriptionRepository,
            SkillExtractionService skillExtractionService,
            SkillGraphService skillGraphService,
            AssessmentRepository assessmentRepository, AssessmentQuestionFactory questionFactory) {

        this.jobDescriptionRepository = jobDescriptionRepository;
        this.skillExtractionService = skillExtractionService;
        this.skillGraphService = skillGraphService;
        this.assessmentRepository = assessmentRepository;
        this.questionFactory = questionFactory;
    }

    @Transactional
    public JobDescription createJobDescription(
            JobDescription jobDescription) {

        JobDescription savedJob =
                jobDescriptionRepository.save(jobDescription);

        List<ExtractedSkill> extractedSkills =
                skillExtractionService.extractSkills(savedJob);

        savedJob.setExtractedSkills(extractedSkills);
        createAssessments(savedJob, extractedSkills);

        skillGraphService.createJobGraph(
                savedJob.getId(),
                savedJob.getTitle(),
                extractedSkills.stream()
                        .map(ExtractedSkill::getSkillName)
                        .filter(Objects::nonNull)
                        .toList()
        );

        return savedJob;
    }

    /** Each ingested job receives a small, skill-specific assessment so the
     * assessment workflow is available without an administrator-only seed API. */
    private void createAssessments(JobDescription job, List<ExtractedSkill> skills) {
        List<Assessment> assessments = skills.stream().map(skill -> questionFactory.create(job, skill.getSkillName())).toList();
        assessmentRepository.saveAll(assessments);
    }

    public List<JobDescription> getAllJobDescriptions() {

        return jobDescriptionRepository.findAll();
    }

    public JobDescription getJobDescriptionById(Long id) {

        return jobDescriptionRepository
                .findById(id)
                .orElse(null);
    }

    public List<JobDescription> searchByTitle(String title) {

        return jobDescriptionRepository
                .findByTitleContainingIgnoreCase(title);
    }

    @Transactional
    public JobDescription updateJobDescription(Long id, String title, String description) {
        JobDescription job = jobDescriptionRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Job not found."));
        job.setTitle(title.trim()); job.setDescription(description.trim());
        job.getExtractedSkills().clear();
        assessmentRepository.deleteAll(assessmentRepository.findByJobDescriptionId(id));
        JobDescription saved = jobDescriptionRepository.saveAndFlush(job);
        List<ExtractedSkill> extracted = skillExtractionService.extractSkills(saved);
        saved.setExtractedSkills(extracted); createAssessments(saved, extracted);
        skillGraphService.createJobGraph(saved.getId(), saved.getTitle(), extracted.stream().map(ExtractedSkill::getSkillName).toList());
        return saved;
    }

    public void deleteJobDescription(Long id) {
        if (!jobDescriptionRepository.existsById(id)) throw new IllegalArgumentException("Job not found.");
        skillGraphService.deleteJobGraph(id);
        jobDescriptionRepository.deleteById(id);
    }
}
