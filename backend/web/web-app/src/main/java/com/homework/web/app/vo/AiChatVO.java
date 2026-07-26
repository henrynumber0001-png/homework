package com.homework.web.app.vo;

import lombok.Data;

import java.util.List;

@Data
public class AiChatVO {
    //AiChatVO 是整个聊天窗口的数据。

    /** AI 会话 id。首次没有历史会话时可以为空。 */
    private Long sessionId;

    /** 当前会话对应的题库 id。 */
    private Long bankId;

    /** 会话内的完整消息列表，按发送顺序返回给前端渲染弹窗。 */
    private List<AiChatMessageVO> messages;
}
