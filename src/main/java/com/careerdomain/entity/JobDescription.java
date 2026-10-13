package com.careerdomain.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "job_descriptions")
public class JobDescription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    private LocalDateTime createdAt;

    // ==============================
    // Extracted Skills
    // ==============================

    @OneToMany(
        mappedBy = "jobDescription",
        cascade = CascadeType.ALL,
        orphanRemoval = true,
        fetch = FetchType.EAGER
    )
    private List<ExtractedSkill> extractedSkills = new ArrayList<>();


    // ==============================
    // Assessments
    // ==============================

    @OneToMany(
        mappedBy = "jobDescription",
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    private List<Assessment> assessments = new ArrayList<>();


    // ==============================
    // Pre Persist
    // ==============================

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
    }


    // ==============================
    // Extracted Skill Helper Method
    // ==============================

    public void addExtractedSkill(ExtractedSkill skill) {

        extractedSkills.add(skill);

        skill.setJobDescription(this);
    }


    // ==============================
    // Assessment Helper Method
    // ==============================

    public void addAssessment(Assessment assessment) {

        assessments.add(assessment);

        assessment.setJobDescription(this);
    }


    // ==============================
    // Getters and Setters
    // ==============================

    public Long getId() {
        return id;
    }


    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }


    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }


    public LocalDateTime getCreatedAt() {
        return createdAt;
    }


    public List<ExtractedSkill> getExtractedSkills() {
        return extractedSkills;
    }

    public void setExtractedSkills(List<ExtractedSkill> extractedSkills) {

        this.extractedSkills = extractedSkills;

        for (ExtractedSkill skill : extractedSkills) {
            skill.setJobDescription(this);
        }
    }


    public List<Assessment> getAssessments() {
        return assessments;
    }

    public void setAssessments(List<Assessment> assessments) {

        this.assessments = assessments;

        for (Assessment assessment : assessments) {
            assessment.setJobDescription(this);
        }
    }
}
