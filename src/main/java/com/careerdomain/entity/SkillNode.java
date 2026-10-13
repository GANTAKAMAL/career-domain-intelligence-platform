package com.careerdomain.entity;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

@Node("Skill")
public class SkillNode {

    @Id
    private String name;

    public SkillNode() {
    }

    public SkillNode(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}