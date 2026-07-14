package com.homework.web.app.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.homework.common.exception.HomeworkException;
import com.homework.common.result.ResultCodeEnum;
import com.homework.model.entity.HitAction;
import com.homework.model.entity.HitPost;
import com.homework.model.enums.ActionStatus;
import com.homework.model.enums.HitActionType;
import com.homework.model.enums.HitPostStatus;
import com.homework.web.app.context.LoginUserHolder;
import com.homework.web.app.dto.HitActionDTO;
import com.homework.web.app.mapper.*;
import org.junit.jupiter.api.AfterEach;
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
        LoginUserHolder.setUserId(7L);
    }

    @AfterEach
    void tearDown() {
        LoginUserHolder.removeUserId();
    }

    @Test
    void publishCountsEmojiAsOneUnicodeCharacter() {
        // 139 个汉字加一个补充平面 emoji：Java char 长度为 141，但用户感知长度是 140。
        String content = "学".repeat(139) + "🚀";
        when(hitPostMapper.insert(any(HitPost.class))).thenAnswer(invocation -> {
            HitPost post = invocation.getArgument(0);
            post.setId(100L);
            return 1;
        });

        Long id = service.publish(content);

        assertEquals(100L, id);
        verify(hitPostMapper).insert(org.mockito.ArgumentMatchers.<HitPost>argThat(
                post -> post.getPostStatus() == HitPostStatus.PUBLISHED
                && post.getContent().codePointCount(0, post.getContent().length()) == 140));
    }

    @Test
    void publishRejectsMoreThan140UnicodeCharacters() {
        String content = "打".repeat(141);

        HomeworkException error = assertThrows(HomeworkException.class,
                () -> service.publish(content));

        assertEquals(ResultCodeEnum.HIT_CONTENT_TOO_LONG_ERROR, error.getResultCodeEnum());
        verifyNoInteractions(hitPostMapper);
    }

    @Test
    void activateLikeInsertsActionAndIncreasesCounter() {
        HitPost post = publishedPost(100L, 9L, 1);
        when(hitPostMapper.selectOne(any())).thenReturn(post);
        when(hitActionMapper.selectIncludingDeletedForUpdate(100L, 7L, 1)).thenReturn(null);
        when(hitPostMapper.selectById(100L)).thenReturn(post);

        HitActionDTO dto = new HitActionDTO();
        dto.setActionType(HitActionType.LIKE);
        dto.setActionStatus(ActionStatus.ACTIVATE);

        service.action(100L, dto);

        verify(hitActionMapper).insert(org.mockito.ArgumentMatchers.<HitAction>argThat(action ->
                action.getPostId().equals(100L)
                        && action.getActionUserId().equals(7L)
                        && action.getActionType() == HitActionType.LIKE));
        verify(hitPostMapper).changeActionCounters(100L, 1, 0, 0);
    }

    @Test
    void deactivateLikeDeletesActionAndDecreasesCounter() {
        HitPost post = publishedPost(100L, 9L, 0);
        HitAction existing = new HitAction();
        existing.setId(50L);
        existing.setPostId(100L);
        existing.setActionUserId(7L);
        existing.setActionType(HitActionType.LIKE);
        existing.setDeleted(false);

        when(hitPostMapper.selectOne(any())).thenReturn(post);
        when(hitActionMapper.selectIncludingDeletedForUpdate(100L, 7L, 1)).thenReturn(existing);
        when(hitActionMapper.deactivateById(50L)).thenReturn(1);
        when(hitPostMapper.selectById(100L)).thenReturn(post);

        HitActionDTO dto = new HitActionDTO();
        dto.setActionType(HitActionType.LIKE);
        dto.setActionStatus(ActionStatus.DEACTIVATE);

        service.action(100L, dto);

        verify(hitActionMapper).deactivateById(50L);
        verify(hitPostMapper).changeActionCounters(100L, -1, 0, 0);
    }

    private HitPost publishedPost(Long postId, Long postUserId, int likeCount) {
        HitPost post = new HitPost();
        post.setId(postId);
        post.setPostUserId(postUserId);
        post.setPostStatus(HitPostStatus.PUBLISHED);
        post.setContent("测试 Hit #Java");
        post.setLikeCount(likeCount);
        post.setFavoriteCount(0);
        post.setRepostCount(0);
        return post;
    }
}
