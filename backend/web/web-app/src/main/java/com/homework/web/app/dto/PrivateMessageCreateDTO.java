package com.homework.web.app.dto;

import lombok.Data;

@Data
public class PrivateMessageCreateDTO {
    /** 发送者从 JWT 登录上下文获取，客户端只需要指定接收者。 */
    private Long receiverUserId;

    /** 私信只支持纯文本，不提供图片或附件字段。 */
    private String content;
}
