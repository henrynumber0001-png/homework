package com.homework.web.app.service;

import com.homework.web.app.dto.PrivateMessageCreateDTO;

import java.util.List;
import java.util.Map;

public interface MessageService {
    List<Map<String, Object>> listNotifications(Long userId, Integer type, String tab);
    Long countUnread(Long userId);
    List<Map<String, Object>> listPrivateMessages(Long userId);
    Long sendPrivateMessage(PrivateMessageCreateDTO dto);
}
