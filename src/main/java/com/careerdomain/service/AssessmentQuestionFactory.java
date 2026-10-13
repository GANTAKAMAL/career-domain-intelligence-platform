package com.careerdomain.service;

import com.careerdomain.entity.Assessment;
import com.careerdomain.entity.JobDescription;
import java.util.*;
import org.springframework.stereotype.Component;

/** Curated, skill-specific questions. The deterministic shuffle prevents a fixed answer position. */
@Component
public class AssessmentQuestionFactory {
    private record Prompt(String question, String correct, List<String> distractors) { }
    private final Map<String, Prompt> bank = Map.ofEntries(
            entry("Java", "Which Java feature enables platform-independent execution?", "The Java Virtual Machine (JVM)", "A browser plugin", "A database trigger", "A CSS preprocessor"),
            entry("Spring Boot", "What does Spring Boot auto-configuration primarily do?", "Configures application components from classpath dependencies", "Compiles Java into native code", "Creates database backups", "Replaces HTTP with FTP"),
            entry("REST API", "Which HTTP method is conventionally used to partially update a resource?", "PATCH", "TRACE", "CONNECT", "OPTIONS"),
            entry("SQL", "Which SQL clause filters groups after aggregation?", "HAVING", "WHERE", "ORDER BY", "DISTINCT"),
            entry("MySQL", "Which MySQL feature improves lookups on frequently queried columns?", "An index", "A CSS selector", "A browser cache", "A Git tag"),
            entry("Git", "What does git commit create?", "A snapshot in local repository history", "A deployed production server", "A database schema", "A Docker image"),
            entry("AWS", "Which AWS service provides scalable object storage?", "Amazon S3", "Amazon Route 53", "Amazon CloudWatch", "Amazon IAM"),
            entry("Python", "Which Python construct commonly creates a concise transformed list?", "A list comprehension", "A Maven profile", "A SQL view", "A CSS grid"),
            entry("C++", "Which C++ concept releases resources when an object's lifetime ends?", "RAII", "CORS", "ACID", "REST"),
            entry("JavaScript", "Which JavaScript keyword creates a block-scoped variable?", "let", "var only", "class only", "import only"),
            entry("React", "What is the React mechanism for storing state in a function component?", "useState", "useQuery", "useRoute", "useCache"),
            entry("Node.js", "What is Node.js best known for using to handle I/O efficiently?", "An event-driven, non-blocking runtime", "A JVM bytecode compiler", "A relational query planner", "A CSS rendering engine"),
            entry("Docker", "What is a Docker image?", "An immutable template used to create containers", "A running virtual machine", "A SQL database", "A source-control branch"),
            entry("Kubernetes", "Which Kubernetes object maintains a desired number of pod replicas?", "Deployment", "ConfigMap", "Namespace", "ServiceAccount"),
            entry("MongoDB", "MongoDB stores data primarily as what?", "Flexible BSON documents", "CSV rows only", "Java bytecode", "HTML templates"),
            entry("PostgreSQL", "What PostgreSQL capability is useful for semi-structured documents?", "JSONB", "JSX", "JVM", "JWT"),
            entry("Machine Learning", "Why is a validation set used in machine learning?", "To tune and evaluate generalization during development", "To encrypt training data", "To render UI components", "To store Git commits"),
            entry("Deep Learning", "A deep neural network typically learns through what?", "Multiple layers of learned representations", "Manual SQL joins", "Static HTML pages", "Operating-system threads"),
            entry("HTML", "Which HTML element is most appropriate for primary page content?", "main", "style", "script", "meta"),
            entry("CSS", "Which CSS layout system is designed for two-dimensional layouts?", "CSS Grid", "HTML tables only", "REST", "SQL"));

    private static Map.Entry<String, Prompt> entry(String skill, String q, String correct, String... distractors) {
        return Map.entry(skill, new Prompt(q, correct, List.of(distractors)));
    }

    public Assessment create(JobDescription job, String skill) {
        Prompt prompt = bank.getOrDefault(skill, new Prompt("Which practice best demonstrates proficiency in " + skill + "?",
                "Apply " + skill + " to solve a relevant problem", List.of("Avoid documentation", "Use unrelated tools", "Skip testing")));
        List<String> answers = new ArrayList<>(prompt.distractors()); answers.add(prompt.correct());
        Collections.shuffle(answers, new Random(Objects.hash(job.getId(), skill)));
        Assessment assessment = new Assessment(); assessment.setJobDescription(job); assessment.setSkill(skill); assessment.setQuestion(prompt.question());
        assessment.setOptionA(answers.get(0)); assessment.setOptionB(answers.get(1)); assessment.setOptionC(answers.get(2)); assessment.setOptionD(answers.get(3));
        assessment.setCorrectAnswer(String.valueOf((char) ('A' + answers.indexOf(prompt.correct()))));
        return assessment;
    }
}
