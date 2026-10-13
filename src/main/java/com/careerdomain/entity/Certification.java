package com.careerdomain.entity;

import jakarta.persistence.*;

@Entity
@Table(
    name = "certifications",
    uniqueConstraints = @UniqueConstraint(name = "uk_certification_name", columnNames = "name")
)
public class Certification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 250)
    private String name;

    @Column(length = 200)
    private String provider;

    @Column(length = 100)
    private String level;

    @Column(length = 1000)
    private String url;

    @Column(length = 2000)
    private String description;

    public Certification() {
    }

    public Certification(
            String name,
            String provider,
            String level,
            String url,
            String description) {
        this.name = name;
        this.provider = provider;
        this.level = level;
        this.url = url;
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

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
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
}
