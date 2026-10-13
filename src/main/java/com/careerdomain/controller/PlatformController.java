package com.careerdomain.controller;

import com.careerdomain.entity.*;

import com.careerdomain.graph.CareerGraphService;
import com.careerdomain.repository.*;
import com.careerdomain.service.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/** REST boundary only; extraction, graph persistence, assessments and matching live in services. */
@RestController
public class PlatformController {
    private final UserRepository users; private final UserProfileRepository profiles; private final UserSkillRepository userSkills; private final JobDescriptionRepository jobs;
    private final JobDescriptionService jobService; private final AssessmentRepository assessments; private final AssessmentResultRepository results;
    private final SkillCatalogService catalog;
    private final SkillGraphService graph;
    private final CareerGraphService careerGraph;
    private final LearningPathService learningPathService;
    private final RecommendationService recommendationService;

    public PlatformController(UserRepository users, UserProfileRepository profiles, UserSkillRepository userSkills, JobDescriptionRepository jobs, JobDescriptionService jobService,
                              AssessmentRepository assessments, AssessmentResultRepository results, SkillCatalogService catalog,
                              SkillGraphService graph,
                                  CareerGraphService careerGraph,
                                  LearningPathService learningPathService,
                                  RecommendationService recommendationService) {
        this.users = users; this.profiles = profiles; this.userSkills = userSkills; this.jobs = jobs; this.jobService = jobService; this.assessments = assessments;
        this.results = results;
        this.catalog = catalog;
        this.graph = graph;
        this.careerGraph = careerGraph;
        this.learningPathService = learningPathService;
        this.recommendationService = recommendationService;
    }
    public record ProfileRequest(@Size(max = 500) String experience, @Size(max = 3000) String education, @Size(max = 3000) String careerGoal) { }
    public record SkillRequest(@NotBlank @Size(max = 100) String skillName) { }
    public record JobRequest(@NotBlank @Size(max = 200) String title, @NotBlank @Size(max = 15000) String description) { }
    public record AnswerRequest(@NotBlank String selectedAnswer) { }

    private User current(UserDetails principal) {
        if (principal == null) throw new org.springframework.security.access.AccessDeniedException("Authentication is required.");
        return users.findByEmail(principal.getUsername()).orElseThrow(() -> new IllegalArgumentException("User account not found."));
    }
    private UserProfile profile(User user) { return profiles.findByUserId(user.getId()).orElseGet(() -> { UserProfile value = new UserProfile(); value.setUser(user); value.setSkills(""); return profiles.save(value); }); }
    private Set<String> skills(User user) {
        Set<String> response = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        userSkills.findByUserId(user.getId()).stream().map(UserSkill::getSkillName).filter(Objects::nonNull).map(catalog::normalize).forEach(response::add);
        // Keep profiles created by earlier versions readable; the next skill write
        // migrates this legacy comma-separated representation into user_skills.
        if (response.isEmpty()) {
            String stored = profile(user).getSkills();
            if (stored != null && !stored.isBlank()) Arrays.stream(stored.split(",")).filter(s -> !s.isBlank()).map(catalog::normalize).forEach(response::add);
        }
        return response;
    }
    private void saveSkills(User user, Collection<String> values) {
        Set<String> normalized = new TreeSet<>(String.CASE_INSENSITIVE_ORDER); values.forEach(value -> normalized.add(catalog.normalize(value)));
        userSkills.findByUserId(user.getId()).forEach(userSkills::delete);
        normalized.forEach(skill -> userSkills.save(new UserSkill(user, skill, 50, 80, false)));
        UserProfile value = profile(user); value.setSkills(String.join(",", normalized)); profiles.save(value);
        graph.syncUserSkills(user.getId(), user.getName(), new ArrayList<>(normalized));
    }
    private Map<String, Object> profileResponse(User user) {
        UserProfile p = profile(user); Set<String> currentSkills = skills(user);
        int completed = (user.getName().isBlank() ? 0 : 1) + (p.getEducation() == null || p.getEducation().isBlank() ? 0 : 1)
                + (p.getExperience() == null || p.getExperience().isBlank() ? 0 : 1) + (p.getCareerGoal() == null || p.getCareerGoal().isBlank() ? 0 : 1)
                + (currentSkills.isEmpty() ? 0 : 1);
        return Map.of("id", user.getId(), "name", user.getName(), "email", user.getEmail(), "experience", Optional.ofNullable(p.getExperience()).orElse(""),
                "education", Optional.ofNullable(p.getEducation()).orElse(""), "careerGoal", Optional.ofNullable(p.getCareerGoal()).orElse(""), "skills", currentSkills,
                "completionPercentage", completed * 20);
    }

    @GetMapping({"/users/me/profile", "/users/profile"}) public Map<String, Object> getProfile(@AuthenticationPrincipal UserDetails principal) { return profileResponse(current(principal)); }
    @PutMapping({"/users/me/profile", "/users/profile"}) public Map<String, Object> updateProfile(@AuthenticationPrincipal UserDetails principal, @Valid @RequestBody ProfileRequest request) {
        User user = current(principal); UserProfile p = profile(user); p.setExperience(nullToEmpty(request.experience())); p.setEducation(nullToEmpty(request.education())); p.setCareerGoal(nullToEmpty(request.careerGoal())); profiles.save(p);
        graph.syncUserSkills(user.getId(), user.getName(), new ArrayList<>(skills(user))); return profileResponse(user);
    }
    @GetMapping({"/users/me/skills", "/users/skills"}) public Set<String> listSkills(@AuthenticationPrincipal UserDetails principal) { return skills(current(principal)); }
    @PostMapping({"/users/me/skills", "/users/skills"}) public ResponseEntity<Set<String>> addSkill(@AuthenticationPrincipal UserDetails principal, @Valid @RequestBody SkillRequest request) {
        User user = current(principal); Set<String> all = skills(user); all.add(catalog.normalize(request.skillName())); saveSkills(user, all); return ResponseEntity.status(HttpStatus.CREATED).body(skills(user));
    }
    @DeleteMapping({"/users/me/skills", "/users/skills"}) public Set<String> removeSkill(@AuthenticationPrincipal UserDetails principal, @Valid @RequestBody SkillRequest request) {
        User user = current(principal); Set<String> all = skills(user); all.remove(catalog.normalize(request.skillName())); saveSkills(user, all); return skills(user);
    }
    @DeleteMapping({"/users/me/skills/{skillName}", "/users/skills/{skillName}"}) public Set<String> removeSkillByName(@AuthenticationPrincipal UserDetails principal, @PathVariable String skillName) {
        User user = current(principal); Set<String> all = skills(user); all.remove(catalog.normalize(skillName)); saveSkills(user, all); return skills(user);
    }

    @PostMapping("/jobs") public ResponseEntity<Map<String, Object>> createJob(@Valid @RequestBody JobRequest request) { return ResponseEntity.status(HttpStatus.CREATED).body(jobResponse(jobService.createJobDescription(newJob(request)))); }
    @PutMapping("/jobs/{id}") public Map<String, Object> updateJob(@PathVariable Long id, @Valid @RequestBody JobRequest request) { return jobResponse(jobService.updateJobDescription(id, request.title(), request.description())); }
    @DeleteMapping("/jobs/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void deleteJob(@PathVariable Long id) { jobService.deleteJobDescription(id); }
    @GetMapping("/jobs") public List<Map<String, Object>> listJobs(@RequestParam(required = false) String search) {
        List<JobDescription> result = search == null || search.isBlank() ? jobs.findAll() : jobService.searchByTitle(search.trim()); return result.stream().map(this::jobResponse).toList();
    }
    @GetMapping("/jobs/{id}") public Map<String, Object> job(@PathVariable Long id) { return jobResponse(findJob(id)); }
    @GetMapping("/jobs/{id}/graph") public Map<String, Object> graph(@PathVariable Long id, @AuthenticationPrincipal UserDetails principal) { findJob(id); User user = current(principal); return graph.graphData(id, user.getId()); }


    // ============================================================
    // CAREER KNOWLEDGE GRAPH INTELLIGENCE
    // ============================================================

    @GetMapping("/graph/bfs/{skill}")
    public Map<String, Object> breadthFirstGraph(
            @PathVariable String skill,
            @RequestParam(defaultValue = "3") int depth) {

        int safeDepth = Math.max(1, Math.min(depth, 10));

        var traversal = careerGraph.breadthFirstTraversal(skill, safeDepth);
        return Map.of(
                "algorithm", "Breadth-First Search",
                "startSkill", traversal.startSkill(),
                "maxDepth", safeDepth,
                "nodes", traversal.nodes(),
                "edges", traversal.edges(),
                "explanation", "Nearby skills are explored first, then the search moves outward one level at a time."
        );
    }

    @GetMapping("/graph/dfs/{skill}")
    public Map<String, Object> depthFirstGraph(
            @PathVariable String skill,
            @RequestParam(defaultValue = "3") int depth) {

        int safeDepth = Math.max(1, Math.min(depth, 10));

        var traversal = careerGraph.depthFirstTraversal(skill, safeDepth);
        return Map.of(
                "algorithm", "Depth-First Search",
                "startSkill", traversal.startSkill(),
                "maxDepth", safeDepth,
                "nodes", traversal.nodes(),
                "edges", traversal.edges(),
                "explanation", "One connected branch is explored as deeply as possible before another branch is visited."
        );
    }

    @GetMapping("/graph/shortest-path")
    public Map<String, Object> shortestPath(
            @RequestParam("from") String from,
            @RequestParam("to") String to) {

        Map<String, Object> path = new LinkedHashMap<>(careerGraph.shortestSkillPath(from, to));
        path.put("startSkill", from.trim());
        path.put("targetSkill", to.trim());
        path.put("explanation", Boolean.TRUE.equals(path.get("found"))
                ? "This route uses the fewest available knowledge-graph connections between the selected skills."
                : "No connected learning route was found for these skills.");
        return path;
    }

    @GetMapping("/graph/skill-gap")
    public Map<String, Object> graphSkillGap(
            @RequestParam("current") String current,
            @RequestParam("required") String required) {

        List<String> currentSkills = Arrays.stream(current.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(catalog::normalize)
                .toList();

        List<String> requiredSkills = Arrays.stream(required.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(catalog::normalize)
                .toList();

        return careerGraph.skillGap(currentSkills, requiredSkills);
    }


    @GetMapping("/learning-path/jobs/{jobId}")
    public LearningPathService.LearningPath learningPath(
            @PathVariable Long jobId,
            @AuthenticationPrincipal UserDetails principal) {

        JobDescription job = findJob(jobId);
        User user = current(principal);

        return learningPathService.generate(
                job,
                skills(user)
        );
    }

    @GetMapping({"/recommendations/me", "/recommendations"}) public List<RecommendationService.Recommendation> recommendations(@AuthenticationPrincipal UserDetails principal) { return recommendationService.recommend(jobs.findAll(), skills(current(principal))); }
    @GetMapping({"/skill-gaps/jobs/{id}", "/skill-gaps/{id}"}) public RecommendationService.Recommendation skillGap(@PathVariable Long id, @AuthenticationPrincipal UserDetails principal) { return recommendationService.score(findJob(id), skills(current(principal)), jobs.findAll()); }
    @GetMapping({"/insights/me", "/insights"}) public Map<String, Object> insights(@AuthenticationPrincipal UserDetails principal) {
        User user = current(principal); List<RecommendationService.Recommendation> recommendations = recommendationService.recommend(jobs.findAll(), skills(user));
        Set<String> learn = recommendations.stream().limit(3).flatMap(r -> r.missingSkills().stream()).collect(Collectors.toCollection(() -> new TreeSet<>(String.CASE_INSENSITIVE_ORDER)));
        List<AssessmentResult> history = results.findByUserIdOrderByIdDesc(user.getId()); double average = history.stream().mapToDouble(AssessmentResult::getScore).average().orElse(0);
        Set<String> weakSkills = history.stream().filter(r -> !Boolean.TRUE.equals(r.getCorrect())).map(r -> r.getAssessment().getSkill()).collect(Collectors.toCollection(() -> new TreeSet<>(String.CASE_INSENSITIVE_ORDER)));
        return Map.of("careerGoal", Optional.ofNullable(profile(user).getCareerGoal()).orElse(""), "strongestSkills", skills(user), "recommendedSkills", learn,
                "suitableRoles", recommendations.stream().filter(r -> r.matchPercentage() > 0).limit(3).map(RecommendationService.Recommendation::title).toList(),
                "bestMatch", recommendations.isEmpty() ? Map.of() : recommendations.getFirst(), "assessmentAverage", round(average), "assessmentWeaknesses", weakSkills);
    }

    @GetMapping("/assessments/job/{jobId}") public List<Map<String, Object>> assessments(@PathVariable Long jobId) {
        findJob(jobId); return assessments.findByJobDescriptionId(jobId).stream().map(a -> Map.<String, Object>of("id", a.getId(), "skill", a.getSkill(), "question", a.getQuestion(), "optionA", a.getOptionA(), "optionB", a.getOptionB(), "optionC", a.getOptionC(), "optionD", a.getOptionD())).toList();
    }
    @PostMapping("/assessments/{id}/submit") public ResponseEntity<Map<String, Object>> submit(@PathVariable Long id, @Valid @RequestBody AnswerRequest answer, @AuthenticationPrincipal UserDetails principal) {
        Assessment assessment = assessments.findById(id).orElseThrow(() -> new IllegalArgumentException("Assessment question not found.")); String selected = answer.selectedAnswer().trim().toUpperCase(Locale.ROOT);
        if (!Set.of("A", "B", "C", "D").contains(selected)) throw new IllegalArgumentException("selectedAnswer must be A, B, C, or D.");
        boolean correct = selected.equals(assessment.getCorrectAnswer()); AssessmentResult result = new AssessmentResult(); result.setAssessment(assessment); result.setUser(current(principal)); result.setSelectedAnswer(selected); result.setCorrect(correct); result.setScore(correct ? 100d : 0d); result.setResultSummary(correct ? "Correct: you demonstrated " + assessment.getSkill() + " knowledge." : "Not quite. Review " + assessment.getSkill() + " and try another assessment."); results.save(result);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("assessmentId", id, "skill", assessment.getSkill(), "score", result.getScore(), "correct", correct, "resultSummary", result.getResultSummary()));
    }
    @GetMapping({"/assessment-results/me", "/assessment-results"}) public List<Map<String, Object>> history(@AuthenticationPrincipal UserDetails principal) { return results.findByUserIdOrderByIdDesc(current(principal).getId()).stream().map(r -> Map.<String, Object>of("id", r.getId(), "assessmentId", r.getAssessment().getId(), "skill", r.getAssessment().getSkill(), "score", r.getScore(), "correct", r.getCorrect(), "selectedAnswer", r.getSelectedAnswer(), "resultSummary", r.getResultSummary(), "completedAt", String.valueOf(r.getCompletedAt()))).toList(); }

    private JobDescription newJob(JobRequest request) { JobDescription job = new JobDescription(); job.setTitle(request.title().trim()); job.setDescription(request.description().trim()); return job; }
    private JobDescription findJob(Long id) { return jobs.findById(id).orElseThrow(() -> new IllegalArgumentException("Job not found.")); }
    private Map<String, Object> jobResponse(JobDescription job) { return Map.of("id", job.getId(), "title", job.getTitle(), "description", job.getDescription(), "createdAt", String.valueOf(job.getCreatedAt()), "skills", job.getExtractedSkills().stream().map(ExtractedSkill::getSkillName).toList(), "extractedSkills", job.getExtractedSkills().stream().map(s -> Map.of("name", s.getSkillName(), "confidence", s.getConfidence())).toList()); }
    private String nullToEmpty(String value) { return value == null ? "" : value.trim(); }
    private double round(double value) { return Math.round(value * 100.0) / 100.0; }
}
