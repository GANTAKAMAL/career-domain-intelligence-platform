package com.careerdomain.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
    name = "domains",
    uniqueConstraints = @UniqueConstraint(name = "uk_domain_name", columnNames = "name")
)
public class Domain {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 3000)
    private String description;

    @OneToMany(mappedBy = "domain", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<JobRole> jobRoles = new ArrayList<>();

    @OneToMany(mappedBy = "domain")
    private List<Project> projects = new ArrayList<>();

    public Domain() {
    }

    public Domain(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<JobRole> getJobRoles() {
        return jobRoles;
    }

    public void setJobRoles(List<JobRole> jobRoles) {
        this.jobRoles = jobRoles;
    }

    public List<Project> getProjects() {
        return projects;
    }

    public void setProjects(List<Project> projects) {
        this.projects = projects;
    }
}
