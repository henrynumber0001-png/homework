package com.homework.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.homework.common.entity.BaseEntity;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_medal")
public class UserMedal extends BaseEntity {

    private Long userId;

    private Long medalId;

    private LocalDateTime earnedTime;
}
