package com.careerdomain.repository;

import com.careerdomain.entity.LearningResource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LearningResourceRepository extends JpaRepository<LearningResource, Long> {
    List<LearningResource> findBySkillNameIgnoreCase(String skillName);
}
