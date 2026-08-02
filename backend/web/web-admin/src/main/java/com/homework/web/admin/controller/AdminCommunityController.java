package com.homework.web.admin.controller;

import com.homework.common.result.PageResult;
import com.homework.common.result.Result;
import com.homework.model.enums.HitPostStatus;
import com.homework.web.admin.auth.AdminPermission;
import com.homework.web.admin.dto.CommunityContentActionDTO;
import com.homework.web.admin.service.AdminCommunityService;
import com.homework.web.admin.vo.ActionResultVO;
import com.homework.web.admin.vo.CommunityCommentVO;
import com.homework.web.admin.vo.CommunityPostVO;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 后台社区内容治理接口。 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/community")
@AdminPermission("community:moderate")
public class AdminCommunityController {

    private final AdminCommunityService communityService;

    /** 分页查询需要治理的动态及完整正文。 */
    @Operation(summary = "分页查询社区动态")
    @GetMapping("/posts")
    public Result<PageResult<CommunityPostVO>> listPosts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) HitPostStatus status,
            @RequestParam(required = false) Integer pageNum,
            @RequestParam(required = false) Integer pageSize
    ) {
        return Result.success(communityService.listPosts(keyword, userId, status, pageNum, pageSize));
    }

    /** 隐藏、恢复或删除一条社区动态。 */
    @Operation(summary = "执行动态治理动作")
    @PostMapping("/posts/{postId}/actions")
    public Result<ActionResultVO> actionPost(
            @PathVariable Long postId,
            @Valid @RequestBody CommunityContentActionDTO dto
    ) {
        return Result.success(communityService.actionPost(postId, dto));
    }

    /** 分页查询需要治理的评论及完整正文。 */
    @Operation(summary = "分页查询社区评论")
    @GetMapping("/comments")
    public Result<PageResult<CommunityCommentVO>> listComments(
            @RequestParam(required = false) Long postId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) HitPostStatus status,
            @RequestParam(required = false) Integer pageNum,
            @RequestParam(required = false) Integer pageSize
    ) {
        return Result.success(communityService.listComments(postId, userId, status, pageNum, pageSize));
    }

    /** 隐藏、恢复或删除一条社区评论。 */
    @Operation(summary = "执行评论治理动作")
    @PostMapping("/comments/{commentId}/actions")
    public Result<ActionResultVO> actionComment(
            @PathVariable Long commentId,
            @Valid @RequestBody CommunityContentActionDTO dto
    ) {
        return Result.success(communityService.actionComment(commentId, dto));
    }
}
