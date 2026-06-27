package com.homework.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.homework.common.entity.BaseEntity;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("question_ai_evaluation")
public class QuestionAiEvaluation extends BaseEntity {

    private Long answerId;

    private Long questionId;

    private Long userId;

    private BigDecimal scoreRate;

    private String accurateComment;

    private String innovativeComment;

    private String missingComment;

    private String wrongComment;

    private String summary;

    private String modelName;
}
