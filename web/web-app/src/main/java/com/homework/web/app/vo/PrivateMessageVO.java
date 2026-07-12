package com.homework.web.app.vo;

import com.homework.model.enums.PrivateMessageAllowReason;
import com.homework.model.enums.PrivateMessageMessageStatus;
import lombok.Data;

import java.time.LocalDateTime;

/** 私信模块的消息展示模型。 */
@Data
public class PrivateMessageVO {
    private Long id;
    private Long senderUserId;
    private String senderDisplayName;
    private String senderAvatar;
    private Long receiverUserId;
    private String content;
    private PrivateMessageMessageStatus messageStatus;
    private PrivateMessageAllowReason allowReason;
    private LocalDateTime createdTime;
}
