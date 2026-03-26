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

    @Column(name = "strengths_text", columnDefinition = "TEXT")
    private String strengthsText;

    @Column(name = "feedback_text", columnDefinition = "TEXT")
    private String feedbackText;

    @Column(name = "grammar_corrections", columnDefinition = "TEXT")
    private String grammarCorrections;

    @Column(name = "evaluated_at", updatable = false)
    private LocalDateTime evaluatedAt;

    @PrePersist
    protected void onCreate() {
        this.evaluatedAt = LocalDateTime.now();
    }
}
