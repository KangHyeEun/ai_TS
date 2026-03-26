package com.aihye.toeicspeaking.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "study_stats", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "study_date"})
})
@Getter
@Setter
@NoArgsConstructor
public class StudyStats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "stat_id")
    private Integer statId;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "study_date", nullable = false)
    private LocalDate studyDate;

    @Column(name = "completed_questions_count")
    private Integer completedQuestionsCount = 0;

    @Column(name = "average_score", precision = 5, scale = 2)
    private BigDecimal averageScore = BigDecimal.ZERO;
}
