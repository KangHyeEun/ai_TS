package com.aihye.toeicspeaking.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "questions")
@Getter
@Setter
@NoArgsConstructor
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "question_id")
    private Integer questionId;

    @Column(name = "part_number", nullable = false)
    private Integer partNumber;

    @Column(name = "question_type", nullable = false, length = 50)
    private String questionType;

    @Column(name = "content_text", nullable = false, columnDefinition = "TEXT")
    private String contentText;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "audio_url", length = 500)
    private String audioUrl;

    @Column(name = "preparation_time", nullable = false)
    private Integer preparationTime;

    @Column(name = "response_time", nullable = false)
    private Integer responseTime;

    @Column(name = "is_set", nullable = false)
    private Boolean isSet = false;

    @Column(name = "set_order")
    private Integer setOrder;
}
