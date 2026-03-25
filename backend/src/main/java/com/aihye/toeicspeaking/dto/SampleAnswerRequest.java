package com.aihye.toeicspeaking.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class SampleAnswerRequest {
    private Integer partId;
    private String questionText;
    private String info;
    private List<String> subQuestions;
    private Integer targetScore;
    private Integer responseTime;
}
