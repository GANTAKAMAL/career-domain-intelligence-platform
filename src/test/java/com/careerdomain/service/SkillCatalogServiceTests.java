package com.careerdomain.service;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class SkillCatalogServiceTests {
    private final SkillCatalogService catalog = new SkillCatalogService();

    @Test
    void normalizesAliasesAndDoesNotTreatJavaScriptAsJava() {
        assertEquals("Node.js", catalog.normalize("NodeJS"));
        assertEquals("PostgreSQL", catalog.normalize("Postgres"));
        assertEquals("Machine Learning", catalog.normalize("ML"));
        var extracted = catalog.extract("ReactJS, Node.js, JavaScript, My SQL and RESTful APIs experience");
        assertTrue(extracted.keySet().containsAll(java.util.Set.of("React", "Node.js", "JavaScript", "MySQL", "REST API")));
        assertFalse(extracted.containsKey("Java"));
    }

    @Test
    void extractsSkillsFollowedByNormalSentencePunctuation() {
        var extracted = catalog.extract("Experience with AWS. Linux and Node.js are required.");
        assertTrue(extracted.keySet().containsAll(java.util.Set.of("AWS", "Linux", "Node.js")));
    }
}
