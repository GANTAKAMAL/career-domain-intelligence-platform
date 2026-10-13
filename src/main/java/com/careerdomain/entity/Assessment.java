package com.careerdomain.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "assessments")
public class Assessment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String question;

    @Column(nullable = false)
    private String optionA;

    @Column(nullable = false)
    private String optionB;

    @Column(nullable = false)
    private String optionC;

    @Column(nullable = false)
    private String optionD;

    @Column(nullable = false)
    private String correctAnswer;

    @Column(nullable = false)
    private String skill;

    private LocalDateTime createdAt;

    @ManyToOne
    @JoinColumn(name = "job_description_id", nullable = false)
    private JobDescription jobDescription;


    // ==============================
    // Pre Persist
    // ==============================

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
    }


    // ==============================
    // Getters and Setters
    // ==============================

    public Long getId() {
        return id;
    }


    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }


    public String getOptionA() {
        return optionA;
    }

    public void setOptionA(String optionA) {
        this.optionA = optionA;
    }


    public String getOptionB() {
        return optionB;
    }

    public void setOptionB(String optionB) {
        this.optionB = optionB;
    }


    public String getOptionC() {
        return optionC;
    }

    public void setOptionC(String optionC) {
        this.optionC = optionC;
    }


    public String getOptionD() {
        return optionD;
    }

    public void setOptionD(String optionD) {
        this.optionD = optionD;
    }


    public String getCorrectAnswer() {
        return correctAnswer;
    }

    public String getSkill() { return skill; }
    public void setSkill(String skill) { this.skill = skill; }

    public void setCorrectAnswer(String correctAnswer) {
        this.correctAnswer = correctAnswer;
    }


    public LocalDateTime getCreatedAt() {
        return createdAt;
    }


    public JobDescription getJobDescription() {
        return jobDescription;
    }

    public void setJobDescription(JobDescription jobDescription) {
        this.jobDescription = jobDescription;
    }
}
