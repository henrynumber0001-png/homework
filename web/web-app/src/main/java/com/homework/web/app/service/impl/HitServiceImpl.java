package com.homework.web.app.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.homework.common.enums.EnumUtils;
import com.homework.common.enums.HitActionActionType;
import com.homework.common.enums.HitPostPostStatus;
import com.homework.web.app.dto.HitActionDTO;
import com.homework.web.app.dto.HitCommentCreateDTO;
import com.homework.web.app.dto.HitPostCreateDTO;
import com.homework.model.entity.HitAction;
import com.homework.model.entity.HitComment;
import com.homework.model.entity.HitPost;
import com.homework.web.app.mapper.HitActionMapper;
import com.homework.web.app.mapper.HitCommentMapper;
import com.homework.web.app.mapper.HitPostMapper;
import com.homework.web.app.service.HitService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Service
@RequiredArgsConstructor
public class HitServiceImpl implements HitService {

    private final HitPostMapper hitPostMapper;
    private final HitCommentMapper hitCommentMapper;
    private final HitActionMapper hitActionMapper;
    private final ObjectMapper objectMapper;

    @Override
    public List<Map<String, Object>> listHits(Integer pageNum, Integer pageSize) {
        int size = pageSize == null ? 20 : Math.max(1, Math.min(pageSize, 100));
        int page = pageNum == null ? 1 : Math.max(1, pageNum);
        int offset = (page - 1) * size;
        return hitPostMapper.selectMaps(new QueryWrapper<HitPost>()
                .select("id", "user_id", "content", "tags_json", "comment_count", "like_count", "favorite_count", "repost_count", "created_time")
                .eq("post_status", 1)
                .eq("is_deleted", 0)
                .orderByDesc("created_time")
                .last("LIMIT " + offset + ", " + size));
    }

    @Override
    public Long publish(HitPostCreateDTO dto) {
        HitPost post = new HitPost();
        post.setUserId(dto.getUserId());
        post.setContent(dto.getContent());
        post.setTagsJson(toTagsJson(dto));
        post.setPostStatus(HitPostPostStatus.PUBLISHED);
        post.setCommentCount(0);
        post.setLikeCount(0);
        post.setFavoriteCount(0);
        post.setRepostCount(0);
        hitPostMapper.insert(post);
        return post.getId();
    }

    @Override
    @Transactional
    public Long comment(Long postId, HitCommentCreateDTO dto) {
        HitComment comment = new HitComment();
        comment.setPostId(postId);
        comment.setUserId(dto.getUserId());
        comment.setParentId(dto.getParentId());
        comment.setContent(dto.getContent());
        hitCommentMapper.insert(comment);
        HitPost post = hitPostMapper.selectById(postId);
        if (post != null) {
            post.setCommentCount((post.getCommentCount() == null ? 0 : post.getCommentCount()) + 1);
            hitPostMapper.updateById(post);
        }
        return comment.getId();
    }

    @Override
    @Transactional
    public Map<String, Object> action(Long postId, HitActionDTO dto) {
        HitActionActionType actionType = EnumUtils.fromValue(HitActionActionType.class, dto.getActionType());
        HitPost post = hitPostMapper.selectById(postId);
        if (post == null) {
            throw new IllegalArgumentException("Hit post not found");
        }

        HitAction existing = hitActionMapper.selectOne(new QueryWrapper<HitAction>()
                .eq("post_id", postId)
                .eq("user_id", dto.getUserId())
                .eq("action_type", dto.getActionType())
                .eq("is_deleted", 0)
                .last("LIMIT 1"));
        boolean nextActive = dto.getActive() == null ? existing == null : dto.getActive();
        if (nextActive && existing == null) {
            HitAction action = new HitAction();
            action.setPostId(postId);
            action.setUserId(dto.getUserId());
            action.setActionType(actionType);
            hitActionMapper.insert(action);
            changeCounter(post, dto.getActionType(), 1);
        } else if (!nextActive && existing != null) {
            hitActionMapper.deleteById(existing.getId());
            changeCounter(post, dto.getActionType(), -1);
        }
        hitPostMapper.updateById(post);

        Map<String, Object> result = new HashMap<>();
        result.put("active", nextActive);
        result.put("likeCount", post.getLikeCount());
        result.put("favoriteCount", post.getFavoriteCount());
        result.put("repostCount", post.getRepostCount());
        return result;
    }

    private void changeCounter(HitPost post, Integer actionType, int delta) {
        if (actionType == null) {
            return;
        }
        if (actionType == 1) {
            post.setLikeCount(Math.max(0, (post.getLikeCount() == null ? 0 : post.getLikeCount()) + delta));
        } else if (actionType == 2) {
            post.setFavoriteCount(Math.max(0, (post.getFavoriteCount() == null ? 0 : post.getFavoriteCount()) + delta));
        } else if (actionType == 3) {
            post.setRepostCount(Math.max(0, (post.getRepostCount() == null ? 0 : post.getRepostCount()) + delta));
        }
    }

    private String toTagsJson(HitPostCreateDTO dto) {
        if (dto.getTags() == null || dto.getTags().isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(dto.getTags());
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid hit post tags", e);
        }
    }
}
