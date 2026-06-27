package com.homework.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.homework.common.entity.BaseEntity;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("browsing_history")
public class BrowsingHistory extends BaseEntity {

    private Long userId;

    private Long bankId;

    private Long groupId;

    private Long moduleId;

    private Long detailId;

    /** Accumulated answer time in seconds. */
    private Integer answerTime;

    private Integer viewCount;

    private LocalDateTime lastViewedTime;
}
