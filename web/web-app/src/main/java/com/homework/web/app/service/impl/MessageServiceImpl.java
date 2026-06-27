package com.homework.web.app.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.homework.common.enums.PrivateMessageAllowReason;
import com.homework.common.enums.PrivateMessageMessageStatus;
import com.homework.web.app.dto.PrivateMessageCreateDTO;
import com.homework.model.entity.PrivateMessage;
import com.homework.model.entity.UserFollow;
import com.homework.model.entity.UserNotification;
import com.homework.web.app.mapper.PrivateMessageMapper;
import com.homework.web.app.mapper.UserFollowMapper;
import com.homework.web.app.mapper.UserNotificationMapper;
import com.homework.web.app.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final UserNotificationMapper userNotificationMapper;
    private final PrivateMessageMapper privateMessageMapper;
    private final UserFollowMapper userFollowMapper;

    @Override
    public List<Map<String, Object>> listNotifications(Long userId, Integer type, String tab) {
        QueryWrapper<UserNotification> wrapper = new QueryWrapper<UserNotification>()
                .eq("receiver_user_id", userId)
                .eq("is_deleted", 0);
        List<Integer> tabTypes = resolveTabTypes(tab);
        if (type != null) {
            wrapper.eq("notification_type", type);
        } else if (!tabTypes.isEmpty()) {
            wrapper.in("notification_type", tabTypes);
        }
        return userNotificationMapper.selectMaps(wrapper
                .orderByDesc("created_time")
                .last("LIMIT 50"));
    }

    @Override
    public Long countUnread(Long userId) {
        return userNotificationMapper.selectCount(new QueryWrapper<UserNotification>()
                .eq("receiver_user_id", userId)
                .eq("read_status", 1)
                .eq("is_deleted", 0));
    }

    @Override
    public List<Map<String, Object>> listPrivateMessages(Long userId) {
        return privateMessageMapper.selectMaps(new QueryWrapper<PrivateMessage>()
                .and(w -> w.eq("sender_user_id", userId).or().eq("receiver_user_id", userId))
                .eq("is_deleted", 0)
                .orderByDesc("created_time")
                .last("LIMIT 50"));
    }

    @Override
    public Long sendPrivateMessage(PrivateMessageCreateDTO dto) {
        String content = dto.getContent() == null ? "" : dto.getContent().trim();
        if (content.isEmpty()) {
            throw new IllegalArgumentException("Private message content is required");
        }
        boolean mutualFollow = isMutualFollow(dto.getSenderUserId(), dto.getReceiverUserId());
        if (!mutualFollow && hasFirstNonMutualMessage(dto.getSenderUserId(), dto.getReceiverUserId())) {
            throw new IllegalArgumentException("Only one first private message is allowed for non-mutual follows");
        }
        PrivateMessage message = new PrivateMessage();
        message.setSenderUserId(dto.getSenderUserId());
        message.setReceiverUserId(dto.getReceiverUserId());
        message.setContent(content);
        message.setMessageStatus(PrivateMessageMessageStatus.SENT);
        message.setAllowReason(mutualFollow ? PrivateMessageAllowReason.MUTUAL_FOLLOW : PrivateMessageAllowReason.FIRST_NON_MUTUAL_MESSAGE);
        privateMessageMapper.insert(message);
        return message.getId();
    }

    private List<Integer> resolveTabTypes(String tab) {
        if ("replies".equalsIgnoreCase(tab)) {
            return List.of(1);
        }
        if ("likes".equalsIgnoreCase(tab)) {
            return List.of(2, 5, 6);
        }
        if ("system".equalsIgnoreCase(tab)) {
            return List.of(3, 7);
        }
        if ("dm".equalsIgnoreCase(tab)) {
            return List.of(4);
        }
        return Collections.emptyList();
    }

    private boolean isMutualFollow(Long senderUserId, Long receiverUserId) {
        if (senderUserId == null || receiverUserId == null) {
            return false;
        }
        Long senderFollowsReceiver = userFollowMapper.selectCount(new QueryWrapper<UserFollow>()
                .eq("follower_user_id", senderUserId)
                .eq("following_user_id", receiverUserId)
                .eq("is_deleted", 0));
        Long receiverFollowsSender = userFollowMapper.selectCount(new QueryWrapper<UserFollow>()
                .eq("follower_user_id", receiverUserId)
                .eq("following_user_id", senderUserId)
                .eq("is_deleted", 0));
        return senderFollowsReceiver > 0 && receiverFollowsSender > 0;
    }

    private boolean hasFirstNonMutualMessage(Long senderUserId, Long receiverUserId) {
        Long count = privateMessageMapper.selectCount(new QueryWrapper<PrivateMessage>()
                .eq("sender_user_id", senderUserId)
                .eq("receiver_user_id", receiverUserId)
                .eq("allow_reason", 2)
                .eq("is_deleted", 0));
        return count > 0;
    }
}
