package com.homework.web.app.vo;

import lombok.Data;

import java.util.List;

@Data
public class QuestionVO {
    private Long id;
    private String title;
    private String content;
    private String answer;
    private String analysis;
    private Integer questionType;
    private Integer difficulty;
    private Boolean premium;
    private List<QuestionOptionVO> options;
}
