package com.aihye.toeicspeaking.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "evaluations")
@Getter
@Setter
@NoArgsConstructor
public class Evaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "evaluation_id")
    private Integer evaluationId;

    @Column(name = "response_id", nullable = false)
    private Integer responseId;

    @Column(name = "score")
    private Integer score;

    @Column(name = "score_comment", columnDefinition = "TEXT")
    private String scoreComment;

    @Column(name = "target_analysis", columnDefinition = "TEXT")
    private String targetAnalysis;

    @Column(name = "target_tips", columnDefinition = "TEXT")
    private String targetTips;

    @Column(name = "strengths_text", columnDefinition = "TEXT")
    private String strengthsText;

    @Column(name = "feedback_text", columnDefinition = "TEXT")
    private String feedbackText;

    @Column(name = "corrected_answers", columnDefinition = "TEXT")
    private String correctedAnswers;

    @Column(name = "key_expressions", columnDefinition = "TEXT")
    private String keyExpressions;

    @Column(name = "evaluated_at", updatable = false)
    private LocalDateTime evaluatedAt;

    @PrePersist
    protected void onCreate() {
        this.evaluatedAt = LocalDateTime.now();
    }
}
