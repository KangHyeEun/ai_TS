package com.aihye.toeicspeaking.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FeedbackRequest {
    private Integer partId;
    private String questionText;
    private String userAnswer;
    private Integer targetScore;
    private Integer responseTime;
}
