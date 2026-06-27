package com.homework.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.homework.common.entity.BaseEntity;
import com.homework.common.enums.QuestionBankAccessType;
import com.homework.common.enums.QuestionBankBankStatus;
import com.homework.common.enums.QuestionBankBankType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("question_bank")
public class QuestionBank extends BaseEntity {

    private String name;

    private String slug;

    /** 1.interview;2.certification */
    private QuestionBankBankType bankType;

    private String description;

    private String searchText;

    private String coverUrl;

    private Integer questionCount;

    private Integer practiceUserCount;

    private BigDecimal avgCorrectRate;

    private Integer favoriteCount;

    private Integer viewCount;

    private Integer priority;

    @TableField("is_premium")
    private Boolean premium;

    /** 1.free;2.premium;3.mixed_preview */
    private QuestionBankAccessType accessType;

    /** 1.draft;2.published;3.archived */
    private QuestionBankBankStatus bankStatus;

    private Long createUserId;

    private LocalDateTime publishedTime;
}
