package com.homework.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.homework.common.entity.BaseEntity;
import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_learning_stat_daily")
public class UserLearningStatDaily extends BaseEntity {

    private Long userId;

    //统计日期
    private LocalDate statDate;

    /** 当天累计学习时间。 */
    private Long studySeconds;

    /** 后端最后一次接受的心跳时间。 */
    private LocalDateTime lastHeartbeatTime;
}
