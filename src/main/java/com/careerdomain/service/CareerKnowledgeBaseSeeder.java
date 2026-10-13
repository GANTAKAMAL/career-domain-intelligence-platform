package com.careerdomain.service;

import com.careerdomain.entity.Certification;
import com.careerdomain.entity.Domain;
import com.careerdomain.entity.JobRole;
import com.careerdomain.entity.LearningResource;
import com.careerdomain.entity.Project;
import com.careerdomain.entity.Technology;
import com.careerdomain.graph.CareerGraphService;
import com.careerdomain.repository.CertificationRepository;
import com.careerdomain.repository.DomainRepository;
import com.careerdomain.repository.JobRoleRepository;
import com.careerdomain.repository.LearningResourceRepository;
import com.careerdomain.repository.ProjectRepository;
import com.careerdomain.repository.TechnologyRepository;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@Profile("!test")
public class CareerKnowledgeBaseSeeder implements CommandLineRunner {

    private final DomainRepository domainRepository;
    private final JobRoleRepository jobRoleRepository;
    private final TechnologyRepository technologyRepository;
    private final CertificationRepository certificationRepository;
    private final LearningResourceRepository learningResourceRepository;
    private final ProjectRepository projectRepository;
    private final CareerGraphService graph;

    public CareerKnowledgeBaseSeeder(
            DomainRepository domainRepository,
            JobRoleRepository jobRoleRepository,
            TechnologyRepository technologyRepository,
            CertificationRepository certificationRepository,
            LearningResourceRepository learningResourceRepository,
            ProjectRepository projectRepository,
            CareerGraphService graph) {

        this.domainRepository = domainRepository;
        this.jobRoleRepository = jobRoleRepository;
        this.technologyRepository = technologyRepository;
        this.certificationRepository = certificationRepository;
        this.learningResourceRepository = learningResourceRepository;
        this.projectRepository = projectRepository;
        this.graph = graph;
    }

    @Override
    @Transactional
    public void run(String... args) {

        /*
         * Seed domains.
         */

        Domain software = domain(
                "Software Engineering",
                "Designing, building, testing and maintaining production software systems."
        );

        Domain data = domain(
                "Data & Artificial Intelligence",
                "Data engineering, analytics and machine-learning focused careers."
        );

        Domain cybersecurity = domain(
                "Cybersecurity",
                "Security engineering, application security and defensive operations."
        );

        Domain informationTechnology = domain(
                "Information Technology",
                "Operating, supporting and improving organisational information systems and end-user services."
        );

        Domain cloudDevOps = domain(
                "Cloud & DevOps",
                "Automating, deploying and operating reliable cloud-native platforms and delivery pipelines."
        );

        /*
         * Seed job roles.
         */

        JobRole backend = role(
                "Backend Developer",
                "Builds server-side applications, APIs, business logic and data services.",
                software,
                List.of(
                        "Java",
                        "Spring Boot",
                        "SQL",
                        "REST API",
                        "Git"
                )
        );

        JobRole cloud = role(
                "Cloud Engineer",
                "Builds and operates cloud-native infrastructure and distributed services.",
                software,
                List.of(
                        "AWS",
                        "Docker",
                        "Kubernetes",
                        "Linux",
                        "Git"
                )
        );

        JobRole fullStack = role(
                "Full Stack Developer",
                "Develops complete web applications across frontend and backend layers.",
                software,
                List.of(
                        "JavaScript",
                        "React",
                        "Node.js",
                        "SQL",
                        "REST API",
                        "Git"
                )
        );

        JobRole mlEngineer = role(
                "Machine Learning Engineer",
                "Develops, evaluates and deploys machine-learning systems.",
                data,
                List.of(
                        "Python",
                        "SQL",
                        "Machine Learning",
                        "Deep Learning",
                        "Git"
                )
        );

        JobRole dataEngineer = role(
                "Data Engineer",
                "Builds reliable data pipelines, storage systems and processing workflows.",
                data,
                List.of(
                        "Python",
                        "SQL",
                        "PostgreSQL",
                        "AWS",
                        "Docker",
                        "Git"
                )
        );

        JobRole securityEngineer = role(
                "Security Engineer",
                "Designs secure applications, infrastructure controls and security automation.",
                cybersecurity,
                List.of(
                        "Python",
                        "Linux",
                        "Docker",
                        "AWS",
                        "Git"
                )
        );

        JobRole itSupportEngineer = role(
                "IT Support Engineer",
                "Supports users, endpoint systems, networks and operational IT services.",
                informationTechnology,
                List.of("Linux", "MySQL", "Git", "REST API")
        );

        JobRole devOpsEngineer = role(
                "DevOps Engineer",
                "Builds automated delivery pipelines and operates resilient cloud infrastructure.",
                cloudDevOps,
                List.of("AWS", "Docker", "Kubernetes", "Git", "Linux")
        );

        /*
         * Seed supporting career knowledge.
         */

        seedTechnologies();
        seedCertifications();
        seedResources();
        seedProjects();

        /*
         * Ensure role-to-skill graph relationships.
         */

        graphRoleConnections(
                backend,
                List.of(
                        "Java",
                        "Spring Boot",
                        "SQL",
                        "REST API",
                        "Git"
                )
        );

        graphRoleConnections(
                cloud,
                List.of(
                        "AWS",
                        "Docker",
                        "Kubernetes",
                        "Linux",
                        "Git"
                )
        );

        graphRoleConnections(
                fullStack,
                List.of(
                        "JavaScript",
                        "React",
                        "Node.js",
                        "SQL",
                        "REST API",
                        "Git"
                )
        );

        graphRoleConnections(
                mlEngineer,
                List.of(
                        "Python",
                        "SQL",
                        "Machine Learning",
                        "Deep Learning",
                        "Git"
                )
        );

        graphRoleConnections(
                dataEngineer,
                List.of(
                        "Python",
                        "SQL",
                        "PostgreSQL",
                        "AWS",
                        "Docker",
                        "Git"
                )
        );

        graphRoleConnections(
                securityEngineer,
                List.of(
                        "Python",
                        "Linux",
                        "Docker",
                        "AWS",
                        "Git"
                )
        );

        graphRoleConnections(itSupportEngineer, List.of("Linux", "MySQL", "Git", "REST API"));
        graphRoleConnections(devOpsEngineer, List.of("AWS", "Docker", "Kubernetes", "Git", "Linux"));

        /*
         * Seed technology, certification, learning-resource
         * and prerequisite relationships.
         */

        seedGraphRelationships();
    }

    private Domain domain(
            String name,
            String description) {

        Domain value = domainRepository
                .findByNameIgnoreCase(name)
                .orElseGet(Domain::new);

        value.setName(name);
        value.setDescription(description);

        Domain saved = domainRepository.save(value);

        graph.upsertDomain(
                saved.getId(),
                saved.getName(),
                saved.getDescription()
        );

        return saved;
    }

    private JobRole role(
            String name,
            String description,
            Domain domain,
            List<String> skills) {

        JobRole value = jobRoleRepository
                .findByNameIgnoreCase(name)
                .orElseGet(JobRole::new);

        value.setName(name);
        value.setDescription(description);
        value.setDomain(domain);

        JobRole saved = jobRoleRepository.save(value);

        graph.upsertJobRole(
                saved.getId(),
                saved.getName(),
                domain.getId()
        );

        graph.connectRoleSkills(
                saved.getId(),
                skills
        );

        return saved;
    }

    private void graphRoleConnections(
            JobRole role,
            List<String> skills) {

        graph.upsertJobRole(
                role.getId(),
                role.getName(),
                role.getDomain().getId()
        );

        graph.connectRoleSkills(
                role.getId(),
                skills
        );
    }

    private void seedTechnologies() {

        List<Technology> values = List.of(
                new Technology(
                        "JVM",
                        "Runtime",
                        "Java virtual machine runtime."
                ),

                new Technology(
                        "Spring Boot",
                        "Backend Framework",
                        "Framework for production Java services."
                ),

                new Technology(
                        "MySQL",
                        "Database",
                        "Relational database platform."
                ),

                new Technology(
                        "PostgreSQL",
                        "Database",
                        "Advanced open-source relational database."
                ),

                new Technology(
                        "AWS",
                        "Cloud",
                        "Amazon cloud infrastructure and managed services."
                ),

                new Technology(
                        "Docker",
                        "Containers",
                        "Container packaging and runtime technology."
                ),

                new Technology(
                        "Kubernetes",
                        "Orchestration",
                        "Container orchestration platform."
                ),

                new Technology(
                        "React",
                        "Frontend",
                        "Component-based frontend library."
                ),

                new Technology(
                        "Node.js",
                        "Runtime",
                        "JavaScript server runtime."
                ),

                new Technology(
                        "Git",
                        "Version Control",
                        "Distributed version control system."
                )
        );

        for (Technology item : values) {

            Technology existing = technologyRepository
                    .findByNameIgnoreCase(item.getName())
                    .orElseGet(Technology::new);

            existing.setName(item.getName());
            existing.setCategory(item.getCategory());
            existing.setDescription(item.getDescription());

            technologyRepository.save(existing);
        }
    }

    private void seedCertifications() {

        List<Certification> values = List.of(
                new Certification(
                        "AWS Certified Cloud Practitioner",
                        "AWS",
                        "Foundational",
                        "https://aws.amazon.com/certification/",
                        "Foundational AWS cloud certification."
                ),

                new Certification(
                        "AWS Certified Developer – Associate",
                        "AWS",
                        "Associate",
                        "https://aws.amazon.com/certification/",
                        "Developer-focused AWS certification."
                ),

                new Certification(
                        "Oracle Java Certification",
                        "Oracle",
                        "Professional",
                        "https://education.oracle.com/java",
                        "Java programming certification pathway."
                ),

                new Certification(
                        "Certified Kubernetes Application Developer",
                        "CNCF",
                        "Associate",
                        "https://www.cncf.io/training/certification/",
                        "Kubernetes application development certification."
                )
        );

        for (Certification item : values) {

            Certification existing = certificationRepository
                    .findByNameIgnoreCase(item.getName())
                    .orElseGet(Certification::new);

            existing.setName(item.getName());
            existing.setProvider(item.getProvider());
            existing.setLevel(item.getLevel());
            existing.setUrl(item.getUrl());
            existing.setDescription(item.getDescription());

            certificationRepository.save(existing);
        }
    }

    private void seedResources() {

        List<LearningResource> values = List.of(

                resource(
                        "Java Fundamentals",
                        "Course",
                        "https://dev.java/learn/",
                        "Java language fundamentals and platform concepts.",
                        "Java",
                        "Beginner",
                        "12"
                ),

                resource(
                        "Spring Boot Guides",
                        "Documentation",
                        "https://spring.io/guides",
                        "Official Spring learning guides.",
                        "Spring Boot",
                        "Intermediate",
                        "8"
                ),

                resource(
                        "SQL Tutorial",
                        "Tutorial",
                        "https://www.w3schools.com/sql/",
                        "Structured query language fundamentals.",
                        "SQL",
                        "Beginner",
                        "10"
                ),

                resource(
                        "MySQL Reference",
                        "Documentation",
                        "https://dev.mysql.com/doc/",
                        "Official MySQL documentation and reference material.",
                        "MySQL",
                        "Intermediate",
                        "8"
                ),

                resource(
                        "PostgreSQL Tutorial",
                        "Documentation",
                        "https://www.postgresql.org/docs/",
                        "Official PostgreSQL documentation and learning material.",
                        "PostgreSQL",
                        "Intermediate",
                        "10"
                ),

                resource(
                        "REST API Design",
                        "Tutorial",
                        "https://restfulapi.net/",
                        "REST architectural concepts and API design.",
                        "REST API",
                        "Intermediate",
                        "6"
                ),

                resource(
                        "AWS Skill Builder",
                        "Learning Platform",
                        "https://skillbuilder.aws/",
                        "AWS guided training resources.",
                        "AWS",
                        "Intermediate",
                        "20"
                ),

                resource(
                        "Docker Get Started",
                        "Documentation",
                        "https://docs.docker.com/get-started/",
                        "Official Docker introduction.",
                        "Docker",
                        "Beginner",
                        "5"
                ),

                resource(
                        "Kubernetes Basics",
                        "Tutorial",
                        "https://kubernetes.io/docs/tutorials/kubernetes-basics/",
                        "Official Kubernetes fundamentals.",
                        "Kubernetes",
                        "Intermediate",
                        "10"
                ),

                resource(
                        "Linux Command Line Basics",
                        "Tutorial",
                        "https://ubuntu.com/tutorials/command-line-for-beginners",
                        "Linux command-line and system fundamentals.",
                        "Linux",
                        "Beginner",
                        "6"
                ),

                resource(
                        "React Learn",
                        "Documentation",
                        "https://react.dev/learn",
                        "Official React learning material.",
                        "React",
                        "Beginner",
                        "12"
                ),

                resource(
                        "Node.js Learn",
                        "Documentation",
                        "https://nodejs.org/en/learn",
                        "Official Node.js learning resources.",
                        "Node.js",
                        "Intermediate",
                        "10"
                ),

                resource(
                        "Python Tutorial",
                        "Documentation",
                        "https://docs.python.org/3/tutorial/",
                        "Python programming fundamentals.",
                        "Python",
                        "Beginner",
                        "12"
                ),

                resource(
                        "Machine Learning Crash Course",
                        "Course",
                        "https://developers.google.com/machine-learning/crash-course",
                        "Machine-learning fundamentals and practical exercises.",
                        "Machine Learning",
                        "Intermediate",
                        "15"
                ),

                resource(
                        "Deep Learning",
                        "Course",
                        "https://www.deeplearning.ai/",
                        "Deep-learning concepts and practical learning.",
                        "Deep Learning",
                        "Advanced",
                        "25"
                ),

                resource(
                        "Git Documentation",
                        "Documentation",
                        "https://git-scm.com/doc",
                        "Official Git documentation.",
                        "Git",
                        "Beginner",
                        "5"
                )
        );

        for (LearningResource item : values) {

            List<LearningResource> existing =
                    learningResourceRepository
                            .findBySkillNameIgnoreCase(
                                    item.getSkillName()
                            );

            LearningResource saved;

            if (existing.isEmpty()) {

                saved = learningResourceRepository.save(item);

            } else {

                saved = existing.getFirst();

                saved.setTitle(item.getTitle());
                saved.setType(item.getType());
                saved.setUrl(item.getUrl());
                saved.setDescription(item.getDescription());
                saved.setDifficulty(item.getDifficulty());
                saved.setEstimatedHours(item.getEstimatedHours());

                saved = learningResourceRepository.save(saved);
            }

            graph.connectSkillLearningResource(
                    saved.getSkillName(),
                    List.of(saved.getTitle())
            );
        }
    }

    private LearningResource resource(
            String title,
            String type,
            String url,
            String description,
            String skill,
            String difficulty,
            String hours) {

        return new LearningResource(
                title,
                type,
                url,
                description,
                skill,
                difficulty,
                hours
        );
    }

    private void seedProjects() {

        Domain software = domainRepository
                .findByNameIgnoreCase("Software Engineering")
                .orElseThrow();

        if (projectRepository.count() == 0) {

            projectRepository.save(
                    new Project(
                            "Career Intelligence REST Platform",
                            "Backend platform exposing career intelligence, assessment and recommendation APIs.",
                            software,
                            "Java, Spring Boot, MySQL, Neo4j, Docker",
                            "Java, Spring Boot, REST API, SQL, Git"
                    )
            );

            projectRepository.save(
                    new Project(
                            "Cloud-Native Microservice Platform",
                            "Containerized backend deployed with cloud infrastructure and Kubernetes.",
                            software,
                            "AWS, Docker, Kubernetes",
                            "AWS, Docker, Kubernetes, Git"
                    )
            );

            projectRepository.save(
                    new Project(
                            "Full Stack Career Dashboard",
                            "Interactive career analytics dashboard consuming a REST backend.",
                            software,
                            "React, Node.js, JavaScript, REST API",
                            "React, Node.js, JavaScript, REST API"
                    )
            );
        }
    }

    private void seedGraphRelationships() {

        /*
         * ----------------------------------------------------------
         * Skill -> Technology
         * ----------------------------------------------------------
         */

        graph.connectSkillTechnology(
                "Java",
                List.of("JVM")
        );

        graph.connectSkillTechnology(
                "Spring Boot",
                List.of(
                        "Spring Boot",
                        "JVM"
                )
        );

        graph.connectSkillTechnology(
                "SQL",
                List.of(
                        "MySQL",
                        "PostgreSQL"
                )
        );

        graph.connectSkillTechnology(
                "AWS",
                List.of("AWS")
        );

        graph.connectSkillTechnology(
                "Docker",
                List.of("Docker")
        );

        graph.connectSkillTechnology(
                "Kubernetes",
                List.of(
                        "Kubernetes",
                        "Docker"
                )
        );

        graph.connectSkillTechnology(
                "React",
                List.of(
                        "React",
                        "JavaScript"
                )
        );

        graph.connectSkillTechnology(
                "Node.js",
                List.of(
                        "Node.js",
                        "JavaScript"
                )
        );

        /*
         * ----------------------------------------------------------
         * Skill -> Certification
         * ----------------------------------------------------------
         */

        graph.connectSkillCertification(
                "AWS",
                List.of(
                        "AWS Certified Cloud Practitioner",
                        "AWS Certified Developer – Associate"
                )
        );

        graph.connectSkillCertification(
                "Java",
                List.of(
                        "Oracle Java Certification"
                )
        );

        graph.connectSkillCertification(
                "Kubernetes",
                List.of(
                        "Certified Kubernetes Application Developer"
                )
        );

        /*
         * ----------------------------------------------------------
         * Skill -> Prerequisite Skill
         *
         * source -[:PREREQUISITE_FOR]-> target
         * ----------------------------------------------------------
         */

        graph.connectSkillPrerequisites(
                "Spring Boot",
                List.of("Java")
        );

        graph.connectSkillPrerequisites(
                "MySQL",
                List.of("SQL")
        );

        graph.connectSkillPrerequisites(
                "PostgreSQL",
                List.of("SQL")
        );

        graph.connectSkillPrerequisites(
                "Docker",
                List.of("Linux")
        );

        graph.connectSkillPrerequisites(
                "AWS",
                List.of("Linux")
        );

        graph.connectSkillPrerequisites(
                "Kubernetes",
                List.of(
                        "Docker",
                        "AWS"
                )
        );

        graph.connectSkillPrerequisites(
                "Machine Learning",
                List.of(
                        "Python",
                        "SQL"
                )
        );

        graph.connectSkillPrerequisites(
                "Deep Learning",
                List.of(
                        "Python",
                        "Machine Learning"
                )
        );

        graph.connectSkillPrerequisites(
                "React",
                List.of("JavaScript")
        );

        graph.connectSkillPrerequisites(
                "Node.js",
                List.of("JavaScript")
        );
    }
}
