package com.homework.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.homework.common.entity.BaseEntity;
import com.homework.model.enums.AiChatSessionStatus;
import com.homework.model.enums.GroupType;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_chat_session")
public class AiChatSession extends BaseEntity {

    /** 这个 AI 会话属于哪个用户。 */
    private Long userId;

    /** 这个 AI 会话属于哪个题库；同一用户同一题库复用一个追问会话。 */
    private Long bankId;

    /** 题库类型：1.面试题库；2.认证题库。用于后续区分题目来自哪张题表。 */
    private GroupType groupType;


    /** 会话状态：active 表示还在使用，closed 表示用户主动关闭或后续不再使用。 */
    private AiChatSessionStatus status;
}
