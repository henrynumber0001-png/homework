package com.homework.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.homework.common.entity.BaseEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("question_bank")
public class QuestionBank extends BaseEntity {

    private String bankName;

    private Long subModuleId;

    @Schema(description = "完成题库的人数")
    private Integer completeCount;

    private BigDecimal avgCorrectRate;

    private Integer viewCount;

    private Integer hotScore;

    private Integer priority;

    @Schema(description = "题库中题目的数量")
    private Integer questionCount;

    private Long createUserId;

    private LocalDateTime publishedTime;
}
