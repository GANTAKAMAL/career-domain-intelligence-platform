package com.careerdomain.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "user_skills",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_user_skill",
        columnNames = {"user_id", "skill_name"}
    )
)
public class UserSkill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "skill_name", nullable = false, length = 150)
    private String skillName;

    @Column(nullable = false)
    private Integer proficiency;

    @Column(nullable = false)
    private Integer targetProficiency;

    @Column(nullable = false)
    private Boolean verified;

    private LocalDateTime updatedAt;

    public UserSkill() {
    }

    public UserSkill(
            User user,
            String skillName,
            Integer proficiency,
            Integer targetProficiency,
            Boolean verified) {
        this.user = user;
        this.skillName = skillName;
        this.proficiency = proficiency;
        this.targetProficiency = targetProficiency;
        this.verified = verified;
    }

    @PrePersist
    @PreUpdate
    public void touch() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getSkillName() {
        return skillName;
    }

    public void setSkillName(String skillName) {
        this.skillName = skillName;
    }

    public Integer getProficiency() {
        return proficiency;
    }

    public void setProficiency(Integer proficiency) {
        this.proficiency = proficiency;
    }

    public Integer getTargetProficiency() {
        return targetProficiency;
    }

    public void setTargetProficiency(Integer targetProficiency) {
        this.targetProficiency = targetProficiency;
    }

    public Boolean getVerified() {
        return verified;
    }

    public void setVerified(Boolean verified) {
        this.verified = verified;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
