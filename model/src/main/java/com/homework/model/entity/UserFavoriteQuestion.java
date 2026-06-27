package com.homework.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.homework.common.entity.BaseEntity;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_favorite_question")
public class UserFavoriteQuestion extends BaseEntity {

    private Long userId;

    private Long questionId;

    private LocalDateTime saveTime;
}
