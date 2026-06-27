package com.homework.web.app.dto;

import lombok.Data;

@Data
public class PrivateMessageCreateDTO {
    private Long senderUserId;
    private Long receiverUserId;
    private String content;
}
