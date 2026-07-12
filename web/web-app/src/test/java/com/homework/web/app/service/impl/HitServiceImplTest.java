package com.homework.web.app.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.homework.model.entity.HitPost;
import com.homework.model.enums.HitPostStatus;
import com.homework.web.app.dto.HitPostCreateDTO;
import com.homework.web.app.mapper.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HitServiceImplTest {

    @Mock
    private HitPostMapper hitPostMapper;
    @Mock
    private HitCommentMapper hitCommentMapper;
    @Mock
    private HitActionMapper hitActionMapper;
    @Mock
    private UserInfoMapper userInfoMapper;
    @Mock
    private UserNotificationMapper userNotificationMapper;

    private HitServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new HitServiceImpl(hitPostMapper, hitCommentMapper, hitActionMapper,
                userInfoMapper, userNotificationMapper, new ObjectMapper());
    }

    @Test
    void publishCountsEmojiAsOneUnicodeCharacter() {
        HitPostCreateDTO dto = new HitPostCreateDTO();
        // 139 个汉字加一个补充平面 emoji：Java char 长度为 141，但用户感知长度是 140。
        dto.setContent("学".repeat(139) + "🚀");
        when(hitPostMapper.insert(any(HitPost.class))).thenAnswer(invocation -> {
            HitPost post = invocation.getArgument(0);
            post.setId(100L);
            return 1;
        });

        Long id = service.publish(7L, dto);

        assertEquals(100L, id);
        verify(hitPostMapper).insert(org.mockito.ArgumentMatchers.<HitPost>argThat(
                post -> post.getPostStatus() == HitPostStatus.PUBLISHED
                && post.getContent().codePointCount(0, post.getContent().length()) == 140));
    }

    @Test
    void publishRejectsMoreThan140UnicodeCharacters() {
        HitPostCreateDTO dto = new HitPostCreateDTO();
        dto.setContent("打".repeat(141));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.publish(7L, dto));

        assertEquals("Hit 内容最多 140 字", error.getMessage());
        verifyNoInteractions(hitPostMapper);
    }
}
