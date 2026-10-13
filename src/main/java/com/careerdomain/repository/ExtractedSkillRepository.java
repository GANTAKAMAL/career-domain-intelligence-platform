package com.careerdomain.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.careerdomain.entity.ExtractedSkill;

@Repository
public interface ExtractedSkillRepository
        extends JpaRepository<ExtractedSkill, Long> {

    List<ExtractedSkill> findByJobDescriptionId(Long jobDescriptionId);

    List<ExtractedSkill> findBySkillNameIgnoreCase(String skillName);
}