package com.careerdomain.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.careerdomain.entity.ExtractedSkill;
import com.careerdomain.repository.ExtractedSkillRepository;

@Service
public class ExtractedSkillService {

    private final ExtractedSkillRepository extractedSkillRepository;

    public ExtractedSkillService(
            ExtractedSkillRepository extractedSkillRepository) {
        this.extractedSkillRepository = extractedSkillRepository;
    }

    public List<ExtractedSkill> getSkillsByJobId(Long jobId) {
        return extractedSkillRepository.findByJobDescriptionId(jobId);
    }

    public List<ExtractedSkill> getSkillsByName(String skillName) {
        return extractedSkillRepository.findBySkillNameIgnoreCase(skillName);
    }

    public ExtractedSkill getSkillById(Long id) {
        return extractedSkillRepository.findById(id).orElse(null);
    }

    public ExtractedSkill saveSkill(ExtractedSkill skill) {
        return extractedSkillRepository.save(skill);
    }

    public void deleteSkill(Long id) {
        extractedSkillRepository.deleteById(id);
    }
}