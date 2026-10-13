package com.careerdomain.entity;

import jakarta.persistence.*;

@Entity
@Table(
    name = "job_roles",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_job_role_domain_name",
        columnNames = {"domain_id", "name"}
    )
)
public class JobRole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String description;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "domain_id", nullable = false)
    private Domain domain;

    public JobRole() {
    }

    public JobRole(
            String name,
            String description,
            Domain domain
    ) {
        this.name = name;
        this.description = description;
        this.domain = domain;
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

    public Domain getDomain() {
        return domain;
    }

    public void setDomain(Domain domain) {
        this.domain = domain;
    }
}
