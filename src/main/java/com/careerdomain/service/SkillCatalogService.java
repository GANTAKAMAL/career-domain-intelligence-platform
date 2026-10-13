package com.careerdomain.service;

import java.util.*;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

/** Canonical vocabulary shared by profile input, extraction, graphing and matching. */
@Service
public class SkillCatalogService {
    private final Map<String, List<Pattern>> vocabulary = new LinkedHashMap<>();

    public SkillCatalogService() {
        add("Java", "java", "java programming");
        add("Spring Boot", "spring boot", "spring framework", "spring");
        add("REST API", "rest api", "rest apis", "restful api", "restful apis", "restful services", "rest services", "rest");
        add("MySQL", "mysql", "my sql"); add("SQL", "sql", "structured query language");
        add("Git", "git", "git version control"); add("AWS", "aws", "amazon web services"); add("Linux", "linux");
        add("Python", "python"); add("C++", "c++", "cpp");
        add("JavaScript", "javascript", "java script", "js");
        add("React", "react", "reactjs", "react.js");
        add("Node.js", "node.js", "nodejs", "node js", "node");
        add("Docker", "docker", "containerization"); add("Kubernetes", "kubernetes", "k8s");
        add("MongoDB", "mongodb", "mongo db"); add("PostgreSQL", "postgresql", "postgres", "postgre sql");
        add("Machine Learning", "machine learning", "machine-learning", "ml");
        add("Deep Learning", "deep learning", "deep-learning", "dl");
        add("HTML", "html", "html5"); add("CSS", "css", "css3");
    }

    private void add(String canonical, String... aliases) {
        vocabulary.put(canonical, Arrays.stream(aliases)
                // A period is normal punctuation after an abbreviation (for
                // example, "AWS.") and must not suppress a valid match.
                // Dots inside aliases such as "Node.js" are already handled
                // by Pattern.quote, while letters, digits, + and # still keep
                // partial identifiers (JavaScript, C++, C#) distinct.
                .map(alias -> Pattern.compile("(?i)(?<![a-z0-9+#])" + Pattern.quote(alias) + "(?![a-z0-9+#])"))
                .toList());
    }

    public String normalize(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("A skill name is required.");
        String clean = value.trim().replaceAll("\\s+", " ");
        for (var entry : vocabulary.entrySet()) {
            if (entry.getValue().stream().anyMatch(pattern -> pattern.matcher(clean).matches())) return entry.getKey();
        }
        return clean;
    }

    /** Returns known skills and a deterministic confidence derived from repeated independent mentions. */
    public Map<String, Double> extract(String text) {
        String source = text == null ? "" : text;
        Map<String, Double> found = new LinkedHashMap<>();
        vocabulary.forEach((skill, patterns) -> {
            int mentions = patterns.stream().mapToInt(pattern -> count(pattern, source)).sum();
            if (mentions > 0) found.put(skill, Math.min(0.98, 0.72 + (Math.min(mentions, 3) * 0.08)));
        });
        return found;
    }

    private int count(Pattern pattern, String source) {
        var matcher = pattern.matcher(source); int count = 0;
        while (matcher.find()) count++;
        return count;
    }
}
