package com.homework.web.app.vo;

import lombok.Data;

@Data
public class CategoryModuleVO {
    private Long id;
    private Long groupId;
    private String moduleName;
    private String displayIcon;
    private String themeColor;
    private String themeBgColor;
    private String themeGradient;
    private Integer sortOrder;
}
