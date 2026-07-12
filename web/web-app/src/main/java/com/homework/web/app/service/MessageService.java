package com.homework.web.app.service;

import com.homework.web.app.dto.PrivateMessageCreateDTO;
import com.homework.web.app.vo.MessageUnreadSummaryVO;
import com.homework.web.app.vo.NotificationVO;
import com.homework.web.app.vo.PrivateMessageVO;

import java.util.List;

public interface MessageService {

    List<NotificationVO> listNotifications(Long userId, Integer type, String tab,
                                           Integer pageNum, Integer pageSize);

    Long countUnread(Long userId);

    MessageUnreadSummaryVO unreadSummary(Long userId);

    void markNotificationRead(Long userId, Long notificationId);

    void markTabRead(Long userId, String tab);

    List<PrivateMessageVO> listPrivateMessages(Long userId, Integer pageNum, Integer pageSize);

    Long sendPrivateMessage(Long senderUserId, PrivateMessageCreateDTO dto);

    void markPrivateMessageRead(Long userId, Long messageId);
}
