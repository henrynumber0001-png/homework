package com.homework.web.app.service;

import com.homework.common.result.PageResult;
import com.homework.web.app.dto.PrivateMessageCreateDTO;
import com.homework.web.app.vo.*;

import java.util.List;

public interface MessageService {
    PageResult<NotificationVO> loadNotificationTab(
            Long userId, String tab, Integer pageNum, Integer pageSize, boolean history);
    MessageUnreadSummaryVO unreadSummary(Long userId);
    PageResult<PrivateChatboxVO> listChatboxes(Long userId, Integer pageNum, Integer pageSize);
    PrivateChatboxVO findChatboxWith(Long userId, Long otherUserId);
    List<PrivateMessageVO> listMessages(Long userId, Long chatboxId, Long beforeId, Long afterId, Integer limit);
    PrivateMessageVO sendPrivateMessage(Long senderUserId, PrivateMessageCreateDTO dto);
    void markPrivateMessageRead(Long userId, Long messageId);
}
