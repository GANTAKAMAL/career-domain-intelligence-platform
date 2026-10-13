package com.careerdomain.service;

import com.careerdomain.entity.Assessment;
import com.careerdomain.entity.ExtractedSkill;
import com.careerdomain.repository.AssessmentRepository;
import java.util.Locale;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * One-way compatibility repair for assessment rows created before the skill
 * column was populated. It preserves each stored question, answer choices and
 * correct answer; only the missing skill label is restored from the associated
 * job's extracted-skill evidence.
 */
@Component
@Profile("!test")
public class AssessmentDataRepair implements CommandLineRunner {

    private final AssessmentRepository assessments;
    private final SkillCatalogService catalog;

    public AssessmentDataRepair(AssessmentRepository assessments, SkillCatalogService catalog) {
        this.assessments = assessments;
        this.catalog = catalog;
    }

    @Override
    @Transactional
    public void run(String... args) {
        assessments.findAll().stream()
                .filter(this::hasMissingSkill)
                .forEach(this::restoreSkill);
    }

    private boolean hasMissingSkill(Assessment assessment) {
        return assessment.getSkill() == null || assessment.getSkill().isBlank();
    }

    private void restoreSkill(Assessment assessment) {
        String optionA = assessment.getOptionA();
        String recovered = assessment.getJobDescription().getExtractedSkills().stream()
                .map(ExtractedSkill::getSkillName)
                .filter(skill -> sameSkill(skill, optionA))
                .findFirst()
                .orElseGet(() -> assessment.getJobDescription().getExtractedSkills().stream()
                        .map(ExtractedSkill::getSkillName)
                        .findFirst()
                        .orElse(null));
        if (recovered != null && !recovered.isBlank()) {
            assessment.setSkill(catalog.normalize(recovered));
        }
    }

    private boolean sameSkill(String left, String right) {
        return left != null && right != null
                && catalog.normalize(left).toLowerCase(Locale.ROOT)
                        .equals(catalog.normalize(right).toLowerCase(Locale.ROOT));
    }
}
