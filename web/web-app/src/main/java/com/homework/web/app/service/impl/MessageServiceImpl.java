package com.homework.web.app.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.homework.model.entity.PrivateMessage;
import com.homework.model.entity.UserFollower;
import com.homework.model.entity.UserInfo;
import com.homework.model.entity.UserNotification;
import com.homework.model.enums.*;
import com.homework.web.app.dto.PrivateMessageCreateDTO;
import com.homework.web.app.mapper.PrivateMessageMapper;
import com.homework.web.app.mapper.UserFollowMapper;
import com.homework.web.app.mapper.UserInfoMapper;
import com.homework.web.app.mapper.UserNotificationMapper;
import com.homework.web.app.service.MessageService;
import com.homework.web.app.vo.MessageUnreadSummaryVO;
import com.homework.web.app.vo.NotificationVO;
import com.homework.web.app.vo.PrivateMessageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private static final int MAX_PRIVATE_MESSAGE_LENGTH = 1000;

    /** 拒绝 Markdown 图片、HTML 图片、data URI 及直接指向常见图片格式的 URL。 */
    private static final Pattern IMAGE_CONTENT_PATTERN = Pattern.compile(
            "(?is)!\\[[^]]*]\\([^)]*\\)|<img\\b|data:image/|https?://\\S+\\.(?:png|jpe?g|gif|webp|bmp|svg)(?:\\?\\S*)?");

    private final UserNotificationMapper userNotificationMapper;
    private final PrivateMessageMapper privateMessageMapper;
    private final UserFollowMapper userFollowMapper;
    private final UserInfoMapper userInfoMapper;

    @Override
    public List<NotificationVO> listNotifications(Long userId, Integer type, String tab,
                                                   Integer pageNum, Integer pageSize) {
        requireUserId(userId);
        QueryWrapper<UserNotification> wrapper = new QueryWrapper<UserNotification>()
                .eq("receiver_user_id", userId);
        if (type != null) {
            // 显式 type 适合跳转到某一种通知；tab 用于“我的消息”四个主模块。
            EnumUtils.fromValue(UserNotificationType.class, type);
            wrapper.eq("notification_type", type);
        } else if (tab != null && !tab.isBlank()) {
            wrapper.in("notification_type", resolveTabTypes(tab));
        }
        wrapper.orderByDesc("created_time").orderByDesc("id");

        Page<UserNotification> page = new Page<>(normalizePage(pageNum), normalizePageSize(pageSize), false);
        List<UserNotification> notifications = userNotificationMapper.selectPage(page, wrapper).getRecords();
        Map<Long, UserInfo> senders = loadUsers(notifications.stream()
                .map(UserNotification::getSenderUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()));
        return notifications.stream().map(item -> toNotificationVO(item, senders.get(item.getSenderUserId()))).toList();
    }

    @Override
    public Long countUnread(Long userId) {
        requireUserId(userId);
        return userNotificationMapper.selectCount(new QueryWrapper<UserNotification>()
                .eq("receiver_user_id", userId)
                .eq("read_status", UserNotificationReadStatus.UNREAD.getValue()));
    }

    /** 一次返回四个模块的角标，避免前端为头像菜单连续发送四个请求。 */
    @Override
    public MessageUnreadSummaryVO unreadSummary(Long userId) {
        requireUserId(userId);
        MessageUnreadSummaryVO summary = new MessageUnreadSummaryVO();
        summary.setReplies(countUnreadByTypes(userId, List.of(UserNotificationType.REPLY.getValue())));
        summary.setLikes(countUnreadByTypes(userId, interactionTypes()));
        summary.setSystem(countUnreadByTypes(userId, systemTypes()));
        summary.setPrivateMessages(countUnreadByTypes(userId,
                List.of(UserNotificationType.PRIVATE_MESSAGE.getValue())));
        return summary;
    }

    @Override
    public void markNotificationRead(Long userId, Long notificationId) {
        requireUserId(userId);
        if (notificationId == null) {
            throw new IllegalArgumentException("通知 ID 不能为空");
        }
        UserNotification notification = userNotificationMapper.selectOne(new QueryWrapper<UserNotification>()
                .eq("id", notificationId)
                .eq("receiver_user_id", userId));
        if (notification == null) {
            throw new IllegalArgumentException("通知不存在或不属于当前用户");
        }
        userNotificationMapper.update(null, new UpdateWrapper<UserNotification>()
                .set("read_status", UserNotificationReadStatus.READ.getValue())
                .eq("id", notificationId)
                .eq("receiver_user_id", userId)
                .eq("read_status", UserNotificationReadStatus.UNREAD.getValue()));
        if (notification.getNotificationType() == UserNotificationType.PRIVATE_MESSAGE
                && notification.getTargetType() == UserNotificationTargetType.PRIVATE_MESSAGE) {
            privateMessageMapper.markRead(notification.getTargetId(), userId);
        }
    }

    @Override
    public void markTabRead(Long userId, String tab) {
        requireUserId(userId);
        List<Integer> types = resolveTabTypes(tab);
        userNotificationMapper.update(null, new UpdateWrapper<UserNotification>()
                .set("read_status", UserNotificationReadStatus.READ.getValue())
                .eq("receiver_user_id", userId)
                .eq("read_status", UserNotificationReadStatus.UNREAD.getValue())
                .in("notification_type", types));
        if (types.contains(UserNotificationType.PRIVATE_MESSAGE.getValue())) {
            // 私信模块“全部已读”同时同步消息本身，避免通知角标与会话状态不一致。
            privateMessageMapper.update(null, new UpdateWrapper<PrivateMessage>()
                    .set("message_status", PrivateMessageStatus.READ.getValue())
                    .eq("receiver_user_id", userId)
                    .eq("message_status", PrivateMessageStatus.SENT.getValue()));
        }
    }

    @Override
    public List<PrivateMessageVO> listPrivateMessages(Long userId, Integer pageNum, Integer pageSize) {
        requireUserId(userId);
        Page<PrivateMessage> page = new Page<>(normalizePage(pageNum), normalizePageSize(pageSize), false);
        List<PrivateMessage> messages = privateMessageMapper.selectPage(page,
                        new QueryWrapper<PrivateMessage>()
                                .and(w -> w.eq("sender_user_id", userId).or().eq("receiver_user_id", userId))
                                .ne("message_status", PrivateMessageStatus.BLOCKED.getValue())
                                .orderByDesc("created_time")
                                .orderByDesc("id"))
                .getRecords();
        Map<Long, UserInfo> senders = loadUsers(messages.stream()
                .map(PrivateMessage::getSenderUserId).collect(Collectors.toSet()));
        return messages.stream().map(item -> toPrivateMessageVO(item, senders.get(item.getSenderUserId()))).toList();
    }

    /**
     * 互相关注可正常私信；非互关用户只允许向同一接收者发送第一条破冰消息。
     * DTO 不包含图片字段，并对正文中的常见图片表达做二次拦截。
     */
    @Override
    @Transactional
    public Long sendPrivateMessage(Long senderUserId, PrivateMessageCreateDTO dto) {
        requireUserId(senderUserId);
        if (dto == null || dto.getReceiverUserId() == null) {
            throw new IllegalArgumentException("私信接收者不能为空");
        }
        Long receiverUserId = dto.getReceiverUserId();
        if (Objects.equals(senderUserId, receiverUserId)) {
            throw new IllegalArgumentException("不能给自己发送私信");
        }
        if (userInfoMapper.selectById(receiverUserId) == null) {
            throw new IllegalArgumentException("私信接收者不存在");
        }
        String content = normalizePrivateMessage(dto.getContent());

        boolean mutualFollow = isMutualFollow(senderUserId, receiverUserId);
        if (!mutualFollow && privateMessageMapper.countFirstNonMutualMessages(senderUserId, receiverUserId) > 0) {
            throw new IllegalArgumentException("非互相关注用户只能发送第一条私信");
        }

        PrivateMessage message = new PrivateMessage();
        message.setSenderUserId(senderUserId);
        message.setReceiverUserId(receiverUserId);
        message.setContent(content);
        message.setMessageStatus(PrivateMessageStatus.SENT);
        message.setAllowReason(mutualFollow
                ? PrivateMessageAllowReason.MUTUAL_FOLLOW
                : PrivateMessageAllowReason.FIRST_NON_MUTUAL_MESSAGE);
        try {
            privateMessageMapper.insert(message);
        } catch (DuplicateKeyException e) {
            // 数据库唯一键处理并发的两次“首条”请求，并转换成稳定的业务提示。
            throw new IllegalArgumentException("非互相关注用户只能发送第一条私信", e);
        }

        UserNotification notification = new UserNotification();
        notification.setReceiverUserId(receiverUserId);
        notification.setSenderUserId(senderUserId);
        notification.setNotificationType(UserNotificationType.PRIVATE_MESSAGE);
        notification.setTargetType(UserNotificationTargetType.PRIVATE_MESSAGE);
        notification.setTargetId(message.getId());
        notification.setTitle("新私信");
        notification.setContent(content);
        notification.setReadStatus(UserNotificationReadStatus.UNREAD);
        userNotificationMapper.insert(notification);
        return message.getId();
    }

    @Override
    @Transactional
    public void markPrivateMessageRead(Long userId, Long messageId) {
        requireUserId(userId);
        if (messageId == null) {
            throw new IllegalArgumentException("私信 ID 不能为空");
        }
        privateMessageMapper.markRead(messageId, userId);
        userNotificationMapper.update(null, new UpdateWrapper<UserNotification>()
                .set("read_status", UserNotificationReadStatus.READ.getValue())
                .eq("receiver_user_id", userId)
                .eq("notification_type", UserNotificationType.PRIVATE_MESSAGE.getValue())
                .eq("target_type", UserNotificationTargetType.PRIVATE_MESSAGE.getValue())
                .eq("target_id", messageId));
    }

    private List<Integer> resolveTabTypes(String tab) {
        if (tab == null) {
            throw new IllegalArgumentException("消息模块不能为空");
        }
        return switch (tab.trim().toLowerCase(Locale.ROOT)) {
            case "replies" -> List.of(UserNotificationType.REPLY.getValue());
            // “收到的赞”模块按产品需求统一承载赞、收藏与转发通知。
            case "likes", "interactions" -> interactionTypes();
            case "system" -> systemTypes();
            case "dm", "private" -> List.of(UserNotificationType.PRIVATE_MESSAGE.getValue());
            default -> throw new IllegalArgumentException("未知的消息模块: " + tab);
        };
    }

    private List<Integer> interactionTypes() {
        return List.of(UserNotificationType.LIKE.getValue(),
                UserNotificationType.FAVORITE.getValue(),
                UserNotificationType.REPOST.getValue());
    }

    private List<Integer> systemTypes() {
        // 新增关注属于系统消息，和平台公告共用该模块。
        return List.of(UserNotificationType.SYSTEM.getValue(),
                UserNotificationType.FOLLOW.getValue());
    }

    private long countUnreadByTypes(Long userId, List<Integer> types) {
        return userNotificationMapper.selectCount(new QueryWrapper<UserNotification>()
                .eq("receiver_user_id", userId)
                .eq("read_status", UserNotificationReadStatus.UNREAD.getValue())
                .in("notification_type", types));
    }

    private boolean isMutualFollow(Long senderUserId, Long receiverUserId) {
        Long senderFollowsReceiver = userFollowMapper.selectCount(new QueryWrapper<UserFollower>()
                .eq("follower_user_id", senderUserId)
                .eq("following_user_id", receiverUserId));
        Long receiverFollowsSender = userFollowMapper.selectCount(new QueryWrapper<UserFollower>()
                .eq("follower_user_id", receiverUserId)
                .eq("following_user_id", senderUserId));
        return senderFollowsReceiver > 0 && receiverFollowsSender > 0;
    }

    private String normalizePrivateMessage(String value) {
        String content = value == null ? "" : value.trim();
        if (content.isEmpty()) {
            throw new IllegalArgumentException("私信内容不能为空");
        }
        if (content.codePointCount(0, content.length()) > MAX_PRIVATE_MESSAGE_LENGTH) {
            throw new IllegalArgumentException("私信内容最多 1000 字");
        }
        if (IMAGE_CONTENT_PATTERN.matcher(content).find()) {
            throw new IllegalArgumentException("私信暂不支持发送图片");
        }
        return content;
    }

    private Map<Long, UserInfo> loadUsers(Set<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return userInfoMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(UserInfo::getId, Function.identity()));
    }

    private NotificationVO toNotificationVO(UserNotification item, UserInfo sender) {
        NotificationVO vo = new NotificationVO();
        vo.setId(item.getId());
        vo.setSenderUserId(item.getSenderUserId());
        vo.setSenderDisplayName(sender == null ? null : sender.getDisplayName());
        vo.setSenderAvatar(sender == null ? null : sender.getAvatar());
        vo.setNotificationType(item.getNotificationType());
        vo.setTargetType(item.getTargetType());
        vo.setTargetId(item.getTargetId());
        vo.setTitle(item.getTitle());
        vo.setContent(item.getContent());
        vo.setReadStatus(item.getReadStatus());
        vo.setCreatedTime(item.getCreatedTime());
        return vo;
    }

    private PrivateMessageVO toPrivateMessageVO(PrivateMessage item, UserInfo sender) {
        PrivateMessageVO vo = new PrivateMessageVO();
        vo.setId(item.getId());
        vo.setSenderUserId(item.getSenderUserId());
        vo.setSenderDisplayName(sender == null ? null : sender.getDisplayName());
        vo.setSenderAvatar(sender == null ? null : sender.getAvatar());
        vo.setReceiverUserId(item.getReceiverUserId());
        vo.setContent(item.getContent());
        vo.setMessageStatus(item.getMessageStatus());
        vo.setAllowReason(item.getAllowReason());
        vo.setCreatedTime(item.getCreatedTime());
        return vo;
    }

    private long normalizePage(Integer pageNum) {
        return pageNum == null ? 1L : Math.max(1, pageNum);
    }

    private long normalizePageSize(Integer pageSize) {
        return pageSize == null ? 20L : Math.max(1, Math.min(pageSize, 100));
    }

    private void requireUserId(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("当前登录用户不存在");
        }
    }
}
