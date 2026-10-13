package com.careerdomain.graph;

import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class CareerGraphService {

    /** Protect the API and browser from an unbounded ESCO traversal while
     * retaining a real, deterministic graph search result. */
    private static final int MAX_TRAVERSAL_NODES = 250;
    private static final int MAX_NEIGHBOURS_PER_NODE = 100;

    private final Neo4jClient neo4jClient;

    /** A traversal node carries the information needed to explain the real
     * search without making the web client infer levels from database IDs. */
    public record TraversalNode(String name, int level, int order, String parent) { }
    public record TraversalEdge(String from, String to) { }
    public record Traversal(String startSkill, List<TraversalNode> nodes,
                            List<TraversalEdge> edges) { }

    public CareerGraphService(Neo4jClient neo4jClient) {
        this.neo4jClient = neo4jClient;
    }

    /*
     * ============================================================
     * DOMAIN
     * ============================================================
     */

    public void upsertDomain(
            Long id,
            String name,
            String description) {

        neo4jClient.query("""
            MERGE (d:Domain {id: $id})
            SET d.name = $name,
                d.description = $description
            """)
                .bind(id).to("id")
                .bind(name).to("name")
                .bind(description).to("description")
                .run();
    }

    /*
     * ============================================================
     * JOB ROLE
     * ============================================================
     */

    public void upsertJobRole(
            Long id,
            String name,
            Long domainId) {

        neo4jClient.query("""
            MERGE (r:JobRole {id: $id})
            SET r.name = $name
            WITH r
            MATCH (d:Domain {id: $domainId})
            MERGE (d)-[:CONTAINS_ROLE]->(r)
            """)
                .bind(id).to("id")
                .bind(name).to("name")
                .bind(domainId).to("domainId")
                .run();
    }

    /*
     * ============================================================
     * ROLE -> SKILL
     * ============================================================
     */

    public void connectRoleSkills(
            Long roleId,
            Collection<String> skills) {

        List<String> cleaned = normalize(skills);

        neo4jClient.query("""
            MATCH (r:JobRole {id: $roleId})
            OPTIONAL MATCH (r)-[old:REQUIRES]->(:Skill)
            DELETE old
            WITH r
            UNWIND $skills AS skillName
            MERGE (s:Skill {name: skillName})
            MERGE (r)-[:REQUIRES]->(s)
            """)
                .bind(roleId).to("roleId")
                .bind(cleaned).to("skills")
                .run();
    }

    /*
     * ============================================================
     * SKILL -> TECHNOLOGY
     * ============================================================
     */

    public void connectSkillTechnology(
            String skillName,
            Collection<String> technologies) {

        if (skillName == null || skillName.isBlank()) {
            return;
        }

        List<String> cleaned = normalize(technologies);

        if (cleaned.isEmpty()) {
            return;
        }

        String skill = skillName.trim();

        neo4jClient.query("""
            MERGE (s:Skill {name: $skillName})
            WITH s
            OPTIONAL MATCH (s)-[old:ENABLED_BY]->(:Technology)
            DELETE old
            WITH s
            UNWIND $technologies AS technology
            MERGE (t:Technology {name: technology})
            MERGE (s)-[:ENABLED_BY]->(t)
            """)
                .bind(skill).to("skillName")
                .bind(cleaned).to("technologies")
                .run();
    }

    /*
     * ============================================================
     * SKILL -> CERTIFICATION
     * ============================================================
     */

    public void connectSkillCertification(
            String skillName,
            Collection<String> certifications) {

        if (skillName == null || skillName.isBlank()) {
            return;
        }

        List<String> cleaned = normalize(certifications);

        if (cleaned.isEmpty()) {
            return;
        }

        String skill = skillName.trim();

        neo4jClient.query("""
            MERGE (s:Skill {name: $skillName})
            WITH s
            OPTIONAL MATCH (s)-[old:HAS_CERTIFICATION]->(:Certification)
            DELETE old
            WITH s
            UNWIND $certifications AS certification
            MERGE (c:Certification {name: certification})
            MERGE (s)-[:HAS_CERTIFICATION]->(c)
            """)
                .bind(skill).to("skillName")
                .bind(cleaned).to("certifications")
                .run();
    }

    /*
     * ============================================================
     * SKILL -> LEARNING RESOURCE
     * ============================================================
     */

    public void connectSkillLearningResource(
            String skillName,
            Collection<String> resourceTitles) {

        if (skillName == null || skillName.isBlank()) {
            return;
        }

        List<String> cleaned = normalize(resourceTitles);

        if (cleaned.isEmpty()) {
            return;
        }

        String skill = skillName.trim();

        neo4jClient.query("""
            MERGE (s:Skill {name: $skillName})
            WITH s
            OPTIONAL MATCH (s)-[old:LEARNED_THROUGH]->(:LearningResource)
            DELETE old
            WITH s
            UNWIND $resources AS resourceTitle
            MERGE (r:LearningResource {title: resourceTitle})
            MERGE (s)-[:LEARNED_THROUGH]->(r)
            """)
                .bind(skill).to("skillName")
                .bind(cleaned).to("resources")
                .run();
    }

    /*
     * ============================================================
     * SKILL -> PREREQUISITE
     * ============================================================
     *
     * source -[:PREREQUISITE_FOR]-> target
     *
     * Example:
     *
     * Java -> Spring Boot
     * SQL -> MySQL
     * SQL -> PostgreSQL
     * Docker -> Kubernetes
     * AWS -> Kubernetes
     */

    public void connectSkillPrerequisites(
            String skillName,
            Collection<String> prerequisites) {

        if (skillName == null || skillName.isBlank()) {
            return;
        }

        List<String> cleaned = normalize(prerequisites);

        if (cleaned.isEmpty()) {
            return;
        }

        String targetSkill = skillName.trim();

        /*
         * IMPORTANT:
         * WITH is required between MERGE and OPTIONAL MATCH.
         */
        neo4jClient.query("""
            MERGE (target:Skill {name: $skillName})
            WITH target
            OPTIONAL MATCH (target)-[old:PREREQUISITE_FOR]->(:Skill)
            DELETE old
            WITH target
            UNWIND $prerequisites AS prerequisite
            MERGE (source:Skill {name: prerequisite})
            MERGE (source)-[:PREREQUISITE_FOR]->(target)
            """)
                .bind(targetSkill).to("skillName")
                .bind(cleaned).to("prerequisites")
                .run();
    }

    /*
     * ============================================================
     * DIRECT PREREQUISITES
     * ============================================================
     */

    public List<String> getPrerequisites(String skillName) {

        if (skillName == null || skillName.isBlank()) {
            return List.of();
        }

        return neo4jClient.query("""
            MATCH (source:Skill)-[:PREREQUISITE_FOR]->
                  (target:Skill {name: $skill})
            RETURN source.name AS name
            ORDER BY name
            """)
                .bind(skillName.trim()).to("skill")
                .fetch()
                .all()
                .stream()
                .map(row -> row.get("name"))
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
    }

    /*
     * ============================================================
     * DIRECT DEPENDENTS
     * ============================================================
     */

    public List<String> getDependentSkills(String skillName) {

        if (skillName == null || skillName.isBlank()) {
            return List.of();
        }

        return neo4jClient.query("""
            MATCH (source:Skill {name: $skill})-[:PREREQUISITE_FOR]->
                  (target:Skill)
            RETURN target.name AS name
            ORDER BY name
            """)
                .bind(skillName.trim()).to("skill")
                .fetch()
                .all()
                .stream()
                .map(row -> row.get("name"))
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
    }

    /*
     * ============================================================
     * USER PROFILE -> SKILLS
     * ============================================================
     */

    public void syncUserProfile(
            Long userId,
            String name,
            Collection<String> skills) {

        List<String> cleaned = normalize(skills);

        neo4jClient.query("""
            MERGE (u:User {id: $userId})
            SET u.name = $name
            WITH u
            OPTIONAL MATCH (u)-[old:HAS_SKILL]->(:Skill)
            DELETE old
            WITH u
            UNWIND $skills AS skillName
            MERGE (s:Skill {name: skillName})
            MERGE (u)-[:HAS_SKILL]->(s)
            """)
                .bind(userId).to("userId")
                .bind(name).to("name")
                .bind(cleaned).to("skills")
                .run();
    }

    /*
     * ============================================================
     * BFS
     * ============================================================
     */

    public List<String> breadthFirstSkills(
            String startSkill,
            int maxDepth) {

        if (startSkill == null || startSkill.isBlank()) {
            return List.of();
        }

        return breadthFirstTraversal(startSkill, maxDepth).nodes().stream()
                .map(TraversalNode::name)
                .filter(name -> !name.equalsIgnoreCase(startSkill.trim()))
                .toList();
    }

    /** Breadth-first traversal with explicit layers and discovery edges. */
    public Traversal breadthFirstTraversal(String startSkill, int maxDepth) {

        if (startSkill == null || startSkill.isBlank()) {
            return new Traversal("", List.of(), List.of());
        }

        String start = startSkill.trim();
        int depthLimit = Math.max(1, maxDepth);

        LinkedHashSet<String> visited = new LinkedHashSet<>();

        Map<String, Integer> depth = new HashMap<>();

        Deque<String> queue = new ArrayDeque<>();
        List<TraversalNode> nodes = new ArrayList<>();
        List<TraversalEdge> edges = new ArrayList<>();

        visited.add(start);
        depth.put(start, 0);
        queue.add(start);
        nodes.add(new TraversalNode(start, 0, 0, null));

        while (!queue.isEmpty() && visited.size() < MAX_TRAVERSAL_NODES) {

            String current = queue.removeFirst();

            int currentDepth = depth.get(current);

            if (currentDepth >= depthLimit) {
                continue;
            }

            for (String neighbour : skillNeighbours(current)) {

                if (visited.size() >= MAX_TRAVERSAL_NODES) {
                    break;
                }

                if (visited.add(neighbour)) {

                    depth.put(
                            neighbour,
                            currentDepth + 1
                    );

                    queue.addLast(neighbour);
                    edges.add(new TraversalEdge(current, neighbour));
                    nodes.add(new TraversalNode(neighbour, currentDepth + 1,
                            nodes.size(), current));
                }
            }
        }

        return new Traversal(start, List.copyOf(nodes), List.copyOf(edges));
    }

    /*
     * ============================================================
     * DFS
     * ============================================================
     */

    public List<String> depthFirstSkills(
            String startSkill,
            int maxDepth) {

        if (startSkill == null || startSkill.isBlank()) {
            return List.of();
        }

        return depthFirstTraversal(startSkill, maxDepth).nodes().stream()
                .map(TraversalNode::name)
                .filter(name -> !name.equalsIgnoreCase(startSkill.trim()))
                .toList();
    }

    /** Depth-first traversal with its actual discovery order and branches. */
    public Traversal depthFirstTraversal(String startSkill, int maxDepth) {

        if (startSkill == null || startSkill.isBlank()) {
            return new Traversal("", List.of(), List.of());
        }

        LinkedHashSet<String> visited =
                new LinkedHashSet<>();

        List<TraversalNode> nodes = new ArrayList<>();
        List<TraversalEdge> edges = new ArrayList<>();

        dfsTraversal(
                startSkill.trim(),
                Math.max(1, maxDepth),
                visited,
                0,
                null,
                nodes,
                edges
        );

        return new Traversal(startSkill.trim(), List.copyOf(nodes), List.copyOf(edges));
    }

    private void dfsTraversal(
            String skill,
            int maxDepth,
            Set<String> visited,
            int depth,
            String parent,
            List<TraversalNode> nodes,
            List<TraversalEdge> edges) {

        if (skill == null
                || skill.isBlank()
                || depth > maxDepth
                || visited.size() >= MAX_TRAVERSAL_NODES) {
            return;
        }

        String current = skill.trim();

        if (!visited.add(current)) {
            return;
        }

        nodes.add(new TraversalNode(current, depth, nodes.size(), parent));
        if (parent != null) {
            edges.add(new TraversalEdge(parent, current));
        }

        if (depth == maxDepth) {
            return;
        }

        for (String neighbour : skillNeighbours(current)) {

            dfsTraversal(
                    neighbour,
                    maxDepth,
                    visited,
                    depth + 1,
                    current,
                    nodes,
                    edges
            );
        }
    }

    /*
     * ============================================================
     * GRAPH NEIGHBOURS
     * ============================================================
     *
     * Skills can be connected indirectly through:
     *
     * Skill <-REQUIRES- JobRole -REQUIRES-> Skill
     *
     * Skill -ENABLED_BY-> Technology <-ENABLED_BY- Skill
     *
     * Skill -LEARNED_THROUGH-> LearningResource
     *
     * Skill -PREREQUISITE_FOR-> Skill
     */

    private List<String> skillNeighbours(String skill) {

        return neo4jClient.query("""
            MATCH (start:Skill {name: $skill})
            MATCH path =
                (start)-[
                    :REQUIRES
                    |ENABLED_BY
                    |LEARNED_THROUGH
                    |PREREQUISITE_FOR
                    *1..2
                ]-(target:Skill)
            WHERE target.name <> $skill
            RETURN DISTINCT target.name AS name
            ORDER BY name
            LIMIT $limit
            """)
                .bind(skill).to("skill")
                .bind(MAX_NEIGHBOURS_PER_NODE).to("limit")
                .fetch()
                .all()
                .stream()
                .map(row -> row.get("name"))
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .filter(value -> !value.isBlank())
                .filter(value ->
                        !value.equalsIgnoreCase(skill))
                .distinct()
                .toList();
    }

    /*
     * ============================================================
     * SHORTEST PATH
     * ============================================================
     */

    public Map<String, Object> shortestSkillPath(
        String sourceSkill,
        String targetSkill) {

    if (sourceSkill == null || targetSkill == null
            || sourceSkill.isBlank() || targetSkill.isBlank()) {
        return Map.of("found", false, "path", List.of(), "distance", -1);
    }

    String source = sourceSkill.trim();
    String target = targetSkill.trim();

    if (source.equalsIgnoreCase(target)) {
        return Map.of("found", true, "path", List.of(source), "distance", 0);
    }

    // Use exactly the same projected skill graph as BFS and DFS.
    Deque<String> queue = new ArrayDeque<>();
    Set<String> visited = new LinkedHashSet<>();
    Map<String, String> parent = new HashMap<>();

    queue.addLast(source);
    visited.add(source);

    String foundTarget = null;

    while (!queue.isEmpty() && foundTarget == null) {
        String current = queue.removeFirst();

        for (String neighbour : skillNeighbours(current)) {
            if (!visited.add(neighbour)) {
                continue;
            }

            parent.put(neighbour, current);

            if (neighbour.equalsIgnoreCase(target)) {
                foundTarget = neighbour;
                break;
            }

            queue.addLast(neighbour);
        }
    }

    if (foundTarget == null) {
        return Map.of("found", false, "path", List.of(), "distance", -1);
    }

    List<String> reversed = new ArrayList<>();
    String cursor = foundTarget;

    while (cursor != null) {
        reversed.add(cursor);
        cursor = parent.get(cursor);
    }

    List<String> path = new ArrayList<>(reversed.size());
    for (int i = reversed.size() - 1; i >= 0; i--) {
        path.add(reversed.get(i));
    }

    return Map.of(
            "found", true,
            "path", List.copyOf(path),
            "distance", path.size() - 1
    );
}

    /*
     * ============================================================
     * SKILL GAP
     * ============================================================
     */

    public Map<String, Object> skillGap(
            Collection<String> currentSkills,
            Collection<String> requiredSkills) {

        Set<String> current =
                normalize(currentSkills)
                        .stream()
                        .map(String::toLowerCase)
                        .collect(Collectors.toSet());

        Set<String> required =
                normalize(requiredSkills)
                        .stream()
                        .map(String::toLowerCase)
                        .collect(
                                Collectors.toCollection(
                                        TreeSet::new
                                )
                        );

        List<String> matched =
                required.stream()
                        .filter(current::contains)
                        .sorted()
                        .toList();

        List<String> missing =
                required.stream()
                        .filter(skill ->
                                !current.contains(skill))
                        .sorted()
                        .toList();

        double readiness =
                required.isEmpty()
                        ? 0.0
                        : (matched.size() * 100.0)
                                / required.size();

        return Map.of(
                "currentSkills",
                current.stream().sorted().toList(),

                "requiredSkills",
                required.stream().sorted().toList(),

                "matchedSkills",
                matched,

                "missingSkills",
                missing,

                "readinessPercentage",
                Math.round(readiness * 100.0) / 100.0
        );
    }

    /*
     * ============================================================
     * NORMALIZATION
     * ============================================================
     */

    private List<String> normalize(
            Collection<String> values) {

        if (values == null) {
            return List.of();
        }

        return values.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }
}
