package com.careerdomain.entity;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

@Node("Job")
public class JobNode {

    @Id
    private Long id;

    private String title;

    @Relationship(type = "REQUIRES")
    private List<SkillNode> skills = new ArrayList<>();

    public JobNode() {
    }

    public JobNode(Long id, String title) {
        this.id = id;
        this.title = title;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<SkillNode> getSkills() {
        return skills;
    }

    public void setSkills(List<SkillNode> skills) {
        this.skills = skills;
    }

    public void addSkill(SkillNode skill) {
        skills.add(skill);
    }
}