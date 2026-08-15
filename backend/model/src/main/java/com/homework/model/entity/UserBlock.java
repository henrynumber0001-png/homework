package com.homework.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.homework.common.entity.BaseEntity;
import lombok.Data;

@Data
@TableName("user_block")
public class UserBlock extends BaseEntity {

    //主动拉黑别人的用户id
    private Long blockerUserId;

    //被拉黑用户的id
    private Long blockedUserId;

}
