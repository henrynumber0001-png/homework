package com.homework.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.homework.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_follow")
public class UserFollow extends BaseEntity {

    /** 发起关注的人。 */
    private Long followerUserId;

    /** 被关注的人。 */
    private Long followeeUserId;
}
