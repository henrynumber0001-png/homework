package com.homework.web.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.homework.common.exception.HomeworkException;
import com.homework.common.result.PageResult;
import com.homework.common.result.ResultCodeEnum;
import com.homework.model.entity.HitComment;
import com.homework.model.entity.HitPost;
import com.homework.model.entity.UserInfo;
import com.homework.model.enums.HitPostStatus;
import com.homework.web.admin.dto.CommunityContentActionDTO;
import com.homework.model.enums.CommunityContentAction;
import com.homework.web.admin.mapper.HitCommentMapper;
import com.homework.web.admin.mapper.HitPostMapper;
import com.homework.web.admin.mapper.UserInfoMapper;
import com.homework.web.admin.vo.ActionResultVO;
import com.homework.web.admin.vo.CommunityCommentVO;
import com.homework.web.admin.vo.CommunityPostVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 后台社区动态和评论的基础治理。 */
@Service
@RequiredArgsConstructor
public class AdminCommunityService {

    private final HitPostMapper postMapper;
    private final HitCommentMapper commentMapper;
    private final UserInfoMapper userMapper;
    private final AdminAuditService auditService;

    public PageResult<CommunityPostVO> listPosts(
            String keyword,
            Long userId,
            HitPostStatus status,
            Integer pageNum,
            Integer pageSize
    ) {
        int normalizedPage = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int normalizedSize = pageSize == null ? 20 : Math.min(Math.max(pageSize, 1), 100);
        LambdaQueryWrapper<HitPost> query = new LambdaQueryWrapper<>();
        query.like(keyword != null && !keyword.isBlank(), HitPost::getContent, keyword == null ? null : keyword.trim())
                .eq(userId != null, HitPost::getPostUserId, userId);
        query.eq(status != null, HitPost::getPostStatus, status);
        query.orderByDesc(HitPost::getCreatedTime).orderByDesc(HitPost::getId);
        Page<HitPost> page = postMapper.selectPage(new Page<>(normalizedPage, normalizedSize), query);
        PageResult<CommunityPostVO> result = new PageResult<>();
        result.setRecords(page.getRecords().stream().map(post -> {
            CommunityPostVO vo = new CommunityPostVO();
            UserInfo user = userMapper.selectById(post.getPostUserId());
            vo.setId(post.getId());
            vo.setUserId(post.getPostUserId());
            vo.setDisplayName(user == null ? null : user.getDisplayName());
            vo.setContent(post.getContent());
            vo.setTagsJson(post.getTagsJson());
            vo.setStatus(post.getPostStatus());
            vo.setCommentCount(post.getCommentCount());
            vo.setLikeCount(post.getLikeCount());
            vo.setFavoriteCount(post.getFavoriteCount());
            vo.setRepostCount(post.getRepostCount());
            vo.setCreatedTime(post.getCreatedTime());
            vo.setVersion(post.getVersion());
            return vo;
        }).toList());
        result.setTotal(page.getTotal());
        result.setPageNum(page.getCurrent());
        result.setPageSize(page.getSize());
        return result;
    }

    public PageResult<CommunityCommentVO> listComments(
            Long postId,
            Long userId,
            HitPostStatus status,
            Integer pageNum,
            Integer pageSize
    ) {
        int normalizedPage = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int normalizedSize = pageSize == null ? 20 : Math.min(Math.max(pageSize, 1), 100);
        LambdaQueryWrapper<HitComment> query = new LambdaQueryWrapper<>();
        query.eq(postId != null, HitComment::getPostId, postId)
                .eq(userId != null, HitComment::getCommentUserId, userId);
        query.eq(status != null, HitComment::getCommentStatus, status);
        query.orderByDesc(HitComment::getCreatedTime).orderByDesc(HitComment::getId);
        Page<HitComment> page = commentMapper.selectPage(new Page<>(normalizedPage, normalizedSize), query);
        PageResult<CommunityCommentVO> result = new PageResult<>();
        result.setRecords(page.getRecords().stream().map(comment -> {
            CommunityCommentVO vo = new CommunityCommentVO();
            UserInfo user = userMapper.selectById(comment.getCommentUserId());
            vo.setId(comment.getId());
            vo.setPostId(comment.getPostId());
            vo.setUserId(comment.getCommentUserId());
            vo.setDisplayName(user == null ? null : user.getDisplayName());
            vo.setParentCommentId(comment.getParentCommentId());
            vo.setContent(comment.getComment());
            vo.setStatus(comment.getCommentStatus());
            vo.setLikeCount(comment.getLikeCount());
            vo.setCreatedTime(comment.getCreatedTime());
            vo.setVersion(comment.getVersion());
            return vo;
        }).toList());
        result.setTotal(page.getTotal());
        result.setPageNum(page.getCurrent());
        result.setPageSize(page.getSize());
        return result;
    }

    @Transactional
    public ActionResultVO actionPost(Long postId, CommunityContentActionDTO dto) {
        HitPost post = postMapper.selectById(postId);
        if (post == null || !post.getVersion().equals(dto.getVersion())) {
            throw new HomeworkException(post == null
                    ? ResultCodeEnum.ADMIN_CONTENT_STATE_INVALID
                    : ResultCodeEnum.ADMIN_RESOURCE_VERSION_CONFLICT);
        }
        CommunityContentAction action = dto.getAction();
        HitPostStatus target;
        if (action == CommunityContentAction.HIDE && post.getPostStatus() == HitPostStatus.PUBLISHED) {
            target = HitPostStatus.HIDDEN;
        } else if (action == CommunityContentAction.RESTORE
                && (post.getPostStatus() == HitPostStatus.HIDDEN || post.getPostStatus() == HitPostStatus.DELETED)) {
            target = HitPostStatus.PUBLISHED;
        } else if (action == CommunityContentAction.DELETE && post.getPostStatus() != HitPostStatus.DELETED) {
            target = HitPostStatus.DELETED;
        } else {
            throw new HomeworkException(ResultCodeEnum.ADMIN_CONTENT_STATE_INVALID);
        }
        HitPost before = new HitPost();
        org.springframework.beans.BeanUtils.copyProperties(post, before);
        post.setPostStatus(target);
        if (postMapper.updateById(post) == 0) {
            throw new HomeworkException(ResultCodeEnum.ADMIN_RESOURCE_VERSION_CONFLICT);
        }
        HitPost updated = postMapper.selectById(postId);
        auditService.record("COMMUNITY", action.name(), "HIT_POST", postId, dto.getReason(), before, updated);
        ActionResultVO result = new ActionResultVO();
        result.setTargetId(postId);
        result.setAction(action.getCode());
        result.setStatus(updated.getPostStatus().getCode());
        result.setVersion(updated.getVersion());
        result.setUpdatedTime(updated.getUpdatedTime());
        return result;
    }

    @Transactional
    public ActionResultVO actionComment(Long commentId, CommunityContentActionDTO dto) {
        HitComment comment = commentMapper.selectById(commentId);
        if (comment == null || !comment.getVersion().equals(dto.getVersion())) {
            throw new HomeworkException(comment == null
                    ? ResultCodeEnum.ADMIN_CONTENT_STATE_INVALID
                    : ResultCodeEnum.ADMIN_RESOURCE_VERSION_CONFLICT);
        }
        CommunityContentAction action = dto.getAction();
        HitPostStatus target;
        int commentCountDelta = 0;
        if (action == CommunityContentAction.HIDE && comment.getCommentStatus() == HitPostStatus.PUBLISHED) {
            target = HitPostStatus.HIDDEN;
        } else if (action == CommunityContentAction.RESTORE
                && (comment.getCommentStatus() == HitPostStatus.HIDDEN
                || comment.getCommentStatus() == HitPostStatus.DELETED)) {
            target = HitPostStatus.PUBLISHED;
            if (comment.getCommentStatus() == HitPostStatus.DELETED) {
                commentCountDelta = 1;
            }
        } else if (action == CommunityContentAction.DELETE && comment.getCommentStatus() != HitPostStatus.DELETED) {
            target = HitPostStatus.DELETED;
            commentCountDelta = -1;
        } else {
            throw new HomeworkException(ResultCodeEnum.ADMIN_CONTENT_STATE_INVALID);
        }
        HitComment before = new HitComment();
        org.springframework.beans.BeanUtils.copyProperties(comment, before);
        comment.setCommentStatus(target);
        if (commentMapper.updateById(comment) == 0) {
            throw new HomeworkException(ResultCodeEnum.ADMIN_RESOURCE_VERSION_CONFLICT);
        }
        if (commentCountDelta != 0) {
            postMapper.changeCommentCount(comment.getPostId(), commentCountDelta);
        }
        HitComment updated = commentMapper.selectById(commentId);
        auditService.record("COMMUNITY", action.name(), "HIT_COMMENT", commentId, dto.getReason(), before, updated);
        ActionResultVO result = new ActionResultVO();
        result.setTargetId(commentId);
        result.setAction(action.getCode());
        result.setStatus(updated.getCommentStatus().getCode());
        result.setVersion(updated.getVersion());
        result.setUpdatedTime(updated.getUpdatedTime());
        return result;
    }
}
