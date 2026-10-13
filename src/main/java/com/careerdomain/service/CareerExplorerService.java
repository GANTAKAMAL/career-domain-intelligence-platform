package com.careerdomain.service;

import com.careerdomain.entity.Domain;
import com.careerdomain.entity.JobRole;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class CareerExplorerService {

    private final Driver neo4jDriver;

    public CareerExplorerService(Driver neo4jDriver) {
        this.neo4jDriver = neo4jDriver;
    }

    /**
     * Basic role response used by role listings.
     */
    public Map<String, Object> toJobRoleResponse(
            JobRole role
    ) {

        Map<String, Object> response =
                new LinkedHashMap<>();

        response.put(
                "id",
                role.getId()
        );

        response.put(
                "name",
                role.getName()
        );

        response.put(
                "description",
                role.getDescription()
        );

        Domain domain = role.getDomain();

        if (domain != null) {

            Map<String, Object> domainResponse =
                    new LinkedHashMap<>();

            domainResponse.put(
                    "id",
                    domain.getId()
            );

            domainResponse.put(
                    "name",
                    domain.getName()
            );

            response.put(
                    "domain",
                    domainResponse
            );

        } else {

            response.put(
                    "domain",
                    null
            );
        }

        return response;
    }

    /**
     * Detailed role response including knowledge-graph
     * skill information.
     */
    public Map<String, Object> toDetailedJobRoleResponse(
            JobRole role
    ) {

        Map<String, Object> response =
                toJobRoleResponse(role);

        Map<String, Object> skillData =
                getRoleSkills(role);

        response.put(
                "skillCount",
                skillData.get("skillCount")
        );

        response.put(
                "skills",
                skillData.get("skills")
        );

        response.put(
                "graphAvailable",
                skillData.get("graphAvailable")
        );

        return response;
    }

    /**
     * Fetches skills connected to a JobRole node in Neo4j.
     */
    public Map<String, Object> getRoleSkills(
            JobRole role
    ) {

        List<Map<String, Object>> skills =
                new ArrayList<>();

        boolean graphAvailable = false;

        try (Session session = neo4jDriver.session()) {

            List<Record> records = session.run(
                    """
                    MATCH (r:JobRole {name: $roleName})
                          -[rel:REQUIRES]->(s:Skill)
                    RETURN
                        s.name AS name,
                        rel.importance AS importance,
                        rel.source AS source
                    ORDER BY
                        CASE
                            WHEN rel.importance = 'ESSENTIAL'
                            THEN 0
                            ELSE 1
                        END,
                        name
                    """,
                    Map.of(
                            "roleName",
                            role.getName()
                    )
            ).list();

            graphAvailable = !records.isEmpty();

            for (Record record : records) {

                Map<String, Object> skill =
                        new LinkedHashMap<>();

                String name =
                        record.get("name").isNull()
                                ? null
                                : record.get("name").asString();

                String importance =
                        record.get("importance").isNull()
                                ? null
                                : record.get("importance")
                                        .asString();

                String source =
                        record.get("source").isNull()
                                ? null
                                : record.get("source")
                                        .asString();

                skill.put(
                        "name",
                        name
                );

                skill.put(
                        "importance",
                        importance
                );

                skill.put(
                        "source",
                        source
                );

                skills.add(skill);
            }
        }

        Map<String, Object> response =
                new LinkedHashMap<>();

        response.put(
                "roleId",
                role.getId()
        );

        response.put(
                "roleName",
                role.getName()
        );

        response.put(
                "skillCount",
                skills.size()
        );

        response.put(
                "skills",
                skills
        );

        response.put(
                "graphAvailable",
                graphAvailable
        );

        return response;
    }
}