package com.homework.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.homework.common.entity.BaseEntity;
import com.homework.model.enums.PrivateMessageStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("private_message")
public class PrivateMessage extends BaseEntity {

    private Long chatboxId;

    private Long senderUserId;

    private Long receiverUserId;

    private String content;

    /** 1.sent;2.read;3.blocked */
    private PrivateMessageStatus messageStatus;

}
