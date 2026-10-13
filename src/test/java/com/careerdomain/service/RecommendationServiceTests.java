package com.careerdomain.service;

import static org.junit.jupiter.api.Assertions.*;
import com.careerdomain.entity.ExtractedSkill;
import com.careerdomain.entity.JobDescription;
import java.util.List;
import org.junit.jupiter.api.Test;

class RecommendationServiceTests {
    @Test
    void calculatesCosineSimilarityAndGapsFromNormalizedSkills() {
        SkillCatalogService catalog = new SkillCatalogService();
        RecommendationService service = new RecommendationService(catalog);
        JobDescription job = job("Backend Engineer", "Java", "Spring Boot", "MySQL");
        var result = service.recommend(List.of(job), List.of("Java programming", "Spring" )).getFirst();
        assertTrue(result.similarityScore() > 0);
        assertEquals(java.util.Set.of("MySQL"), result.missingSkills());
        assertEquals(java.util.Set.of("Java", "Spring Boot"), result.matchingSkills());
        assertTrue(result.matchPercentage() < 100);
    }

    private JobDescription job(String title, String... skills) {
        JobDescription job = new JobDescription(); job.setTitle(title); job.setDescription(title);
        for (String name : skills) { ExtractedSkill skill = new ExtractedSkill(); skill.setSkillName(name); skill.setConfidence(.9); job.addExtractedSkill(skill); }
        return job;
    }
}
