package com.homework.web.app.dto;

import lombok.Data;

@Data
public class BankQueryDTO {
    private Long moduleId;
    private Long subModuleId;
    private String keyword;
    private String sort;
    private Integer pageNum = 1;
    private Integer pageSize = 20;
}
