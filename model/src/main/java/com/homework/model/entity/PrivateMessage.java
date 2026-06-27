package com.homework.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.homework.common.entity.BaseEntity;
import com.homework.common.enums.PrivateMessageAllowReason;
import com.homework.common.enums.PrivateMessageMessageStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("private_message")
public class PrivateMessage extends BaseEntity {

    private Long senderUserId;

    private Long receiverUserId;

    private String content;

    /** 1.sent;2.read;3.blocked */
    private PrivateMessageMessageStatus messageStatus;

    /** 1.mutual_follow;2.first_non_mutual_message */
    private PrivateMessageAllowReason allowReason;
}
