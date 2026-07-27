package com.homework.web.app.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.homework.common.result.PageResult;
import com.homework.model.entity.HitComment;
import com.homework.model.entity.HitPost;
import com.homework.model.entity.PrivateMessage;
import com.homework.model.entity.PrivateChatbox;
import com.homework.model.entity.UserInfo;
import com.homework.model.entity.UserNotification;
import com.homework.model.enums.HitPostStatus;
import com.homework.model.enums.PrivateChatAccess;
import com.homework.model.enums.UserInfoStatus;
import com.homework.model.enums.UserNotificationReadStatus;
import com.homework.model.enums.UserNotificationSendTo;
import com.homework.model.enums.UserNotificationType;
import com.homework.web.app.dto.PrivateMessageCreateDTO;
import com.homework.web.app.mapper.*;
import com.homework.web.app.vo.NotificationVO;
import com.homework.web.app.vo.PrivateMessageVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageServiceImplTest {

    @Mock
    private UserNotificationMapper userNotificationMapper;
    @Mock
    private PrivateMessageMapper privateMessageMapper;
    @Mock
    private UserFollowMapper userFollowMapper;
    @Mock
    private UserInfoMapper userInfoMapper;
    @Mock
    private PrivateChatboxMapper privateChatboxMapper;
    @Mock
    private HitCommentMapper hitCommentMapper;
    @Mock
    private HitPostMapper hitPostMapper;

    private MessageServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new MessageServiceImpl(userNotificationMapper, privateChatboxMapper,
                privateMessageMapper, userInfoMapper, userFollowMapper, hitCommentMapper, hitPostMapper);
    }

    @Test
    void nonMutualUserCanSendFirstTextMessageAndCreatesPendingChatbox() {
        PrivateMessageCreateDTO dto = messageTo(9L, "你好，想交流一下学习经验");
        when(userInfoMapper.selectById(9L)).thenReturn(activeUser());
        when(userFollowMapper.selectCount(any())).thenReturn(0L);
        when(privateChatboxMapper.selectForUpdate(7L, 9L)).thenReturn(null);
        when(privateChatboxMapper.insertIfAbsent(any(PrivateChatbox.class))).thenAnswer(invocation -> {
            PrivateChatbox chatbox = invocation.getArgument(0);
            chatbox.setId(66L);
            return 1;
        });
        when(privateMessageMapper.insert(any(PrivateMessage.class))).thenAnswer(invocation -> {
            PrivateMessage message = invocation.getArgument(0);
            message.setId(88L);
            return 1;
        });

        PrivateMessageVO result = service.sendPrivateMessage(7L, dto);

        assertEquals(88L, result.getId());
        verify(privateChatboxMapper).insertIfAbsent(org.mockito.ArgumentMatchers.<PrivateChatbox>argThat(chatbox ->
                chatbox.getInitiatorUserId().equals(7L)
                        && chatbox.getChatAccess() == PrivateChatAccess.PENDING_REPLY));
        verify(privateMessageMapper).insert(org.mockito.ArgumentMatchers.<PrivateMessage>argThat(message ->
                message.getChatboxId().equals(66L)
                        && message.getReceiverUserId().equals(9L)));
    }

    @Test
    void nonMutualUserCannotSendSecondMessage() {
        PrivateMessageCreateDTO dto = messageTo(9L, "第二条消息");
        when(userInfoMapper.selectById(9L)).thenReturn(activeUser());
        when(userFollowMapper.selectCount(any())).thenReturn(0L);
        when(privateChatboxMapper.selectForUpdate(7L, 9L)).thenReturn(pendingChatbox());

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.sendPrivateMessage(7L, dto));

        assertEquals("正在等待对方回复", error.getMessage());
        verify(privateMessageMapper, never()).insert(any(PrivateMessage.class));
    }

    @Test
    void replyPermanentlyOpensPendingChatbox() {
        PrivateMessageCreateDTO dto = messageTo(7L, "好的，可以交流");
        PrivateChatbox chatbox = pendingChatbox();
        when(userInfoMapper.selectById(7L)).thenReturn(activeUser());
        when(userFollowMapper.selectCount(any())).thenReturn(0L);
        when(privateChatboxMapper.selectForUpdate(7L, 9L)).thenReturn(chatbox);
        when(privateMessageMapper.insert(any(PrivateMessage.class))).thenAnswer(invocation -> {
            PrivateMessage message = invocation.getArgument(0);
            message.setId(89L);
            return 1;
        });

        PrivateMessageVO result = service.sendPrivateMessage(9L, dto);

        assertEquals(89L, result.getId());
        assertEquals(PrivateChatAccess.OPEN, chatbox.getChatAccess());
        verify(privateChatboxMapper).updateById(chatbox);
    }

    @Test
    void deletedCommentNotificationKeepsItsPostTarget() {
        UserNotification notification = new UserNotification();
        notification.setId(5L);
        notification.setReceiverUserId(7L);
        notification.setSenderUserId(9L);
        notification.setNotificationType(UserNotificationType.LIKE);
        notification.setSendTo(UserNotificationSendTo.HIT_COMMENT);
        notification.setItemId(44L);
        notification.setPostId(100L);
        notification.setContent("原评论内容");
        notification.setReadStatus(UserNotificationReadStatus.READ);
        LocalDateTime latestReadTime = LocalDateTime.of(2026, 7, 25, 0, 30);
        notification.setUpdatedTime(latestReadTime);
        Page<UserNotification> page = new Page<>(1, 20);
        page.setRecords(java.util.List.of(notification));
        page.setTotal(1);
        when(userNotificationMapper.selectLatestReadTime(eq(7L), anyCollection()))
                .thenReturn(latestReadTime);
        when(userNotificationMapper.selectPage(
                org.mockito.ArgumentMatchers.<Page<UserNotification>>any(),
                org.mockito.ArgumentMatchers.<com.baomidou.mybatisplus.core.conditions.Wrapper<UserNotification>>any()))
                .thenReturn(page);

        UserInfo actionUser = activeUser();
        actionUser.setId(9L);
        actionUser.setDisplayName("动作用户");
        when(userInfoMapper.selectByIds(anyCollection())).thenReturn(java.util.List.of(actionUser));
        HitPost post = new HitPost();
        post.setId(100L);
        post.setPostStatus(HitPostStatus.PUBLISHED);
        when(hitPostMapper.selectByIds(anyCollection())).thenReturn(java.util.List.of(post));
        HitComment deletedComment = new HitComment();
        deletedComment.setId(44L);
        deletedComment.setPostId(100L);
        deletedComment.setDeleted(true);
        when(hitCommentMapper.selectIncludingDeletedByIds(anyCollection()))
                .thenReturn(java.util.List.of(deletedComment));

        PageResult<NotificationVO> result =
                service.loadNotificationTab(7L, "interactions", 1, 20, false);

        NotificationVO row = result.getRecords().get(0);
        assertTrue(row.isCommentDeleted());
        assertEquals("原评论已删除", row.getContent());
        assertEquals(100L, row.getPostId());
        assertTrue(row.isPostAvailable());
        verify(userNotificationMapper).markTypesRead(
                7L,
                java.util.List.of(
                        UserNotificationType.LIKE,
                        UserNotificationType.FAVORITE,
                        UserNotificationType.REPOST
                )
        );
    }

    @Test
    void loadingNotificationHistoryDoesNotChangeReadStatus() {
        LocalDateTime latestReadTime = LocalDateTime.of(2026, 7, 25, 0, 30);
        when(userNotificationMapper.selectLatestReadTime(eq(7L), anyCollection()))
                .thenReturn(latestReadTime);

        Page<UserNotification> emptyPage = new Page<>(1, 20);
        emptyPage.setRecords(java.util.List.of());
        when(userNotificationMapper.selectPage(
                org.mockito.ArgumentMatchers.<Page<UserNotification>>any(),
                org.mockito.ArgumentMatchers.<com.baomidou.mybatisplus.core.conditions.Wrapper<UserNotification>>any()))
                .thenReturn(emptyPage);

        PageResult<NotificationVO> result =
                service.loadNotificationTab(7L, "comments", 1, 20, true);

        assertTrue(result.getRecords().isEmpty());
        verify(userNotificationMapper, never()).markTypesRead(anyLong(), anyCollection());
    }

    @Test
    void clickingPrivateMessageMarksOnlyThatMessageRead() {
        when(privateMessageMapper.update(
                any(PrivateMessage.class),
                org.mockito.ArgumentMatchers
                        .<com.baomidou.mybatisplus.core.conditions.Wrapper<PrivateMessage>>any()))
                .thenReturn(1);

        service.markPrivateMessageRead(7L, 88L);

        verify(privateMessageMapper).update(
                org.mockito.ArgumentMatchers.<PrivateMessage>argThat(message ->
                        message.getMessageStatus()
                                == com.homework.model.enums.PrivateMessageStatus.READ),
                org.mockito.ArgumentMatchers
                        .<com.baomidou.mybatisplus.core.conditions.Wrapper<PrivateMessage>>any()
        );
    }

    private PrivateMessageCreateDTO messageTo(Long receiverUserId, String content) {
        PrivateMessageCreateDTO dto = new PrivateMessageCreateDTO();
        dto.setReceiverUserId(receiverUserId);
        dto.setContent(content);
        return dto;
    }

    private PrivateChatbox pendingChatbox() {
        PrivateChatbox chatbox = new PrivateChatbox();
        chatbox.setId(66L);
        chatbox.setUserAId(7L);
        chatbox.setUserBId(9L);
        chatbox.setInitiatorUserId(7L);
        chatbox.setChatAccess(PrivateChatAccess.PENDING_REPLY);
        return chatbox;
    }

    private UserInfo activeUser() {
        UserInfo user = new UserInfo();
        user.setStatus(UserInfoStatus.ACTIVE);
        return user;
    }
}
