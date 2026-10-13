package com.careerdomain.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "assessment_results")
public class AssessmentResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "assessment_id", nullable = false)
    private Assessment assessment;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private Double score;

    @Column(length = 2000)
    private String resultSummary;

    @Column(nullable = false) private String selectedAnswer;
    @Column(nullable = false) private Boolean correct;
    private LocalDateTime completedAt;
    @PrePersist void prePersist() { completedAt = LocalDateTime.now(); }

    public AssessmentResult() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Assessment getAssessment() {
        return assessment;
    }

    public void setAssessment(Assessment assessment) {
        this.assessment = assessment;
    }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    public String getResultSummary() {
        return resultSummary;
    }

    public void setResultSummary(String resultSummary) {
        this.resultSummary = resultSummary;
    }
    public String getSelectedAnswer() { return selectedAnswer; }
    public void setSelectedAnswer(String selectedAnswer) { this.selectedAnswer = selectedAnswer; }
    public Boolean getCorrect() { return correct; }
    public void setCorrect(Boolean correct) { this.correct = correct; }
    public LocalDateTime getCompletedAt() { return completedAt; }
}
