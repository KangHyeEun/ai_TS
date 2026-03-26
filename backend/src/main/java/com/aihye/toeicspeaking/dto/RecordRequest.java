package com.aihye.toeicspeaking.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RecordRequest {
    private Integer userId;
    private Integer questionId;
    private Integer partId;
    private String partTitle;
    private String practiceMode;
}
