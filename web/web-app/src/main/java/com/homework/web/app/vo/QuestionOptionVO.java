package com.homework.web.app.vo;

import lombok.Data;

@Data
public class QuestionOptionVO {
    private Long id;
    private String optionKey;
    private String optionContent;
    private Boolean correct;
}
