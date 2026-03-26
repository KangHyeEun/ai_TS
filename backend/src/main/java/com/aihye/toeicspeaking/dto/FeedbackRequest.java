package com.aihye.toeicspeaking.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class FeedbackRequest {
    private Integer partId;
    private String questionText;
    private String userAnswer;
    private Integer targetScore;
    private Integer responseTime;
    // 세트형(Part 3,4) 문제별 상세 정보
    private List<SubQuestionFeedback> subQuestions;
    // Part 4 제공 정보
    private String info;

    @Getter
    @Setter
    public static class SubQuestionFeedback {
        private String question;
        private String answer;
        private Integer responseTime;
    }
}
