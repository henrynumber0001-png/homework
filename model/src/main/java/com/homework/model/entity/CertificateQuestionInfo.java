package com.homework.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.homework.common.entity.BaseEntity;
import com.homework.model.enums.QuestionInfoQuestionType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "certificate_question_info", autoResultMap = true)
public class CertificateQuestionInfo extends BaseEntity {

    private String title;

    @Schema(description = "题目选项")
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> options;

    @Schema(description = "正确选项")
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> correctAnswer;

    @Schema(description = "答案解析")
    private String analysis;

    private QuestionInfoQuestionType questionType;

    private Boolean isReleased;

    private Long createUserId;

    private Integer sortOrder;

    @Schema(description = "题目中的图片的地址")
    private String imageUrl;
}
