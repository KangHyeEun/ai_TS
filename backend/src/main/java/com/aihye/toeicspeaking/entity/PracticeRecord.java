package com.aihye.toeicspeaking.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "practice_records")
@Getter
@Setter
@NoArgsConstructor
public class PracticeRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "question_id")
    private Integer questionId;

    @Column(name = "part_id", nullable = false)
    private Integer partId;

    @Column(name = "part_title", nullable = false, length = 100)
    private String partTitle;

    @Column(name = "practice_mode", nullable = false, length = 20)
    private String practiceMode;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
