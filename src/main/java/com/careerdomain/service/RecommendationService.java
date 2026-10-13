package com.careerdomain.service;

import com.careerdomain.entity.ExtractedSkill;
import com.careerdomain.entity.JobDescription;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/** A small, explainable TF-IDF / cosine-similarity recommendation model. */
@Service
public class RecommendationService {
    private final SkillCatalogService catalog;
    public RecommendationService(SkillCatalogService catalog) { this.catalog = catalog; }

    public record Recommendation(Long jobId, String title, double matchPercentage, double similarityScore,
                                 Set<String> matchingSkills, Set<String> missingSkills, Set<String> requiredSkills,
                                 double gapPercentage, String explanation) { }

    public List<Recommendation> recommend(Collection<JobDescription> jobs, Collection<String> rawUserSkills) {
        Set<String> userSkills = normalized(rawUserSkills);
        Map<String, Long> documentFrequency = new HashMap<>();
        jobs.forEach(job -> required(job).forEach(skill -> documentFrequency.merge(skill, 1L, Long::sum)));
        int documentCount = jobs.size();
        return jobs.stream().map(job -> score(job, userSkills, documentFrequency, documentCount))
                .sorted(Comparator.comparingDouble(Recommendation::similarityScore).reversed()
                        .thenComparing(Recommendation::title, String.CASE_INSENSITIVE_ORDER)).toList();
    }

    public Recommendation score(JobDescription job, Collection<String> rawUserSkills, Collection<JobDescription> corpus) {
        Set<String> userSkills = normalized(rawUserSkills); Map<String, Long> df = new HashMap<>();
        corpus.forEach(candidate -> required(candidate).forEach(skill -> df.merge(skill, 1L, Long::sum)));
        return score(job, userSkills, df, corpus.size());
    }

    private Recommendation score(JobDescription job, Set<String> userSkills, Map<String, Long> df, int documents) {
        Set<String> required = required(job);
        Set<String> matching = new TreeSet<>(String.CASE_INSENSITIVE_ORDER); matching.addAll(required);
        matching.retainAll(userSkills);
        Set<String> missing = new TreeSet<>(String.CASE_INSENSITIVE_ORDER); missing.addAll(required); missing.removeAll(matching);
        if (required.isEmpty()) return new Recommendation(job.getId(), job.getTitle(), 0, 0, matching, missing, required, 100,
                "No supported skills were extracted from this description, so no similarity score can be calculated.");

        Set<String> dimensions = new HashSet<>(userSkills); dimensions.addAll(required);
        double dot = 0, userMagnitude = 0, jobMagnitude = 0;
        for (String skill : dimensions) {
            double idf = Math.log((documents + 1.0) / (df.getOrDefault(skill, 0L) + 1.0)) + 1.0;
            double userWeight = userSkills.contains(skill) ? idf : 0;
            double jobWeight = required.contains(skill) ? idf : 0;
            dot += userWeight * jobWeight; userMagnitude += userWeight * userWeight; jobMagnitude += jobWeight * jobWeight;
        }
        double cosine = userMagnitude == 0 || jobMagnitude == 0 ? 0 : dot / Math.sqrt(userMagnitude * jobMagnitude);
        double percentage = round(cosine * 100); double gap = round(100.0 * missing.size() / required.size());
        return new Recommendation(job.getId(), job.getTitle(), percentage, round(cosine), matching, missing, required, gap,
                matching.size() + " of " + required.size() + " required skills are currently in your profile. "
                        + "The match score uses explainable TF-IDF weighted skill similarity (cosine similarity), not a trained ML model.");
    }

    private Set<String> normalized(Collection<String> rawSkills) {
        Set<String> values = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        if (rawSkills != null) rawSkills.stream().filter(Objects::nonNull).filter(s -> !s.isBlank()).map(catalog::normalize).forEach(values::add);
        return values;
    }
    private Set<String> required(JobDescription job) {
        return job.getExtractedSkills().stream().map(ExtractedSkill::getSkillName).map(catalog::normalize)
                .collect(Collectors.toCollection(() -> new TreeSet<>(String.CASE_INSENSITIVE_ORDER)));
    }
    private double round(double value) { return Math.round(value * 100.0) / 100.0; }
}
