package com.homework.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.homework.common.entity.BaseEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("question_bank_hot_cache")
public class QuestionBankHotCache extends BaseEntity {

    private Long bankId;

    /** For example global:hot:banks or interview:backend:hot. */
    private String cacheKey;

    private Integer rankNo;

    private BigDecimal hotScore;

    private Integer viewCount7d;

    private Integer favoriteCount7d;

    private Integer practiceCount7d;

    /** Optional denormalized card data for fast homepage rendering. */
    private String cachePayload;

    private LocalDateTime cachedTime;

    private LocalDateTime expiresTime;
}
