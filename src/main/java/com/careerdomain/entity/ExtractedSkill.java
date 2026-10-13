package com.careerdomain.entity;

import jakarta.persistence.*;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "extracted_skills")
public class ExtractedSkill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String skillName;

    private Double confidence;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "job_description_id", nullable = false)
    private JobDescription jobDescription;

    public Long getId() {
        return id;
    }

    public String getSkillName() {
        return skillName;
    }

    public void setSkillName(String skillName) {
        this.skillName = skillName;
    }

    public Double getConfidence() {
        return confidence;
    }

    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }

    public JobDescription getJobDescription() {
        return jobDescription;
    }

    public void setJobDescription(JobDescription jobDescription) {
        this.jobDescription = jobDescription;
    }
}