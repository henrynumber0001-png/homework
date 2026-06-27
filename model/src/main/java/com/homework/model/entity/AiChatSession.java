package com.homework.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.homework.common.entity.BaseEntity;
import com.homework.common.enums.AiChatSessionStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_chat_session")
public class AiChatSession extends BaseEntity {

    private Long userId;

    private Long questionId;

    private Long answerId;

    private String title;

    /** 1.active;2.closed */
    private AiChatSessionStatus status;
}
