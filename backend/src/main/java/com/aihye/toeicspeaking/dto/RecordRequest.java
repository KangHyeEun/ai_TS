package com.aihye.toeicspeaking.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RecordRequest {
    private Integer partId;
    private String partTitle;
    private Integer questionIdx;
}
