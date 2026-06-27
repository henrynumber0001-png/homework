package com.homework.web.app.dto;

import lombok.Data;

@Data
public class CategorySubModuleSaveDTO {
    private Long id;
    private Long moduleId;
    private String subModuleName;
    private Integer sortOrder;
}
