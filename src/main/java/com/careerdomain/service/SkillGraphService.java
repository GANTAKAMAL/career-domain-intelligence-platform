package com.careerdomain.service;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Service;

import com.careerdomain.entity.JobNode;
import com.careerdomain.entity.SkillNode;
import org.springframework.data.neo4j.core.Neo4jClient;

@Service
public class SkillGraphService {

    private final Neo4jClient neo4jClient;

    public SkillGraphService(Neo4jClient neo4jClient) {
        this.neo4jClient = neo4jClient;
    }

    public SkillNode createOrGetSkill(String skillName) {

        if (skillName == null || skillName.trim().isEmpty()) {
            return null;
        }

        String cleanSkillName = skillName.trim();

        neo4jClient.query("MERGE (:Skill {name: $name})")
                .bind(cleanSkillName).to("name")
                .run();
        return new SkillNode(cleanSkillName);
    }

    public JobNode createJobGraph(
            Long jobId,
            String title,
            List<String> skillNames) {

        List<String> normalizedSkills = skillNames == null ? List.of() : skillNames.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(name -> !name.isEmpty())
                .distinct()
                .toList();

        // Remove stale edges before merging the current description; MERGE keeps
        // node and relationship identity stable across repeat processing.
        neo4jClient.query("""
                MERGE (job:Job {id: $jobId})
                SET job.title = $title
                WITH job
                OPTIONAL MATCH (job)-[old:REQUIRES]->(:Skill)
                DELETE old
                WITH job
                UNWIND $skillNames AS skillName
                MERGE (skill:Skill {name: skillName})
                MERGE (job)-[:REQUIRES]->(skill)
                """)
                .bind(jobId).to("jobId")
                .bind(title).to("title")
                .bind(normalizedSkills).to("skillNames")
                .run();

        JobNode jobNode = new JobNode(jobId, title);
        normalizedSkills.forEach(name -> jobNode.addSkill(new SkillNode(name)));
        return jobNode;
    }

    public void syncUserSkills(Long userId, String name, List<String> skillNames) {
        List<String> skills = skillNames == null ? List.of() : skillNames.stream().filter(Objects::nonNull)
                .map(String::trim).filter(value -> !value.isEmpty()).distinct().toList();
        neo4jClient.query("""
                MERGE (user:User {id: $userId}) SET user.name = $name
                WITH user OPTIONAL MATCH (user)-[old:HAS_SKILL]->(:Skill) DELETE old
                WITH user UNWIND $skillNames AS skillName
                MERGE (skill:Skill {name: skillName})
                MERGE (user)-[:HAS_SKILL]->(skill)
                """).bind(userId).to("userId").bind(name).to("name").bind(skills).to("skillNames").run();
    }

    public void deleteJobGraph(Long jobId) {
        neo4jClient.query("MATCH (job:Job {id: $jobId}) DETACH DELETE job").bind(jobId).to("jobId").run();
    }

    /** Returns visualization data solely from Neo4j, never relational job skills. */
    public Map<String, Object> graphData(Long jobId, Long userId) {
        return neo4jClient.query("""
                MATCH (job:Job {id: $jobId})
                OPTIONAL MATCH (job)-[:REQUIRES]->(required:Skill)
                OPTIONAL MATCH (user:User {id: $userId})-[has:HAS_SKILL]->(owned:Skill)
                RETURN job.title AS title, collect(DISTINCT required.name) AS requiredSkills,
                       collect(DISTINCT owned.name) AS userSkills, user.name AS userName
                """).bind(jobId).to("jobId").bind(userId).to("userId").fetch().one().map(row -> {
                    List<String> required = strings(row.get("requiredSkills")); List<String> owned = strings(row.get("userSkills"));
                    List<Map<String, Object>> nodes = new java.util.ArrayList<>();
                    nodes.add(Map.of("id", "job-" + jobId, "label", String.valueOf(row.get("title")), "type", "Job"));
                    required.forEach(skill -> nodes.add(Map.of("id", "skill-" + skill, "label", skill, "type", "Skill")));
                    List<Map<String, Object>> relationships = required.stream().map(skill -> Map.<String, Object>of("from", "job-" + jobId, "to", "skill-" + skill, "type", "REQUIRES")).collect(java.util.stream.Collectors.toCollection(java.util.ArrayList::new));
                    if (row.get("userName") != null) {
                        nodes.add(Map.of("id", "user-" + userId, "label", String.valueOf(row.get("userName")), "type", "User"));
                        owned.forEach(skill -> { if (nodes.stream().noneMatch(n -> n.get("id").equals("skill-" + skill))) nodes.add(Map.of("id", "skill-" + skill, "label", skill, "type", "Skill")); relationships.add(Map.of("from", "user-" + userId, "to", "skill-" + skill, "type", "HAS_SKILL")); });
                    }
                    return Map.<String, Object>of("nodes", nodes, "relationships", relationships);
                }).orElseThrow(() -> new IllegalArgumentException("Job graph not found. Re-ingest the job to create its graph."));
    }

    private List<String> strings(Object value) {
        if (!(value instanceof List<?> list)) return List.of();
        return list.stream().filter(Objects::nonNull).map(String::valueOf).sorted(String.CASE_INSENSITIVE_ORDER).toList();
    }

    public JobNode getJobGraph(Long jobId) {
        return neo4jClient.query("""
                MATCH (job:Job {id: $jobId})
                OPTIONAL MATCH (job)-[:REQUIRES]->(skill:Skill)
                RETURN job.title AS title, collect(skill.name) AS skills
                """)
                .bind(jobId).to("jobId")
                .fetch()
                .one()
                .map(graph -> {
                    JobNode job = new JobNode(jobId, String.valueOf(graph.get("title")));
                    Object skills = graph.get("skills");
                    if (skills instanceof List<?> names) {
                        names.stream().filter(Objects::nonNull)
                                .map(String::valueOf)
                                .forEach(name -> job.addSkill(new SkillNode(name)));
                    }
                    return job;
                })
                .orElse(null);
    }
}
