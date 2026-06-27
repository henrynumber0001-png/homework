package com.homework.web.app.dto;

import lombok.Data;

@Data
public class AiChatMessageDTO {
    private Long userId;
    private Long sessionId;
    private Long questionId;
    private Long answerId;
    private String messageContent;
}
