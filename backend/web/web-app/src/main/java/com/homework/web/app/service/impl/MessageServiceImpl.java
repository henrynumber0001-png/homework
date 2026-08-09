package com.homework.web.app.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.homework.common.result.PageResult;
import com.homework.common.storage.UserImageUrlResolver;
import com.homework.model.entity.HitComment;
import com.homework.model.entity.HitPost;
import com.homework.model.entity.PrivateChatbox;
import com.homework.model.entity.PrivateMessage;
import com.homework.model.entity.UserFollow;
import com.homework.model.entity.UserInfo;
import com.homework.model.entity.UserNotification;
import com.homework.model.enums.HitPostStatus;
import com.homework.model.enums.PrivateChatAccess;
import com.homework.model.enums.PrivateMessageStatus;
import com.homework.model.enums.UserInfoStatus;
import com.homework.model.enums.UserNotificationReadStatus;
import com.homework.model.enums.UserNotificationSendTo;
import com.homework.model.enums.UserNotificationType;
import com.homework.web.app.dto.PrivateMessageCreateDTO;
import com.homework.web.app.mapper.HitCommentMapper;
import com.homework.web.app.mapper.HitPostMapper;
import com.homework.web.app.mapper.PrivateChatboxMapper;
import com.homework.web.app.mapper.PrivateMessageMapper;
import com.homework.web.app.mapper.UserFollowMapper;
import com.homework.web.app.mapper.UserInfoMapper;
import com.homework.web.app.mapper.UserNotificationMapper;
import com.homework.web.app.service.MessageService;
import com.homework.web.app.vo.MessageUnreadSummaryVO;
import com.homework.web.app.vo.NotificationVO;
import com.homework.web.app.vo.PrivateChatboxVO;
import com.homework.web.app.vo.PrivateMessageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final UserNotificationMapper notificationMapper;
    private final PrivateChatboxMapper chatboxMapper;
    private final PrivateMessageMapper messageMapper;
    private final UserInfoMapper userInfoMapper;
    private final UserFollowMapper followMapper;
    private final HitCommentMapper commentMapper;
    private final HitPostMapper postMapper;
    private final UserImageUrlResolver userImageUrlResolver;

    @Override
    @Transactional
    public PageResult<NotificationVO> loadNotificationTab(
            Long userId,
            String tab,
            Integer pageNum,
            Integer pageSize,
            boolean history) {

        // 前端传入的 tab 是字符串，这里先转换成数据库中的通知类型。
        List<UserNotificationType> notificationTypes;
        if ("comments".equals(tab)) {
            notificationTypes = List.of(UserNotificationType.COMMENT, UserNotificationType.MENTION);
        } else if ("interactions".equals(tab)) {
            notificationTypes = List.of(UserNotificationType.LIKE, UserNotificationType.FAVORITE, UserNotificationType.REPOST);
        } else if ("system".equals(tab)) {
            notificationTypes = List.of(UserNotificationType.SYSTEM, UserNotificationType.FOLLOW);
        } else {
            throw new IllegalArgumentException("未知消息分类");
        }

        /*
         * 正常打开 Tab 时，先把这个 Tab 当前所有未读通知批量改为已读。
         * 查看历史信息时只查询，不改变任何通知状态。
         */
        if (!history) {
            notificationMapper.markTypesRead(userId, notificationTypes);
        }

        /*
         * 同一次批量更新中的通知具有相同的 updated_time。
         * 最大的 updated_time 就是这个 Tab 最近一次批量已读的时间。
         */
        LocalDateTime latestReadTime =
                notificationMapper.selectLatestReadTime(userId, notificationTypes);

        // 页码最小为 1；每页默认 20 条，最多允许 100 条。
        long currentPage = pageNum == null ? 1L : Math.max(pageNum, 1);
        long currentPageSize = pageSize == null ? 20L : Math.min(Math.max(pageSize, 1), 100); // 1 <= pageSize <= 100

        Page<UserNotification> notificationPage = new Page<>(currentPage, currentPageSize);

        /*
         * 如果这个 Tab 从来没有已读通知，就没有“最近批次”，
         * 正常列表和历史列表都直接返回空结果。
         */
        if (latestReadTime == null) {
            PageResult<NotificationVO> emptyResult = new PageResult<>();
            emptyResult.setRecords(List.of());
            emptyResult.setTotal(0L);
            emptyResult.setPageNum(currentPage);
            emptyResult.setPageSize(currentPageSize);
            return emptyResult;
        }

        LambdaQueryWrapper<UserNotification> notificationQuery =
                new LambdaQueryWrapper<UserNotification>()
                        .eq(UserNotification::getReceiverUserId, userId)
                        .eq(UserNotification::getReadStatus, UserNotificationReadStatus.READ)
                        .in(UserNotification::getNotificationType, notificationTypes);

        if (history) {
            // 历史信息只包含最近一次批量已读之前的通知。
            notificationQuery.lt(UserNotification::getUpdatedTime, latestReadTime);
        } else {
            // 默认列表只显示最近一次批量改为已读的通知。
            notificationQuery.eq(UserNotification::getUpdatedTime, latestReadTime);
        }

        notificationQuery
                .orderByDesc(UserNotification::getCreatedTime)
                .orderByDesc(UserNotification::getId);

        List<UserNotification> notifications = notificationMapper.selectPage(
                notificationPage,
                notificationQuery
        ).getRecords();

        /*
         * 通知表只保存了发送者、Post 和 Comment 的 ID。
         * 下面先批量查询这些关联数据，避免在遍历每一条通知时反复访问数据库。
         */
        Set<Long> senderUserIds = new HashSet<>();
        Set<Long> postIds = new HashSet<>();
        Set<Long> commentIds = new HashSet<>();

        for (UserNotification notification : notifications) {
            if (notification.getSenderUserId() != null) {
                senderUserIds.add(notification.getSenderUserId());
            }
            if (notification.getPostId() != null) {
                postIds.add(notification.getPostId());
            }
            // 这里只收集评论 ID；评论是否被删除，要根据后续查询得到的 is_deleted 判断。
            if (notification.getSendTo() == UserNotificationSendTo.HIT_COMMENT && notification.getItemId() != null) {
                commentIds.add(notification.getItemId());
            }
        }

        Map<Long, UserInfo> senderMap = new HashMap<>();
        if (!senderUserIds.isEmpty()) {
            List<UserInfo> userInfos = userInfoMapper.selectByIds(senderUserIds);
            senderMap = userInfos.stream().collect(Collectors.toMap(UserInfo::getId, Function.identity()));
        }

        Map<Long, HitPost> postsMap = new HashMap<>();
        if (!postIds.isEmpty()) {
            List<HitPost> postList = postMapper.selectByIds(postIds);
            postsMap = postList.stream().collect(Collectors.toMap(HitPost::getId, Function.identity()));
        }

        // 评论即使被软删除，也需要查出来，这样前端才能显示“原评论已删除”。
        Map<Long, HitComment> commentsMap = new HashMap<>();
        if (!commentIds.isEmpty()) {
            List<HitComment> commentList = commentMapper.selectIncludingDeletedByIds(commentIds);
            commentsMap = commentList.stream().collect(Collectors.toMap(HitComment::getId, Function.identity()));
        }

        // 如果通知指向的是一条回复，还需要检查它的父评论是否已被删除。
        // 收集所有父评论的 ID。
        Set<Long> parentCommentIds = new HashSet<>();
        for (HitComment comment : commentsMap.values()) {
            Long parentCommentId = comment.getParentCommentId();

            // parentCommentId 为空，表示这条评论本身就是一级评论。
            if (parentCommentId != null) {
                parentCommentIds.add(parentCommentId);
            }
        }
        // 存在父评论 ID 时，再去数据库批量查询父评论。
        if (!parentCommentIds.isEmpty()) {
            List<HitComment> parentComments = commentMapper.selectIncludingDeletedByIds(parentCommentIds);

            // 把查询到的父评论也放进 commentsMap，方便后面根据 ID 查找。
            for (HitComment parentComment : parentComments) {
                commentsMap.put(parentComment.getId(), parentComment);
            }
        }


        List<NotificationVO> records = new ArrayList<>();
        for (UserNotification notification : notifications) {
            NotificationVO vo = new NotificationVO();
            vo.setId(notification.getId());
            vo.setNotificationType(notification.getNotificationType());
            vo.setPostId(notification.getPostId());
            vo.setTitle(notification.getTitle());
            vo.setContent(notification.getContent());
            vo.setReadStatus(notification.getReadStatus());
            vo.setCreatedTime(notification.getCreatedTime());

            if (notification.getSenderUserId() != null) {
                UserInfo sender = senderMap.get(notification.getSenderUserId());
                vo.setActionUserId(notification.getSenderUserId());
                vo.setActionDisplayName(sender == null ? "该用户已注销" : sender.getDisplayName()); //查不到用户名，只能说明状态异常，但不要报错，属于正常
                vo.setActionAvatar(sender == null ? null : resolveAvatar(sender));
            }

            if (notification.getSendTo() == UserNotificationSendTo.HIT_COMMENT) {
                vo.setCommentId(notification.getItemId());

                HitComment comment = commentsMap.get(notification.getItemId());

                boolean commentDeleted = comment == null || Boolean.TRUE.equals(comment.getDeleted());

                //如果这个评论没删
                if (!commentDeleted && comment.getParentCommentId() != null) {
                    //获取父评论
                    HitComment parentComment = commentsMap.get(comment.getParentCommentId());
                    //然后看看父评论删没删
                    commentDeleted = parentComment == null || Boolean.TRUE.equals(parentComment.getDeleted());
                }
                vo.setCommentDeleted(commentDeleted);
                if (commentDeleted) {
                    vo.setContent("原评论已删除");
                }
            }

            HitPost post = postsMap.get(notification.getPostId());
            boolean postAvailable =
                    post != null && post.getPostStatus() == HitPostStatus.PUBLISHED;
            vo.setPostAvailable(postAvailable);

            if (notification.getPostId() != null && !postAvailable) {
                vo.setContent("原内容已删除或不可见");
            }

            records.add(vo);
        }

        PageResult<NotificationVO> result = new PageResult<>();
        result.setRecords(records);
        result.setTotal(notificationPage.getTotal());
        result.setPageNum(notificationPage.getCurrent());
        result.setPageSize(notificationPage.getSize());
        return result;
    }

    @Override
    public MessageUnreadSummaryVO unreadSummary(Long userId) {
        MessageUnreadSummaryVO result = new MessageUnreadSummaryVO();

        //查 评论和@ 未读数
        long commentsAndMentions = notificationMapper.selectCount(
                new LambdaQueryWrapper<UserNotification>()
                        .eq(UserNotification::getReceiverUserId, userId) //一定是去查UserNotification里的接受者，因为是看接受者的未读信息数量
                        .eq(UserNotification::getReadStatus, UserNotificationReadStatus.UNREAD)
                        .in(UserNotification::getNotificationType, List.of(UserNotificationType.COMMENT, UserNotificationType.MENTION))
        );
        result.setCommentsAndMentions(commentsAndMentions);

        //查 赞藏转 未读数
        long interactions = notificationMapper.selectCount(
                new LambdaQueryWrapper<UserNotification>()
                        .eq(UserNotification::getReceiverUserId, userId)
                        .eq(UserNotification::getReadStatus, UserNotificationReadStatus.UNREAD)
                        .in(UserNotification::getNotificationType, List.of(UserNotificationType.LIKE, UserNotificationType.FAVORITE, UserNotificationType.REPOST))
        );
        result.setInteractions(interactions);

        //查 系统消息 未读数
        long system = notificationMapper.selectCount(
                new LambdaQueryWrapper<UserNotification>()
                        .eq(UserNotification::getReceiverUserId, userId)
                        .eq(UserNotification::getReadStatus, UserNotificationReadStatus.UNREAD)
                        .in(UserNotification::getNotificationType, List.of(UserNotificationType.SYSTEM, UserNotificationType.FOLLOW))
        );
        result.setSystem(system);

        //查 私信 未读数
        long privateMessages = messageMapper.selectCount(
                new LambdaQueryWrapper<PrivateMessage>()
                        .eq(PrivateMessage::getReceiverUserId, userId)
                        .eq(PrivateMessage::getMessageStatus, PrivateMessageStatus.SENT)
        );
        result.setPrivateMessages(privateMessages);

        return result;
    }

    @Override
    //找到当前用户的全部 私信聊天框
    public PageResult<PrivateChatboxVO> listChatboxes(Long userId, Integer pageNum, Integer pageSize) {

        long currentPage = pageNum == null ? 1L : Math.max(pageNum, 1); // pageNum >= 1
        long currentPageSize = pageSize == null ? 20L : Math.min(Math.max(pageSize, 1), 100); // 1 <= pageSize <= 100

        //创建 私信 分页容器
        Page<PrivateChatbox> chatboxPage = new Page<>(currentPage, currentPageSize);

        //这个查询结果，会把 当前用户 作为UserA的chatbox 和 作为UserB的chatbox数据都查出来
        //你一定注意了！private_chatbox 表是一张“会话表”，它不是用来保存每一条聊天内容
        //它是用来保存 “聊天框” 本身的！

        //userAId 和 userBId 没有任何含义上的指向性，单纯依靠 数字大小 排序
        //它们只是用来标记 这两个人之间，有且只有这么一个聊天框 private_chatbox_id 对两个人来说，都是唯一的
        LambdaQueryWrapper<PrivateChatbox> boxQueryWrapper = new LambdaQueryWrapper<>();
        boxQueryWrapper.eq(PrivateChatbox::getUserAId, userId)
                .or()
                .eq(PrivateChatbox::getUserBId, userId)
                .orderByDesc(PrivateChatbox::getLastMessageTime)
                .orderByDesc(PrivateChatbox::getId);

        //简单的单表分页查询，直接用 Mybatis-Plus 预留的 selectPage 方法
        List<PrivateChatbox> chatboxes = chatboxMapper.selectPage(chatboxPage, boxQueryWrapper).getRecords();

        List<PrivateChatboxVO> records = new ArrayList<>();

        //当前用户，可能会有 既作为userA的数据，也有作为userB的数据
        //因为和不同的id聊天，userId大小排序是不固定的，和id = 7聊天，就是userB; 和id = 11聊天，就是userA
        //但是这里设计了一个for循环，每一个循环里只有一行 privateChatbox 数据，这一行数据内，当前用户 只可能是 userA 或 userB
        for (PrivateChatbox chatbox : chatboxes) {
            // 一个聊天框固定有两个人。先判断当前用户是哪一方，再得到对方的 ID。
            Long otherUserId;
            if (Objects.equals(chatbox.getUserAId(), userId)) {
                otherUserId = chatbox.getUserBId();
            } else {
                otherUserId = chatbox.getUserAId();
            }

            UserInfo otherUser = userInfoMapper.selectById(otherUserId);

            boolean canCurrentUserSend;
            if (chatbox.getChatAccess() == PrivateChatAccess.OPEN) {
                // 聊天框已经开放，双方都可以继续发送。
                canCurrentUserSend = true;
            } else if (!Objects.equals(chatbox.getInitiatorUserId(), userId)) {
                //如果第一个发消息的人，不是当前用户，因此允许回复。
                canCurrentUserSend = true;
            } else {
                // 当前用户是发起者。只有双方互相关注后，才允许在对方回复前继续发送。
                //先看 当前用户是否关注了对方用户
                boolean currentUserFollowsOther =
                        followMapper.selectCount(
                                new LambdaQueryWrapper<UserFollow>()
                                        .eq(UserFollow::getFollowerUserId, userId)
                                        .eq(UserFollow::getFolloweeUserId, otherUserId)
                        ) > 0;

                //再看 对方用户是否关注了当前用户
                boolean otherFollowsCurrentUser = false;
                if (currentUserFollowsOther) {
                    otherFollowsCurrentUser =
                            followMapper.selectCount(
                                    new LambdaQueryWrapper<UserFollow>()
                                            .eq(UserFollow::getFollowerUserId, otherUserId)
                                            .eq(UserFollow::getFolloweeUserId, userId)
                            ) > 0;
                }

                canCurrentUserSend = currentUserFollowsOther && otherFollowsCurrentUser;
            }

            PrivateMessage lastMessage = null;
            if (chatbox.getLastMessageId() != null) {
                lastMessage = messageMapper.selectById(chatbox.getLastMessageId());
            }

            long unreadCount = messageMapper.selectCount(
                    new LambdaQueryWrapper<PrivateMessage>()
                            .eq(PrivateMessage::getChatboxId, chatbox.getId())
                            .eq(PrivateMessage::getReceiverUserId, userId)
                            .eq(PrivateMessage::getMessageStatus, PrivateMessageStatus.SENT)
            );

            PrivateChatboxVO vo = new PrivateChatboxVO();
            vo.setChatboxId(chatbox.getId());
            vo.setOtherUserId(otherUserId);
            vo.setOtherDisplayName(otherUser == null ? "该用户已注销" : otherUser.getDisplayName());
            vo.setOtherAvatar(otherUser == null ? null : resolveAvatar(otherUser));
            vo.setChatAccess(chatbox.getChatAccess());
            vo.setCanCurrentUserSend(canCurrentUserSend);
            vo.setLastMessage(lastMessage == null ? null : lastMessage.getContent());
            vo.setLastMessageTime(chatbox.getLastMessageTime());
            vo.setUnreadCount(unreadCount);
            records.add(vo);
        }

        PageResult<PrivateChatboxVO> result = new PageResult<>();
        result.setRecords(records);
        result.setTotal(chatboxPage.getTotal());
        result.setPageNum(chatboxPage.getCurrent());
        result.setPageSize(chatboxPage.getSize());
        return result;
    }

    @Override
    //假设当前登录用户是 9，他进入用户 7 的个人主页，然后点击“发私信”。
    //已经知道对方是谁时，快速找到双方唯一的 Chatbox，不需要遍历整个会话列表。
    public PrivateChatboxVO findChatboxWith(Long userId, Long otherUserId) {
        long userAId = Math.min(userId, otherUserId);
        long userBId = Math.max(userId, otherUserId);

        PrivateChatbox chatbox = chatboxMapper.selectOne(
                new LambdaQueryWrapper<PrivateChatbox>()
                        .eq(PrivateChatbox::getUserAId, userAId)
                        .eq(PrivateChatbox::getUserBId, userBId)
        );

        if (chatbox == null) {
            return null;
        }

        UserInfo otherUser = userInfoMapper.selectById(otherUserId);

        boolean canCurrentUserSend;
        if (chatbox.getChatAccess() == PrivateChatAccess.OPEN) {
            canCurrentUserSend = true;
        } else if (!Objects.equals(chatbox.getInitiatorUserId(), userId)) {
            canCurrentUserSend = true;
        } else {
            boolean currentUserFollowsOther =
                    followMapper.selectCount(
                            new LambdaQueryWrapper<UserFollow>()
                                    .eq(UserFollow::getFollowerUserId, userId)
                                    .eq(UserFollow::getFolloweeUserId, otherUserId)
                    ) > 0;

            boolean otherFollowsCurrentUser = false;
            if (currentUserFollowsOther) {
                otherFollowsCurrentUser =
                        followMapper.selectCount(
                                new LambdaQueryWrapper<UserFollow>()
                                        .eq(UserFollow::getFollowerUserId, otherUserId)
                                        .eq(UserFollow::getFolloweeUserId, userId)
                        ) > 0;
            }

            canCurrentUserSend = currentUserFollowsOther && otherFollowsCurrentUser;
        }

        PrivateMessage lastMessage = null;
        if (chatbox.getLastMessageId() != null) {
            lastMessage = messageMapper.selectById(chatbox.getLastMessageId());
        }

        long unreadCount = messageMapper.selectCount(
                new LambdaQueryWrapper<PrivateMessage>()
                        .eq(PrivateMessage::getChatboxId, chatbox.getId())
                        .eq(PrivateMessage::getReceiverUserId, userId)
                        .eq(PrivateMessage::getMessageStatus, PrivateMessageStatus.SENT)
        );

        PrivateChatboxVO result = new PrivateChatboxVO();
        result.setChatboxId(chatbox.getId());
        result.setOtherUserId(otherUserId);
        result.setOtherDisplayName(
                otherUser == null ? "该用户已注销" : otherUser.getDisplayName()
        );
        result.setOtherAvatar(otherUser == null ? null : resolveAvatar(otherUser));
        result.setChatAccess(chatbox.getChatAccess());
        result.setCanCurrentUserSend(canCurrentUserSend);
        result.setLastMessage(lastMessage == null ? null : lastMessage.getContent());
        result.setLastMessageTime(chatbox.getLastMessageTime());
        result.setUnreadCount(unreadCount);
        return result;
    }

    @Override
    //查询某个 Chatbox 中的聊天消息，并支持首次加载、向上加载历史消息、查询最新消息。
    //beforeId = 查询某条消息之前的历史消息, afterId = 查询某条消息之后的历史消息
    public List<PrivateMessageVO> listMessages(Long userId, Long chatboxId, Long beforeId, Long afterId, Integer limit) {

        // 先确认聊天框存在，并且当前用户确实属于这个聊天框。
        PrivateChatbox chatbox = chatboxMapper.selectById(chatboxId);
        boolean userBelongsToChatbox = chatbox != null
                && (Objects.equals(chatbox.getUserAId(), userId)
                || Objects.equals(chatbox.getUserBId(), userId));
        if (!userBelongsToChatbox) {
            throw new IllegalArgumentException("Chatbox 不存在");
        }

        if (beforeId != null && afterId != null) {
            throw new IllegalArgumentException("beforeId 和 afterId 不能同时使用");
        }

        LambdaQueryWrapper<PrivateMessage> messageQuery =
                new LambdaQueryWrapper<PrivateMessage>()
                        .eq(PrivateMessage::getChatboxId, chatbox.getId());

        if (beforeId != null) {
            messageQuery.lt(PrivateMessage::getId, beforeId);
        }
        if (afterId != null) {
            messageQuery.gt(PrivateMessage::getId, afterId);
        }

        /*
         * 查询更早的消息时，先倒序取最近的若干条，再在内存中恢复成正序。
         * 查询更新的消息时，可以直接按 ID 正序返回。
         */
        if (afterId == null) {
            messageQuery.orderByDesc(PrivateMessage::getId);
        } else {
            messageQuery.orderByAsc(PrivateMessage::getId);
        }

        int queryLimit = limit == null ? 50 : Math.min(Math.max(limit, 1), 100);
        messageQuery.last("LIMIT " + queryLimit);

        // 使用新的 ArrayList 保存结果，避免直接修改 Mapper 返回的集合。
        List<PrivateMessage> messages = new ArrayList<>(messageMapper.selectList(messageQuery));
        if (afterId == null) {
            Collections.reverse(messages);
        }

        if (messages.isEmpty()) {
            return List.of();
        }

        // 批量查询所有发送者，避免每组装一条消息就查询一次用户表。
        Set<Long> senderUserIds = new HashSet<>();
        for (PrivateMessage message : messages) {
            senderUserIds.add(message.getSenderUserId());
        }

        Map<Long, UserInfo> senderUsers = new HashMap<>();
        List<UserInfo> users = userInfoMapper.selectByIds(senderUserIds);
        for (UserInfo user : users) {
            senderUsers.put(user.getId(), user);
        }

        List<PrivateMessageVO> result = new ArrayList<>();
        for (PrivateMessage message : messages) {
            UserInfo sender = senderUsers.get(message.getSenderUserId());

            PrivateMessageVO vo = new PrivateMessageVO();
            vo.setId(message.getId());
            vo.setChatboxId(message.getChatboxId());
            vo.setSenderUserId(message.getSenderUserId());
            vo.setSenderDisplayName(
                    sender == null ? "该用户已注销" : sender.getDisplayName()
            );
            vo.setSenderAvatar(sender == null ? null : resolveAvatar(sender));
            vo.setReceiverUserId(message.getReceiverUserId());
            vo.setContent(message.getContent());
            vo.setMessageStatus(message.getMessageStatus());
            vo.setCreatedTime(message.getCreatedTime());
            result.add(vo);
        }

        return result;
    }

    @Override
    @Transactional

    public PrivateMessageVO sendPrivateMessage(Long senderUserId, PrivateMessageCreateDTO dto) {

        Long receiverUserId = dto == null ? null : dto.getReceiverUserId();
        if (receiverUserId == null) {
            throw new IllegalArgumentException("接收者不能为空");
        }
        if (Objects.equals(senderUserId, receiverUserId)) {
            throw new IllegalArgumentException("不能给自己发送私信");
        }

        String content = dto.getContent() == null ? "" : dto.getContent().trim();
        if (content.isEmpty()
                || content.codePointCount(0, content.length()) > 1000) {
            throw new IllegalArgumentException("私信内容应为 1 至 1000 字");
        }

        UserInfo receiver = userInfoMapper.selectById(receiverUserId);
        boolean receiverCanReceiveMessage = receiver != null
                && receiver.getStatus() == UserInfoStatus.ACTIVE;
        if (!receiverCanReceiveMessage) {
            throw new IllegalArgumentException("接收者不存在");
        }

        /*
         * 无论谁是发送者，都把较小的用户 ID 放在 user_a_id，
         * 较大的用户 ID 放在 user_b_id。这样同一对用户只会对应一种排列。
         */
        long userAId = Math.min(senderUserId, receiverUserId);
        long userBId = Math.max(senderUserId, receiverUserId);

        // 查询现有聊天框并加行锁，后续权限判断和更新都在同一个事务中完成。
        PrivateChatbox chatbox =
                chatboxMapper.selectForUpdate(userAId, userBId);

        boolean senderFollowsReceiver =
                followMapper.selectCount(
                        new LambdaQueryWrapper<UserFollow>()
                                .eq(UserFollow::getFollowerUserId, senderUserId)
                                .eq(UserFollow::getFolloweeUserId, receiverUserId)
                ) > 0;

        boolean receiverFollowsSender = false;
        if (senderFollowsReceiver) {
            receiverFollowsSender =
                    followMapper.selectCount(
                            new LambdaQueryWrapper<UserFollow>()
                                    .eq(UserFollow::getFollowerUserId, receiverUserId)
                                    .eq(UserFollow::getFolloweeUserId, senderUserId)
                    ) > 0;
        }
        boolean usersMutuallyFollow =
                senderFollowsReceiver && receiverFollowsSender;

        boolean chatboxCreatedNow = false;

        if (chatbox == null) {
            // 查询不到聊天框时，准备创建这一对用户的唯一聊天框。
            PrivateChatbox newChatbox = new PrivateChatbox();
            newChatbox.setUserAId(userAId);
            newChatbox.setUserBId(userBId);
            newChatbox.setInitiatorUserId(senderUserId);
            newChatbox.setChatAccess(
                    usersMutuallyFollow
                            ? PrivateChatAccess.OPEN
                            : PrivateChatAccess.PENDING_REPLY
            );

            int insertedRows = chatboxMapper.insertIfAbsent(newChatbox);
            if (insertedRows == 1) {
                // 本次请求成功创建聊天框，MyBatis 会把生成的 ID 写回 newChatbox。
                chatbox = newChatbox;
                chatboxCreatedNow = true;
            } else {
                /*
                 * 查询和插入是两条 SQL。并发请求可能在两者之间先创建聊天框，
                 * 因此插入未成功时，需要重新读取数据库中的那一条记录。
                 */
                chatbox = chatboxMapper.selectForUpdate(userAId, userBId);

                /*
                 * INSERT IGNORE 不只可能忽略唯一键冲突。
                 * 如果重新查询仍然为空，就不能继续使用 chatbox，否则会空指针。
                 */
                if (chatbox == null) {
                    throw new IllegalStateException("私聊框创建失败，请稍后重试");
                }
            }
        }

        if (!chatboxCreatedNow
                && chatbox.getChatAccess() == PrivateChatAccess.PENDING_REPLY) {

            boolean currentSenderIsInitiator =
                    Objects.equals(chatbox.getInitiatorUserId(), senderUserId);

            if (currentSenderIsInitiator && !usersMutuallyFollow) {
                // 非互关发起者只能发送第一条消息，之后必须等待对方回复。
                throw new IllegalArgumentException("正在等待对方回复");
            }

            /*
             * 能走到这里，说明当前消息是对方的回复，或者双方现在已经互相关注。
             * 从此把聊天框永久改为 OPEN，后续双方都可以正常发送。
             */
            chatbox.setChatAccess(PrivateChatAccess.OPEN);
        }

        PrivateMessage message = new PrivateMessage();
        message.setChatboxId(chatbox.getId());
        message.setSenderUserId(senderUserId);
        message.setReceiverUserId(receiverUserId);
        message.setContent(content);
        message.setMessageStatus(PrivateMessageStatus.SENT);
        messageMapper.insert(message);

        // 保存最后一条消息信息，聊天框列表会用它进行排序和展示。
        chatbox.setLastMessageId(message.getId());
        if (message.getCreatedTime() == null) {
            chatbox.setLastMessageTime(java.time.LocalDateTime.now());
        } else {
            chatbox.setLastMessageTime(message.getCreatedTime());
        }
        chatboxMapper.updateById(chatbox);

        UserInfo sender = userInfoMapper.selectById(senderUserId);

        PrivateMessageVO result = new PrivateMessageVO();
        result.setId(message.getId());
        result.setChatboxId(message.getChatboxId());
        result.setSenderUserId(message.getSenderUserId());
        result.setSenderDisplayName(
                sender == null ? "该用户已注销" : sender.getDisplayName()
        );
        result.setSenderAvatar(sender == null ? null : resolveAvatar(sender));
        result.setReceiverUserId(message.getReceiverUserId());
        result.setContent(message.getContent());
        result.setMessageStatus(message.getMessageStatus());
        result.setCreatedTime(message.getCreatedTime());
        return result;
    }

    @Override
    public void markPrivateMessageRead(Long userId, Long messageId) {
        /*
         * 只允许接收者把自己收到的这一条私信从 SENT 改为 READ。
         * 如果消息已经是 READ，重复点击不会再次修改，接口仍然可以安全返回成功。
         */
        PrivateMessage updateData = new PrivateMessage();
        updateData.setMessageStatus(PrivateMessageStatus.READ);

        LambdaUpdateWrapper<PrivateMessage> updateCondition =
                new LambdaUpdateWrapper<PrivateMessage>()
                        .eq(PrivateMessage::getId, messageId)
                        .eq(PrivateMessage::getReceiverUserId, userId)
                        .eq(PrivateMessage::getMessageStatus, PrivateMessageStatus.SENT);

        messageMapper.update(updateData, updateCondition);
    }
    private String resolveAvatar(UserInfo userInfo) {
        return userImageUrlResolver.resolveAvatar(userInfo.getAvatarObjectKey());
    }
}
