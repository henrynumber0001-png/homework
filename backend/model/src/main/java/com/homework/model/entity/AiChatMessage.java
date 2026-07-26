package com.homework.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.homework.common.entity.BaseEntity;
import com.homework.model.enums.AiChatMessageSenderType;
import com.homework.model.enums.GroupType;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_chat_message")
public class AiChatMessage extends BaseEntity {

    /** 所属 AI 会话 id，对应 ai_chat_session.id。 */
    private Long sessionId;
    //一个 userId + 一个 bankId 对应一个 AiChatSession
    //一个 AiChatSession 对应多条 AiChatMessage

    /** 用户是从哪一道题的答案解析入口发起这条追问的。 */
    private Long questionId;

    /** 当前消息关联的题库类型：1.面试题库；2.认证题库。 */
    private GroupType groupType;

    /** 消息发送方：user 表示用户，ai 表示模型回复。 */
    private AiChatMessageSenderType senderType;

    /** 消息正文。用户消息是追问内容，AI 消息是模型回答。 */
    private String messageContent;

    /** 生成 AI 回复的模型名称；用户消息可以为空。 */
    private String modelName;
}
