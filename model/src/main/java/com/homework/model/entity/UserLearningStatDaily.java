package com.homework.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.homework.common.entity.BaseEntity;
import java.time.LocalDate;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_learning_stat_daily")
public class UserLearningStatDaily extends BaseEntity {

    private Long userId;

    private LocalDate statDate;

    private Integer answeredCount;

    private Integer correctCount;

    private Integer learnedBankCount;

    private Integer studySeconds;
}
