package com.careerdomain.service;

import com.careerdomain.entity.Domain;
import com.careerdomain.entity.JobRole;
import com.careerdomain.repository.DomainRepository;
import com.careerdomain.repository.JobRoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DomainClassificationService {

    private final DomainRepository domainRepository;
    private final JobRoleRepository jobRoleRepository;

    public DomainClassificationService(
            DomainRepository domainRepository,
            JobRoleRepository jobRoleRepository
    ) {
        this.domainRepository = domainRepository;
        this.jobRoleRepository = jobRoleRepository;
    }

    @Transactional
    public Map<String, Object> reclassifyAllRoles() {

        List<Domain> domains = domainRepository.findAll();

        Map<String, Domain> domainMap = domains.stream()
                .collect(Collectors.toMap(
                        domain -> normalize(domain.getName()),
                        domain -> domain,
                        (first, second) -> first,
                        LinkedHashMap::new
                ));

        Domain software = requireDomain(
                domainMap,
                "Software Engineering"
        );

        Domain dataAi = requireDomain(
                domainMap,
                "Data & Artificial Intelligence"
        );

        Domain cybersecurity = requireDomain(
                domainMap,
                "Cybersecurity"
        );

        Domain informationTechnology = requireDomain(
                domainMap,
                "Information Technology"
        );

        Domain cloudDevOps = requireDomain(
                domainMap,
                "Cloud & DevOps"
        );

        List<JobRole> roles = jobRoleRepository.findAll();

        int changed = 0;

        for (JobRole role : roles) {

            Domain target = classifyRole(
                    role.getName(),
                    role.getDescription(),
                    software,
                    dataAi,
                    cybersecurity,
                    informationTechnology,
                    cloudDevOps
            );

            Domain current = role.getDomain();

            if (current == null
                    || !Objects.equals(
                            current.getId(),
                            target.getId()
                    )) {

                role.setDomain(target);
                changed++;
            }
        }

        jobRoleRepository.saveAllAndFlush(roles);

        Map<String, Object> result = new LinkedHashMap<>();

        result.put("status", "SUCCESS");
        result.put("totalRoles", roles.size());
        result.put("rolesReclassified", changed);

        result.put(
                "softwareEngineeringRoles",
                countRoles(software)
        );

        result.put(
                "dataArtificialIntelligenceRoles",
                countRoles(dataAi)
        );

        result.put(
                "cybersecurityRoles",
                countRoles(cybersecurity)
        );

        result.put(
                "informationTechnologyRoles",
                countRoles(informationTechnology)
        );

        result.put(
                "cloudDevOpsRoles",
                countRoles(cloudDevOps)
        );

        return result;
    }

    private Domain classifyRole(
            String name,
            String description,
            Domain software,
            Domain dataAi,
            Domain cybersecurity,
            Domain informationTechnology,
            Domain cloudDevOps
    ) {

        String roleName = normalize(name);

        String combinedText = normalize(
                (name == null ? "" : name)
                        + " "
                        + (description == null ? "" : description)
        );

        /*
         * High-confidence role-name classification.
         */

        if (containsAny(
                roleName,
                "machine learning engineer",
                "machine learning specialist",
                "data scientist",
                "data engineer",
                "database developer",
                "database designer",
                "database integrator",
                "statistician",
                "bioinformatics scientist"
        )) {
            return dataAi;
        }

        if (containsAny(
                roleName,
                "security engineer",
                "security administrator",
                "ict security administrator",
                "cybersecurity",
                "cyber security",
                "information security"
        )) {
            return cybersecurity;
        }

        if (containsAny(
                roleName,
                "cloud engineer",
                "cloud architect",
                "cloud developer",
                "devops engineer",
                "devops specialist",
                "site reliability engineer",
                "platform engineer"
        )) {
            return cloudDevOps;
        }

        if (containsAny(
                roleName,
                "system administrator",
                "systems administrator",
                "network administrator",
                "network architect",
                "network engineer",
                "system analyst",
                "systems analyst",
                "system architect",
                "systems architect",
                "database administrator",
                "ict network architect",
                "ict system analyst",
                "ict system architect"
        )) {
            return informationTechnology;
        }

        if (containsAny(
                roleName,
                "software developer",
                "software engineer",
                "software architect",
                "backend developer",
                "backend engineer",
                "full stack developer",
                "full stack engineer",
                "web developer",
                "web engineer",
                "application developer",
                "ict application developer",
                "user interface developer",
                "embedded systems software developer",
                "embedded system software developer"
        )) {
            return software;
        }

        /*
         * Explainable keyword scoring fallback.
         */

        Map<Domain, Integer> scores = new LinkedHashMap<>();

        scores.put(
                cloudDevOps,
                score(
                        combinedText,
                        Set.of(
                                "cloud engineer",
                                "cloud computing",
                                "cloud infrastructure",
                                "cloud architecture",
                                "cloud native",
                                "cloud-native",
                                "devops",
                                "site reliability",
                                "platform engineering",
                                "container orchestration",
                                "kubernetes",
                                "docker",
                                "continuous integration",
                                "continuous deployment",
                                "infrastructure as code",
                                "terraform",
                                "aws",
                                "azure",
                                "google cloud"
                        )
                )
        );

        scores.put(
                cybersecurity,
                score(
                        combinedText,
                        Set.of(
                                "cybersecurity",
                                "cyber security",
                                "information security",
                                "security engineering",
                                "security engineer",
                                "security administrator",
                                "ict security",
                                "application security",
                                "security automation",
                                "vulnerability",
                                "threat detection",
                                "incident response",
                                "penetration testing",
                                "network security",
                                "access control",
                                "security operations"
                        )
                )
        );

        scores.put(
                dataAi,
                score(
                        combinedText,
                        Set.of(
                                "data scientist",
                                "data science",
                                "data engineer",
                                "data engineering",
                                "machine learning",
                                "artificial intelligence",
                                "deep learning",
                                "predictive model",
                                "data analysis",
                                "data analytics",
                                "statistics",
                                "statistical analysis",
                                "database development",
                                "database developer",
                                "database designer",
                                "database integrator",
                                "data pipeline",
                                "data processing",
                                "big data",
                                "bioinformatics"
                        )
                )
        );

        scores.put(
                software,
                score(
                        combinedText,
                        Set.of(
                                "software development",
                                "software developer",
                                "software engineering",
                                "software engineer",
                                "software system",
                                "software application",
                                "programming",
                                "programming language",
                                "application development",
                                "web development",
                                "frontend",
                                "front end",
                                "backend",
                                "back end",
                                "full stack",
                                "java",
                                "javascript",
                                "python",
                                "spring",
                                "react",
                                "node.js",
                                "rest api",
                                "api development",
                                "application software",
                                "embedded software"
                        )
                )
        );

        scores.put(
                informationTechnology,
                score(
                        combinedText,
                        Set.of(
                                "information technology",
                                "ict system",
                                "ict systems",
                                "system administration",
                                "systems administration",
                                "system analyst",
                                "systems analyst",
                                "system architecture",
                                "systems architecture",
                                "network administration",
                                "network architecture",
                                "telecommunications",
                                "it infrastructure",
                                "ict infrastructure",
                                "technical support",
                                "it support",
                                "computer network",
                                "network systems"
                        )
                )
        );

        int highestScore = scores.values()
                .stream()
                .mapToInt(Integer::intValue)
                .max()
                .orElse(0);

        /*
         * No meaningful technical evidence:
         * classify the role as general IT instead of incorrectly
         * calling unrelated occupations software careers.
         */
        if (highestScore == 0) {
            return informationTechnology;
        }

        /*
         * Tie-breaking:
         * specific infrastructure/security/data/cloud domains
         * take precedence over generic software/IT.
         */
        if (scores.get(cloudDevOps) == highestScore) {
            return cloudDevOps;
        }

        if (scores.get(cybersecurity) == highestScore) {
            return cybersecurity;
        }

        if (scores.get(dataAi) == highestScore) {
            return dataAi;
        }

        if (scores.get(software) == highestScore) {
            return software;
        }

        return informationTechnology;
    }

    private int score(
            String text,
            Set<String> keywords
    ) {

        int total = 0;

        for (String keyword : keywords) {
            if (text.contains(normalize(keyword))) {
                total++;
            }
        }

        return total;
    }

    private boolean containsAny(
            String text,
            String... values
    ) {

        for (String value : values) {
            if (text.contains(normalize(value))) {
                return true;
            }
        }

        return false;
    }

    private long countRoles(Domain domain) {
        return jobRoleRepository
                .findByDomainId(domain.getId())
                .size();
    }

    private Domain requireDomain(
            Map<String, Domain> domainMap,
            String name
    ) {

        Domain domain = domainMap.get(
                normalize(name)
        );

        if (domain == null) {
            throw new IllegalStateException(
                    "Required career domain is missing: " + name
            );
        }

        return domain;
    }

    private String normalize(String value) {

        if (value == null) {
            return "";
        }

        return value
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9+#.]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
}