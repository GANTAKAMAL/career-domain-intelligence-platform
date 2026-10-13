package com.careerdomain.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "projects")
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 250)
    private String title;

    @Column(length = 5000)
    private String description;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "domain_id")
    private Domain domain;

    @Column(length = 3000)
    private String technologies;

    @Column(length = 3000)
    private String skills;

    public Project() {
    }

    public Project(
            String title,
            String description,
            Domain domain,
            String technologies,
            String skills) {
        this.title = title;
        this.description = description;
        this.domain = domain;
        this.technologies = technologies;
        this.skills = skills;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Domain getDomain() {
        return domain;
    }

    public void setDomain(Domain domain) {
        this.domain = domain;
    }

    public String getTechnologies() {
        return technologies;
    }

    public void setTechnologies(String technologies) {
        this.technologies = technologies;
    }

    public String getSkills() {
        return skills;
    }

    public void setSkills(String skills) {
        this.skills = skills;
    }
}
