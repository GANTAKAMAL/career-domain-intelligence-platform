package com.careerdomain.service;

import com.careerdomain.entity.ExtractedSkill;
import com.careerdomain.entity.JobDescription;
import com.careerdomain.entity.LearningResource;
import com.careerdomain.graph.CareerGraphService;
import com.careerdomain.repository.LearningResourceRepository;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class LearningPathService {

    public record LearningStep(
            int step,
            String skill,
            String status,
            double priority,
            List<String> graphPath,
            int graphDistance,
            List<Resource> resources,
            String explanation
    ) {
    }

    public record Resource(
            Long id,
            String title,
            String type,
            String url,
            String difficulty,
            String estimatedHours,
            String description
    ) {
    }

    public record LearningPath(
            Long jobId,
            String jobTitle,
            double currentReadiness,
            List<String> currentSkills,
            List<String> requiredSkills,
            List<String> missingSkills,
            List<LearningStep> steps,
            String explanation
    ) {
    }

    private final LearningResourceRepository resourceRepository;
    private final CareerGraphService careerGraph;
    private final SkillCatalogService catalog;

    public LearningPathService(
            LearningResourceRepository resourceRepository,
            CareerGraphService careerGraph,
            SkillCatalogService catalog) {

        this.resourceRepository = resourceRepository;
        this.careerGraph = careerGraph;
        this.catalog = catalog;
    }

    public LearningPath generate(
            JobDescription job,
            Collection<String> currentSkills) {

        Set<String> current = normalize(currentSkills);

        Set<String> required = job.getExtractedSkills()
                .stream()
                .map(ExtractedSkill::getSkillName)
                .filter(Objects::nonNull)
                .map(catalog::normalize)
                .collect(Collectors.toCollection(
                        () -> new TreeSet<>(String.CASE_INSENSITIVE_ORDER)
                ));

        List<String> missing = required.stream()
                .filter(skill -> !containsIgnoreCase(current, skill))
                .toList();

        List<String> matched = required.stream()
                .filter(skill -> containsIgnoreCase(current, skill))
                .toList();

        double readiness = required.isEmpty()
                ? 0.0
                : (matched.size() * 100.0) / required.size();

        /*
         * Calculate the intelligence score for every missing skill.
         */
        Map<String, MissingSkillScore> scores = new TreeMap<>(
                String.CASE_INSENSITIVE_ORDER
        );

        for (String skill : missing) {
            MissingSkillScore score =
                    scoreMissingSkill(
                            skill,
                            current,
                            required,
                            missing
                    );

            scores.put(skill, score);
        }

        /*
         * ----------------------------------------------------------
         * Build the prerequisite dependency graph.
         *
         * Only prerequisites that are ALSO missing from the target
         * job are used for ordering.
         *
         * Example:
         *
         * Docker -> Kubernetes
         * AWS    -> Kubernetes
         *
         * Therefore Kubernetes cannot be placed before Docker/AWS.
         * ----------------------------------------------------------
         */
        Map<String, Set<String>> dependencies =
                buildDependencies(missing);

        /*
         * ----------------------------------------------------------
         * Topological ordering.
         *
         * At every stage we select only skills whose missing
         * prerequisites have already been scheduled.
         *
         * Among currently available skills, the intelligence score
         * determines the order.
         * ----------------------------------------------------------
         */
        List<MissingSkillScore> ordered =
                prerequisiteAwareOrdering(
                        missing,
                        dependencies,
                        scores
                );

        List<LearningStep> steps = new ArrayList<>();

        int position = 1;

        for (MissingSkillScore score : ordered) {

            List<LearningResource> resources =
                    resourceRepository.findBySkillNameIgnoreCase(
                            score.skill()
                    );

            List<Resource> mappedResources =
                    resources.stream()
                            .map(this::resource)
                            .limit(5)
                            .toList();

            String explanation =
                    buildExplanation(
                            score,
                            mappedResources,
                            dependencies.getOrDefault(
                                    score.skill(),
                                    Set.of()
                            )
                    );

            steps.add(
                    new LearningStep(
                            position++,
                            score.skill(),
                            "MISSING",
                            score.priority(),
                            score.graphPath(),
                            score.graphDistance(),
                            mappedResources,
                            explanation
                    )
            );
        }

        return new LearningPath(
                job.getId(),
                job.getTitle(),
                round(readiness),

                current.stream()
                        .sorted(String.CASE_INSENSITIVE_ORDER)
                        .toList(),

                required.stream()
                        .sorted(String.CASE_INSENSITIVE_ORDER)
                        .toList(),

                missing.stream()
                        .sorted(String.CASE_INSENSITIVE_ORDER)
                        .toList(),

                steps,

                "Learning path generated using job requirements, user skills, "
                        + "knowledge-graph connectivity, explicit skill prerequisites, "
                        + "learning-resource coverage, and deterministic priority scoring. "
                        + "Prerequisite relationships are enforced so dependent skills "
                        + "are scheduled only after their required missing skills."
        );
    }

    /*
     * ============================================================
     * BUILD DEPENDENCY GRAPH
     * ============================================================
     */

    private Map<String, Set<String>> buildDependencies(
            List<String> missing) {

        Map<String, Set<String>> dependencies =
                new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        Set<String> missingSet =
                new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

        missingSet.addAll(missing);

        for (String skill : missing) {

            List<String> prerequisites =
                    careerGraph.getPrerequisites(skill);

            Set<String> relevantPrerequisites =
                    prerequisites.stream()
                            .filter(Objects::nonNull)
                            .map(String::trim)
                            .filter(value -> !value.isBlank())
                            .filter(value ->
                                    containsIgnoreCase(
                                            missingSet,
                                            value
                                    )
                            )
                            .collect(Collectors.toCollection(
                                    () -> new TreeSet<>(
                                            String.CASE_INSENSITIVE_ORDER
                                    )
                            ));

            dependencies.put(
                    skill,
                    relevantPrerequisites
            );
        }

        return dependencies;
    }

    /*
     * ============================================================
     * PREREQUISITE-AWARE TOPOLOGICAL ORDERING
     * ============================================================
     */

    private List<MissingSkillScore> prerequisiteAwareOrdering(
            List<String> missing,
            Map<String, Set<String>> dependencies,
            Map<String, MissingSkillScore> scores) {

        /*
         * Remaining prerequisites for every skill.
         */
        Map<String, Set<String>> remaining =
                new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        for (String skill : missing) {

            remaining.put(
                    skill,
                    new TreeSet<>(
                            dependencies.getOrDefault(
                                    skill,
                                    Set.of()
                            )
                    )
            );
        }

        List<MissingSkillScore> ordered =
                new ArrayList<>();

        Set<String> completed =
                new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

        while (ordered.size() < missing.size()) {

            /*
             * Find all currently executable skills.
             *
             * A skill is executable when every missing prerequisite
             * has already been completed.
             */
            List<MissingSkillScore> available =
                    remaining.entrySet()
                            .stream()
                            .filter(entry ->
                                    !completed.contains(entry.getKey())
                            )
                            .filter(entry ->
                                    completed.containsAll(
                                            entry.getValue()
                                    )
                            )
                            .map(entry ->
                                    scores.get(entry.getKey())
                            )
                            .filter(Objects::nonNull)
                            .sorted(
                                    Comparator
                                            .comparingDouble(
                                                    MissingSkillScore::priority
                                            )
                                            .reversed()

                                            /*
                                             * Prefer shorter graph
                                             * routes when priorities
                                             * are equal.
                                             */
                                            .thenComparingInt(
                                                    score ->
                                                            score.graphDistance()
                                                                    < 0
                                                                    ? Integer.MAX_VALUE
                                                                    : score.graphDistance()
                                            )

                                            /*
                                             * Prefer skills that have
                                             * learning resources.
                                             */
                                            .thenComparing(
                                                    MissingSkillScore::resourceCount,
                                                    Comparator.reverseOrder()
                                            )

                                            /*
                                             * Final deterministic tie-break.
                                             */
                                            .thenComparing(
                                                    MissingSkillScore::skill,
                                                    String.CASE_INSENSITIVE_ORDER
                                            )
                            )
                            .toList();

            if (!available.isEmpty()) {

                for (MissingSkillScore score : available) {

                    if (completed.contains(score.skill())) {
                        continue;
                    }

                    ordered.add(score);
                    completed.add(score.skill());
                }

                continue;
            }

            /*
             * ------------------------------------------------------
             * Cycle protection.
             *
             * A malformed knowledge graph should never cause the API
             * to hang. If there is a cyclic prerequisite dependency,
             * append the remaining skills deterministically using
             * their priority score.
             * ------------------------------------------------------
             */
            List<MissingSkillScore> remainingScores =
                    missing.stream()
                            .filter(skill ->
                                    !completed.contains(skill)
                            )
                            .map(scores::get)
                            .filter(Objects::nonNull)
                            .sorted(
                                    Comparator
                                            .comparingDouble(
                                                    MissingSkillScore::priority
                                            )
                                            .reversed()
                                            .thenComparing(
                                                    MissingSkillScore::skill,
                                                    String.CASE_INSENSITIVE_ORDER
                                            )
                            )
                            .toList();

            ordered.addAll(remainingScores);
            break;
        }

        return ordered;
    }

    /*
     * ============================================================
     * SKILL SCORING
     * ============================================================
     */

    private MissingSkillScore scoreMissingSkill(
            String target,
            Set<String> current,
            Set<String> required,
            List<String> missing) {

        Map<String, Object> bestPath = null;

        int bestDistance =
                Integer.MAX_VALUE;

        /*
         * Find the closest route from any skill already owned by
         * the user.
         */
        for (String existing : current) {

            Map<String, Object> result =
                    careerGraph.shortestSkillPath(
                            existing,
                            target
                    );

            if (!Boolean.TRUE.equals(
                    result.get("found"))) {

                continue;
            }

            Object distanceValue =
                    result.get("distance");

            int distance =
                    distanceValue instanceof Number number
                            ? number.intValue()
                            : Integer.MAX_VALUE;

            if (distance < bestDistance) {

                bestDistance = distance;
                bestPath = result;
            }
        }

        List<String> path =
                bestPath == null
                        ? List.of()
                        : extractPath(
                                bestPath.get("path")
                        );

        int distance =
                bestDistance == Integer.MAX_VALUE
                        ? -1
                        : bestDistance;

        /*
         * ----------------------------------------------------------
         * 1. Graph proximity
         * ----------------------------------------------------------
         */
        double proximityScore =
                distance < 0
                        ? 0.0
                        : 1.0 / (1.0 + distance);

        /*
         * ----------------------------------------------------------
         * 2. Required-skill connectivity
         * ----------------------------------------------------------
         *
         * Count how many other required skills can be reached.
         */
        int connectedRequiredSkills = 0;

        for (String other : required) {

            if (other.equalsIgnoreCase(target)) {
                continue;
            }

            Map<String, Object> result =
                    careerGraph.shortestSkillPath(
                            target,
                            other
                    );

            if (Boolean.TRUE.equals(
                    result.get("found"))) {

                connectedRequiredSkills++;
            }
        }

        int denominator =
                Math.max(
                        required.size() - 1,
                        1
                );

        double connectivityScore =
                connectedRequiredSkills
                        / (double) denominator;

        /*
         * ----------------------------------------------------------
         * 3. Learning-resource coverage
         * ----------------------------------------------------------
         */
        List<LearningResource> resources =
                resourceRepository.findBySkillNameIgnoreCase(
                        target
                );

        double resourceScore =
                resources.isEmpty()
                        ? 0.0
                        : Math.min(
                                resources.size() / 3.0,
                                1.0
                        );

        /*
         * ----------------------------------------------------------
         * 4. Gap urgency
         * ----------------------------------------------------------
         */
        double urgencyScore =
                missing.isEmpty()
                        ? 0.0
                        : 1.0 / missing.size();

        /*
         * ----------------------------------------------------------
         * Final explainable score.
         *
         * Proximity      = 40%
         * Connectivity   = 35%
         * Resources      = 15%
         * Urgency        = 10%
         * ----------------------------------------------------------
         */
        double priority =
                (proximityScore * 0.40)
                        + (connectivityScore * 0.35)
                        + (resourceScore * 0.15)
                        + (urgencyScore * 0.10);

        return new MissingSkillScore(
                target,
                round(priority * 100.0),
                distance,
                path,
                connectedRequiredSkills,
                resources.size()
        );
    }

    /*
     * ============================================================
     * EXPLANATION
     * ============================================================
     */

    private String buildExplanation(
            MissingSkillScore score,
            List<Resource> resources,
            Set<String> prerequisites) {

        if (score.graphDistance() < 0) {

            return "This skill is missing from the current profile and "
                    + "has no connected graph path from the user's existing skills. "
                    + buildPrerequisiteMessage(prerequisites)
                    + " It remains in the learning plan as an independent gap.";
        }

        String resourceMessage =
                resources.isEmpty()
                        ? "No curated learning resource is currently mapped to this skill."
                        : resources.size()
                                + " curated learning resource(s) are available.";

        String prerequisiteMessage =
                buildPrerequisiteMessage(prerequisites);

        return "This skill is "
                + score.graphDistance()
                + " graph step(s) from the closest current skill and connects to "
                + score.connectedRequiredSkills()
                + " other required skill(s). "
                + prerequisiteMessage
                + " "
                + resourceMessage
                + " Its priority is determined using explainable "
                + "knowledge-graph connectivity, prerequisite dependencies, "
                + "and learning-resource coverage.";
    }

    private String buildPrerequisiteMessage(
            Set<String> prerequisites) {

        if (prerequisites == null || prerequisites.isEmpty()) {

            return "It has no missing prerequisite among the target job's required skills.";
        }

        return "Its missing prerequisite skill(s) are "
                + String.join(
                        ", ",
                        prerequisites.stream()
                                .sorted(
                                        String.CASE_INSENSITIVE_ORDER
                                )
                                .toList()
                )
                + ".";
    }

    /*
     * ============================================================
     * PATH / RESOURCE HELPERS
     * ============================================================
     */

    private List<String> extractPath(Object value) {

        if (!(value instanceof Collection<?> collection)) {
            return List.of();
        }

        return collection.stream()
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .toList();
    }

    private Resource resource(
            LearningResource resource) {

        return new Resource(
                resource.getId(),
                resource.getTitle(),
                resource.getType(),
                resource.getUrl(),
                resource.getDifficulty(),
                resource.getEstimatedHours(),
                resource.getDescription()
        );
    }

    /*
     * ============================================================
     * NORMALIZATION
     * ============================================================
     */

    private Set<String> normalize(
            Collection<String> values) {

        if (values == null) {

            return new TreeSet<>(
                    String.CASE_INSENSITIVE_ORDER
            );
        }

        Set<String> normalized =
                new TreeSet<>(
                        String.CASE_INSENSITIVE_ORDER
                );

        values.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(catalog::normalize)
                .forEach(normalized::add);

        return normalized;
    }

    private boolean containsIgnoreCase(
            Collection<String> values,
            String target) {

        return values.stream()
                .anyMatch(
                        value ->
                                value.equalsIgnoreCase(target)
                );
    }

    private double round(double value) {

        return Math.round(value * 100.0) / 100.0;
    }

    /*
     * ============================================================
     * INTERNAL SCORE
     * ============================================================
     */

    private record MissingSkillScore(
            String skill,
            double priority,
            int graphDistance,
            List<String> graphPath,
            int connectedRequiredSkills,
            int resourceCount
    ) {
    }
}