package com.homework.web.app.dto;

import lombok.Data;

@Data
public class QuestionBankSaveDTO {
    private Long id;
    private String name;
    private String slug;
    private Integer bankType;
    private String description;
    private String coverUrl;
    private Boolean premium;
    private Integer accessType;
    private Integer bankStatus;
    private Long createUserId;
}
