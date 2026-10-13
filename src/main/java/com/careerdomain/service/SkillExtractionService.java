package com.careerdomain.service;

import java.util.*;

import org.springframework.stereotype.Service;

import com.careerdomain.entity.ExtractedSkill;
import com.careerdomain.entity.JobDescription;
import com.careerdomain.repository.ExtractedSkillRepository;

@Service
public class SkillExtractionService {

    private final ExtractedSkillRepository extractedSkillRepository;
    private final SkillCatalogService catalog;
    public SkillExtractionService(
            ExtractedSkillRepository extractedSkillRepository, SkillCatalogService catalog) {

        this.extractedSkillRepository = extractedSkillRepository;
        this.catalog = catalog;
    }

    public List<ExtractedSkill> extractSkills(JobDescription jobDescription) {

        String text = jobDescription.getTitle() + "\n" + jobDescription.getDescription();
        List<ExtractedSkill> skills = catalog.extract(text).entrySet().stream().map(entry -> {
            ExtractedSkill skill = new ExtractedSkill();
            skill.setSkillName(entry.getKey()); skill.setConfidence(entry.getValue()); skill.setJobDescription(jobDescription);
            return skill;
        }).toList();
        return extractedSkillRepository.saveAll(skills);
    }
}
