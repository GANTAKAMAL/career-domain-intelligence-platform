package com.careerdomain.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "learning_resources")
public class LearningResource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 250)
    private String title;

    @Column(nullable = false, length = 100)
    private String type;

    @Column(nullable = false, length = 1000)
    private String url;

    @Column(length = 2000)
    private String description;

    @Column(nullable = false, length = 150)
    private String skillName;

    @Column(length = 50)
    private String difficulty;

    @Column(length = 50)
    private String estimatedHours;

    public LearningResource() {
    }

    public LearningResource(
            String title,
            String type,
            String url,
            String description,
            String skillName,
            String difficulty,
            String estimatedHours) {
        this.title = title;
        this.type = type;
        this.url = url;
        this.description = description;
        this.skillName = skillName;
        this.difficulty = difficulty;
        this.estimatedHours = estimatedHours;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSkillName() {
        return skillName;
    }

    public void setSkillName(String skillName) {
        this.skillName = skillName;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    public String getEstimatedHours() {
        return estimatedHours;
    }

    public void setEstimatedHours(String estimatedHours) {
        this.estimatedHours = estimatedHours;
    }
}
