package com.careerdomain.service;

import com.careerdomain.entity.Domain;
import com.careerdomain.entity.JobRole;
import com.careerdomain.entity.Technology;
import com.careerdomain.repository.DomainRepository;
import com.careerdomain.repository.JobRoleRepository;
import com.careerdomain.repository.TechnologyRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@ConditionalOnProperty(
        name = "app.esco.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class EscoDataService {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final Driver neo4jDriver;

    private final DomainRepository domainRepository;
    private final JobRoleRepository jobRoleRepository;
    private final TechnologyRepository technologyRepository;

    private final String baseUrl;
    private final String language;
    private final int resultsPerQuery;
    private final int maxQueries;

    public EscoDataService(
            DomainRepository domainRepository,
            JobRoleRepository jobRoleRepository,
            TechnologyRepository technologyRepository,
            Driver neo4jDriver,
            @Value("${app.esco.base-url:https://ec.europa.eu/esco/api}")
            String baseUrl,
            @Value("${app.esco.language:en}")
            String language,
            @Value("${app.esco.results-per-query:5}")
            int resultsPerQuery,
            @Value("${app.esco.max-queries:12}")
            int maxQueries
    ) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();

        this.objectMapper = new ObjectMapper();
        this.neo4jDriver = neo4jDriver;

        this.domainRepository = domainRepository;
        this.jobRoleRepository = jobRoleRepository;
        this.technologyRepository = technologyRepository;

        this.baseUrl = baseUrl;
        this.language = language;
        this.resultsPerQuery = Math.max(1, resultsPerQuery);
        this.maxQueries = Math.max(1, maxQueries);
    }

    public Map<String, Object> refreshKnowledgeBase() {

        long start = System.currentTimeMillis();

        List<String> searchQueries = List.of(
                "software developer",
                "backend developer",
                "web developer",
                "data engineer",
                "data scientist",
                "machine learning",
                "cloud engineer",
                "devops",
                "cybersecurity",
                "database administrator",
                "systems analyst",
                "software architect"
        );

        int queriesProcessed = Math.min(
                maxQueries,
                searchQueries.size()
        );

        Set<String> occupationUris =
                new LinkedHashSet<>();

        Map<String, JsonNode> searchResults =
                new LinkedHashMap<>();

        Set<String> importedSkills =
                new LinkedHashSet<>();

        Set<String> relationshipKeys =
                new LinkedHashSet<>();

        List<String> errors =
                new ArrayList<>();

        int occupationsImported = 0;
        int skillOccurrences = 0;
        int relationshipsImported = 0;
        int neo4jSkillsProcessed = 0;
        int neo4jRelationshipsProcessed = 0;

        /*
         * =========================================================
         * STEP 1
         * SEARCH ESCO OCCUPATIONS
         * =========================================================
         */
        for (int i = 0; i < queriesProcessed; i++) {

            String query =
                    searchQueries.get(i);

            try {

                String json =
                        restClient.get()
                                .uri(uriBuilder ->
                                        uriBuilder
                                                .path("/search")
                                                .queryParam(
                                                        "text",
                                                        query
                                                )
                                                .queryParam(
                                                        "type",
                                                        "occupation"
                                                )
                                                .queryParam(
                                                        "language",
                                                        language
                                                )
                                                .queryParam(
                                                        "limit",
                                                        resultsPerQuery
                                                )
                                                .build()
                                )
                                .accept(
                                        MediaType.APPLICATION_JSON
                                )
                                .retrieve()
                                .body(String.class);

                JsonNode root =
                        objectMapper.readTree(json);

                JsonNode results =
                        root.path("_embedded")
                                .path("results");

                if (!results.isArray()
                        && root.isArray()) {

                    results = root;
                }

                if (!results.isArray()) {
                    continue;
                }

                for (JsonNode result : results) {

                    String uri =
                            result
                                    .path("uri")
                                    .asText(null);

                    if (isBlank(uri)) {
                        continue;
                    }

                    occupationUris.add(uri);

                    searchResults.putIfAbsent(
                            uri,
                            result
                    );
                }

            } catch (Exception ex) {

                errors.add(
                        "Search failed for '"
                                + query
                                + "': "
                                + rootCause(ex)
                );
            }
        }

        /*
         * =========================================================
         * STEP 2
         * FETCH OCCUPATION DETAILS
         * =========================================================
         */
        for (String occupationUri :
                occupationUris) {

            try {

                String json =
                        restClient.get()
                                .uri(uriBuilder ->
                                        uriBuilder
                                                .path(
                                                        "/resource/occupation"
                                                )
                                                .queryParam(
                                                        "uri",
                                                        occupationUri
                                                )
                                                .queryParam(
                                                        "language",
                                                        language
                                                )
                                                .build()
                                )
                                .accept(
                                        MediaType.APPLICATION_JSON
                                )
                                .retrieve()
                                .body(String.class);

                JsonNode occupation =
                        objectMapper.readTree(json);

                /*
                 * -------------------------------------------------
                 * OCCUPATION TITLE
                 * -------------------------------------------------
                 */
                String occupationTitle =
                        occupation
                                .path("title")
                                .asText(null);

                if (isBlank(occupationTitle)) {

                    JsonNode searchResult =
                            searchResults.get(
                                    occupationUri
                            );

                    if (searchResult != null) {

                        occupationTitle =
                                searchResult
                                        .path("title")
                                        .asText(null);
                    }
                }

                if (isBlank(occupationTitle)) {
                    occupationTitle = "ESCO Occupation";
                }

                occupationTitle =
                        occupationTitle.trim();

                /*
                 * -------------------------------------------------
                 * DESCRIPTION
                 * -------------------------------------------------
                 */
                String description =
                        extractDescription(
                                occupation
                        );

                /*
                 * -------------------------------------------------
                 * DOMAIN
                 * -------------------------------------------------
                 */
                Domain domain =
                        resolveDomain(
                                occupationTitle
                        );

                /*
                 * -------------------------------------------------
                 * MYSQL JOB ROLE
                 * -------------------------------------------------
                 */
                JobRole role =
                        findJobRole(
                                occupationTitle
                        );

                if (role == null) {

                    role =
                            new JobRole();

                    role.setName(
                            occupationTitle
                    );
                }

                role.setDescription(
                        description
                );

                role.setDomain(
                        domain
                );

                role =
                        jobRoleRepository.save(
                                role
                        );

                occupationsImported++;

                /*
                 * =================================================
                 * STEP 3
                 * EXTRACT ESCO SKILLS
                 *
                 * We do NOT depend solely on:
                 *
                 * occupation.path("hasEssentialSkill")
                 *
                 * Instead, recursively inspect the complete
                 * response for objects whose URI points to
                 * /esco/skill/.
                 * =================================================
                 */
                List<JsonNode> skillNodes =
                        new ArrayList<>();

                collectEscoSkills(
                        occupation,
                        skillNodes
                );

                /*
                 * Also explicitly collect the two known ESCO
                 * arrays. This makes the importer tolerant of
                 * different ESCO response structures.
                 */
                collectSkillArray(
                        occupation.path(
                                "hasEssentialSkill"
                        ),
                        skillNodes
                );

                collectSkillArray(
                        occupation.path(
                                "hasOptionalSkill"
                        ),
                        skillNodes
                );

                /*
                 * Qualified property names.
                 */
                collectSkillArray(
                        occupation.path(
                                "http://data.europa.eu/esco/model#hasEssentialSkill"
                        ),
                        skillNodes
                );

                collectSkillArray(
                        occupation.path(
                                "http://data.europa.eu/esco/model#hasOptionalSkill"
                        ),
                        skillNodes
                );

                /*
                 * Deduplicate skill objects.
                 */
                Map<String, JsonNode> uniqueSkills =
                        new LinkedHashMap<>();

                for (JsonNode skill :
                        skillNodes) {

                    String skillUri =
                            skill.path("uri")
                                    .asText(null);

                    String skillTitle =
                            skill.path("title")
                                    .asText(null);

                    String key;

                    if (!isBlank(skillUri)) {

                        key = skillUri;

                    } else if (!isBlank(skillTitle)) {

                        key =
                                "title:"
                                        + skillTitle
                                        .toLowerCase(
                                                Locale.ROOT
                                        );

                    } else {

                        continue;
                    }

                    uniqueSkills.putIfAbsent(
                            key,
                            skill
                    );
                }

                /*
                 * =================================================
                 * STEP 4
                 * IMPORT EACH SKILL
                 * =================================================
                 */
                for (JsonNode skill :
                        uniqueSkills.values()) {

                    String skillUri =
                            skill.path("uri")
                                    .asText(null);

                    String skillName =
                            skill.path("title")
                                    .asText(null);

                    /*
                     * If title is missing, fetch the real ESCO
                     * skill resource.
                     */
                    if (isBlank(skillName)
                            && !isBlank(skillUri)) {

                        skillName =
                                fetchSkillTitle(
                                        skillUri
                                );
                    }

                    if (isBlank(skillName)) {
                        continue;
                    }

                    skillName =
                            normalizeSkillName(
                                    skillName
                            );

                    importedSkills.add(
                            skillName
                    );

                    skillOccurrences++;

                    /*
                     * -------------------------------------------------
                     * MYSQL TECHNOLOGY
                     * -------------------------------------------------
                     */
                    Technology technology =
                            findTechnology(
                                    skillName
                            );

                    if (technology == null) {

                        technology =
                                new Technology();

                        technology.setName(
                                skillName
                        );
                    }

                    technology.setCategory(
                            getSkillCategory(
                                    skill
                            )
                    );

                    technology.setDescription(
                            "Real-world skill imported "
                                    + "from the ESCO Web Service API."
                    );

                    technologyRepository.save(
                            technology
                    );

                    relationshipsImported++;

                    /*
                     * -------------------------------------------------
                     * NEO4J
                     * -------------------------------------------------
                     */
                    boolean graphSaved =
                            saveNeo4jRelation(
                                    occupationTitle,
                                    occupationUri,
                                    description,
                                    skillName,
                                    skillUri,
                                    skill
                            );

                    if (graphSaved) {

                        neo4jSkillsProcessed++;

                        String relationshipKey =
                                occupationUri
                                        + "::"
                                        + skillName;

                        if (relationshipKeys.add(
                                relationshipKey
                        )) {

                            neo4jRelationshipsProcessed++;
                        }
                    }
                }

            } catch (Exception ex) {

                errors.add(
                        "Occupation import failed for '"
                                + occupationUri
                                + "': "
                                + rootCause(ex)
                );
            }
        }

        /*
         * =========================================================
         * RESULT
         * =========================================================
         */
        Map<String, Object> response =
                new LinkedHashMap<>();

        response.put(
                "source",
                "ESCO Web Service API"
        );

        response.put(
                "apiVersion",
                "default"
        );

        response.put(
                "language",
                language
        );

        response.put(
                "baseUrl",
                baseUrl
        );

        response.put(
                "queriesProcessed",
                queriesProcessed
        );

        response.put(
                "uniqueOccupationsFound",
                occupationUris.size()
        );

        response.put(
                "occupationsImported",
                occupationsImported
        );

        response.put(
                "uniqueSkillsImported",
                importedSkills.size()
        );

        response.put(
                "skillOccurrencesImported",
                skillOccurrences
        );

        response.put(
                "relationshipsImported",
                relationshipsImported
        );

        response.put(
                "neo4jSkillsProcessed",
                neo4jSkillsProcessed
        );

        response.put(
                "neo4jRelationshipsProcessed",
                neo4jRelationshipsProcessed
        );

        response.put(
                "durationMs",
                System.currentTimeMillis()
                        - start
        );

        response.put(
                "status",
                errors.isEmpty()
                        ? "SUCCESS"
                        : "PARTIAL_SUCCESS"
        );

        response.put(
                "errors",
                errors
        );

        return response;
    }

    /*
     * =============================================================
     * RECURSIVE ESCO SKILL DISCOVERY
     * =============================================================
     */
    private void collectEscoSkills(
            JsonNode node,
            List<JsonNode> target
    ) {

        if (node == null
                || node.isMissingNode()
                || node.isNull()) {

            return;
        }

        if (node.isObject()) {

            JsonNode uri =
                    node.get("uri");

            JsonNode title =
                    node.get("title");

            /*
             * Verified ESCO skill objects have:
             *
             * uri = http://data.europa.eu/esco/skill/...
             * title = ...
             */
            if (uri != null
                    && uri.isTextual()
                    && title != null
                    && title.isTextual()) {

                String uriValue =
                        uri.asText();

                String titleValue =
                        title.asText();

                if (uriValue.contains(
                        "/esco/skill/"
                )
                        && !uriValue.isBlank()
                        && !titleValue.isBlank()) {

                    target.add(node);
                }
            }

            var fields =
                    node.fields();

            while (fields.hasNext()) {

                Map.Entry<String, JsonNode> entry =
                        fields.next();

                collectEscoSkills(
                        entry.getValue(),
                        target
                );
            }

        } else if (node.isArray()) {

            for (JsonNode child : node) {

                collectEscoSkills(
                        child,
                        target
                );
            }
        }
    }

    /*
     * =============================================================
     * EXPLICIT SKILL ARRAY
     * =============================================================
     */
    private void collectSkillArray(
            JsonNode node,
            List<JsonNode> target
    ) {

        if (node == null
                || !node.isArray()) {

            return;
        }

        for (JsonNode child : node) {

            if (child != null
                    && child.isObject()) {

                target.add(child);
            }
        }
    }

    /*
     * =============================================================
     * FETCH ACTUAL ESCO SKILL
     * =============================================================
     */
    private String fetchSkillTitle(
            String skillUri
    ) {

        if (isBlank(skillUri)) {
            return null;
        }

        try {

            String json =
                    restClient
                            .get()
                            .uri(uriBuilder ->
                                    uriBuilder
                                            .path(
                                                    "/resource/skill"
                                            )
                                            .queryParam(
                                                    "uri",
                                                    skillUri
                                            )
                                            .queryParam(
                                                    "language",
                                                    language
                                            )
                                            .build()
                            )
                            .accept(
                                    MediaType.APPLICATION_JSON
                            )
                            .retrieve()
                            .body(String.class);

            JsonNode skill =
                    objectMapper.readTree(
                            json
                    );

            String title =
                    skill.path("title")
                            .asText(null);

            if (!isBlank(title)) {
                return title;
            }

            return extractPreferredLabel(
                    skill
            );

        } catch (Exception ex) {

            return null;
        }
    }

    /*
     * =============================================================
     * MYSQL JOB ROLE LOOKUP
     * =============================================================
     */
    private JobRole findJobRole(
            String name
    ) {

        List<JobRole> roles =
                jobRoleRepository.findAll();

        for (JobRole role : roles) {

            if (role.getName() != null
                    && role.getName()
                    .equalsIgnoreCase(name)) {

                return role;
            }
        }

        return null;
    }

    /*
     * =============================================================
     * MYSQL TECHNOLOGY LOOKUP
     * =============================================================
     */
    private Technology findTechnology(
            String name
    ) {

        List<Technology> technologies =
                technologyRepository.findAll();

        for (Technology technology :
                technologies) {

            if (technology.getName() != null
                    && technology.getName()
                    .equalsIgnoreCase(name)) {

                return technology;
            }
        }

        return null;
    }

    /*
     * =============================================================
     * DOMAIN RESOLUTION
     * =============================================================
     */
    private Domain resolveDomain(
            String occupationTitle
    ) {

        String normalized =
                occupationTitle
                        .toLowerCase(
                                Locale.ROOT
                        );

        String domainName =
                "Software Engineering";

        if (normalized.contains("data")
                || normalized.contains(
                        "scientist"
                )
                || normalized.contains(
                        "machine learning"
                )
                || normalized.contains(
                        "artificial intelligence"
                )) {

            domainName =
                    "Data & Artificial Intelligence";

        } else if (
                normalized.contains("security")
                        || normalized.contains(
                                "cyber"
                        )
                        || normalized.contains(
                                "penetration"
                        )
                        || normalized.contains(
                                "information security"
                        )
        ) {

            domainName =
                    "Cybersecurity";
        }

        Domain domain = null;

        for (Domain existing :
                domainRepository.findAll()) {

            if (existing.getName() != null
                    && existing.getName()
                    .equalsIgnoreCase(
                            domainName
                    )) {

                domain = existing;
                break;
            }
        }

        if (domain == null) {

            domain =
                    new Domain();

            domain.setName(
                    domainName
            );

            domain.setDescription(
                    "Career domain enriched using "
                            + "real-world ESCO occupational data."
            );

            domain =
                    domainRepository.save(
                            domain
                    );
        }

        return domain;
    }

    /*
     * =============================================================
     * NEO4J GRAPH INSERTION
     * =============================================================
     */
    private boolean saveNeo4jRelation(
            String roleName,
            String roleUri,
            String description,
            String skillName,
            String skillUri,
            JsonNode skill
    ) {

        try (Session session =
                     neo4jDriver.session()) {

            Map<String, Object> parameters =
                    new LinkedHashMap<>();

            parameters.put(
                    "roleName",
                    roleName
            );

            parameters.put(
                    "roleUri",
                    roleUri
            );

            parameters.put(
                    "description",
                    description == null
                            ? ""
                            : description
            );

            parameters.put(
                    "skillName",
                    skillName
            );

            parameters.put(
                    "skillUri",
                    skillUri == null
                            ? ""
                            : skillUri
            );

            parameters.put(
                    "skillType",
                    getSkillCategory(
                            skill
                    )
            );

            session.run(
                    """
                    MERGE (r:JobRole {name: $roleName})
                    SET r.escoUri = $roleUri,
                        r.source = 'ESCO',
                        r.description = $description

                    MERGE (s:Skill {name: $skillName})
                    SET s.escoUri = $skillUri,
                        s.source = 'ESCO',
                        s.skillType = $skillType

                    MERGE (r)-[:REQUIRES {source: 'ESCO'}]->(s)
                    """,
                    parameters
            ).consume();

            return true;

        } catch (Exception ex) {

            return false;
        }
    }

    /*
     * =============================================================
     * SKILL CATEGORY
     * =============================================================
     */
    private String getSkillCategory(
            JsonNode skill
    ) {

        String type =
                skill.path("skillType")
                        .asText("");

        if (type.contains("knowledge")) {
            return "ESCO Knowledge";
        }

        if (type.contains("skill")) {
            return "ESCO Skill";
        }

        return "ESCO Skill";
    }

    /*
     * =============================================================
     * DESCRIPTION
     * =============================================================
     */
    private String extractDescription(
            JsonNode occupation
    ) {

        JsonNode description =
                occupation.path(
                        "description"
                );

        if (description.isTextual()) {
            return description.asText();
        }

        if (!description.isObject()) {
            return "";
        }

        JsonNode languageNode =
                description.path(
                        language
                );

        if (languageNode.isObject()) {

            String literal =
                    languageNode
                            .path("literal")
                            .asText(null);

            if (!isBlank(literal)) {
                return literal;
            }
        }

        JsonNode en =
                description.path("en");

        if (en.isObject()) {

            String literal =
                    en.path("literal")
                            .asText(null);

            if (!isBlank(literal)) {
                return literal;
            }
        }

        JsonNode literal =
                description.path("literal");

        if (literal.isTextual()) {
            return literal.asText();
        }

        return "";
    }

    /*
     * =============================================================
     * PREFERRED LABEL
     * =============================================================
     */
    private String extractPreferredLabel(
            JsonNode node
    ) {

        JsonNode preferred =
                node.path(
                        "preferredLabel"
                );

        if (!preferred.isObject()) {
            return null;
        }

        JsonNode localized =
                preferred.path(
                        language
                );

        if (localized.isTextual()) {
            return localized.asText();
        }

        JsonNode en =
                preferred.path("en");

        if (en.isTextual()) {
            return en.asText();
        }

        JsonNode literal =
                preferred.path("literal");

        if (literal.isTextual()) {
            return literal.asText();
        }

        return null;
    }

    /*
     * =============================================================
     * NORMALIZATION
     * =============================================================
     */
    private String normalizeSkillName(
            String value
    ) {

        String normalized =
                value
                        .trim()
                        .replaceAll(
                                "\\s+",
                                " "
                        );

        if (normalized.length() > 500) {

            normalized =
                    normalized.substring(
                            0,
                            500
                    );
        }

        return normalized;
    }

    /*
     * =============================================================
     * UTILITIES
     * =============================================================
     */
    private boolean isBlank(
            String value
    ) {

        return value == null
                || value.isBlank();
    }

    private String rootCause(
            Exception ex
    ) {

        Throwable current = ex;

        String message =
                ex.getMessage();

        while (
                current.getCause() != null
        ) {

            current =
                    current.getCause();

            if (!isBlank(
                    current.getMessage()
            )) {

                message =
                        current.getMessage();
            }
        }

        if (isBlank(message)) {

            return ex.getClass()
                    .getSimpleName();
        }

        return message;
    }
}