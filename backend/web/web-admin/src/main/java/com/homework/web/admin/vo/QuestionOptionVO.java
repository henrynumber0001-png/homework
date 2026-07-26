package com.homework.web.admin.vo;

import lombok.Data;

/** 认证题选项。 */
@Data
public class QuestionOptionVO {

    /** 连续的大写选项键，例如 A、B。 */
    private String key;

    /** 展示给用户的选项内容。 */
    private String content;
}
