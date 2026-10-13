package com.careerdomain.controller;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/skills")
public class SkillExplorerController {

    private final Driver neo4jDriver;

    public SkillExplorerController(Driver neo4jDriver) {
        this.neo4jDriver = neo4jDriver;
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> searchSkills(
            @RequestParam(required = false) String search
    ) {

        String query = search == null ? "" : search.trim();

        try (Session session = neo4jDriver.session()) {

            List<Record> records;

            if (query.isBlank()) {

                records = session.run(
                        """
                        MATCH (s:Skill)
                        RETURN s.name AS name
                        ORDER BY name
                        LIMIT 100
                        """
                ).list();

            } else {

                records = session.run(
                        """
                        MATCH (s:Skill)
                        WHERE toLower(s.name)
                              CONTAINS toLower($search)
                        RETURN s.name AS name
                        ORDER BY name
                        LIMIT 100
                        """,
                        Map.of("search", query)
                ).list();
            }

            List<Map<String, Object>> response = new ArrayList<>();

            for (Record record : records) {

                if (record.get("name").isNull()) {
                    continue;
                }

                String skillName = record
                        .get("name")
                        .asString();

                if (skillName.isBlank()) {
                    continue;
                }

                Map<String, Object> item =
                        new LinkedHashMap<>();

                item.put("name", skillName);

                response.add(item);
            }

            return ResponseEntity.ok(response);
        }
    }
}