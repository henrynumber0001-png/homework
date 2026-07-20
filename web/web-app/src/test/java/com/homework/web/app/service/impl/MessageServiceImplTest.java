package com.homework.web.app.service.impl;

import com.homework.model.entity.PrivateMessage;
import com.homework.model.entity.UserInfo;
import com.homework.model.entity.UserNotification;
import com.homework.model.enums.PrivateMessageAllowReason;
import com.homework.web.app.dto.PrivateMessageCreateDTO;
import com.homework.web.app.mapper.PrivateMessageMapper;
import com.homework.web.app.mapper.UserInfoMapper;
import com.homework.web.app.mapper.UserFollowMapper;
import com.homework.web.app.mapper.UserNotificationMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

    private MessageServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new MessageServiceImpl(userNotificationMapper, privateMessageMapper,
                userFollowMapper, userInfoMapper);
    }

    @Test
    void nonMutualUserCanSendFirstTextMessageAndCreatesNotification() {
        PrivateMessageCreateDTO dto = messageTo(9L, "你好，想交流一下学习经验");
        when(userInfoMapper.selectById(9L)).thenReturn(new UserInfo());
        when(userFollowMapper.selectCount(any())).thenReturn(0L);
        when(privateMessageMapper.countFirstNonMutualMessages(7L, 9L)).thenReturn(0L);
        when(privateMessageMapper.insert(any(PrivateMessage.class))).thenAnswer(invocation -> {
            PrivateMessage message = invocation.getArgument(0);
            message.setId(88L);
            return 1;
        });

        Long id = service.sendPrivateMessage(7L, dto);

        assertEquals(88L, id);
        verify(privateMessageMapper).insert(org.mockito.ArgumentMatchers.<PrivateMessage>argThat(message ->
                message.getAllowReason() == PrivateMessageAllowReason.FIRST_NON_MUTUAL_MESSAGE));
        verify(userNotificationMapper).insert(org.mockito.ArgumentMatchers.<UserNotification>argThat(notification ->
                notification.getTargetId().equals(88L) && notification.getReceiverUserId().equals(9L)));
    }

    @Test
    void nonMutualUserCannotSendSecondMessage() {
        PrivateMessageCreateDTO dto = messageTo(9L, "第二条消息");
        when(userInfoMapper.selectById(9L)).thenReturn(new UserInfo());
        when(userFollowMapper.selectCount(any())).thenReturn(0L);
        when(privateMessageMapper.countFirstNonMutualMessages(7L, 9L)).thenReturn(1L);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.sendPrivateMessage(7L, dto));

        assertEquals("非互相关注用户只能发送第一条私信", error.getMessage());
        verify(privateMessageMapper, never()).insert(any(PrivateMessage.class));
        verify(userNotificationMapper, never()).insert(any(UserNotification.class));
    }

    @Test
    void privateMessageRejectsImageMarkup() {
        PrivateMessageCreateDTO dto = messageTo(9L, "看图 ![截图](https://example.com/a.png)");
        when(userInfoMapper.selectById(9L)).thenReturn(new UserInfo());

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.sendPrivateMessage(7L, dto));

        assertEquals("私信暂不支持发送图片", error.getMessage());
        verifyNoInteractions(userFollowMapper, privateMessageMapper, userNotificationMapper);
    }

    private PrivateMessageCreateDTO messageTo(Long receiverUserId, String content) {
        PrivateMessageCreateDTO dto = new PrivateMessageCreateDTO();
        dto.setReceiverUserId(receiverUserId);
        dto.setContent(content);
        return dto;
    }
}
