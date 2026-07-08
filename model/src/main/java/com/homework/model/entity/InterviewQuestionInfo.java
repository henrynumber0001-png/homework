package com.homework.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.homework.common.entity.BaseEntity;
import com.homework.model.enums.QuestionInfoQuestionType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("question_info")
public class InterviewQuestionInfo extends BaseEntity {

    private String title;

    @Schema(description = "参考答案")
    private String analysis;

    private QuestionInfoQuestionType questionType;

    private Boolean isReleased;

    private Long createUserId;

    private Integer sortOrder;
}
