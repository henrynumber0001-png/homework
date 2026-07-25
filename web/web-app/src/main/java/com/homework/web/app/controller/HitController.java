package com.homework.web.app.controller;

import com.homework.common.result.Result;
import com.homework.web.app.context.LoginUserHolder;
import com.homework.web.app.dto.HitActionDTO;
import com.homework.web.app.dto.HitCommentCreateDTO;
import com.homework.web.app.dto.HitCommentLikeDTO;
import com.homework.web.app.dto.HitPostCreateDTO;
import com.homework.web.app.service.HitService;
import com.homework.web.app.vo.HitActionResultVO;
import com.homework.web.app.vo.HitCommentLikeResultVO;
import com.homework.web.app.vo.HitCommentVO;
import com.homework.web.app.vo.HitPostVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/*
前端：识别用户点击了哪个导航标签，切换页面，并请求对应的后端 API。
后端：根据 HTTP 方法和 URL，把请求交给对应的 Controller。
一句话概括：前端决定“用户要去哪个页面、请求哪个 API”，后端根据 URL 决定“由哪个 Controller 处理请求”。
 */

/*
1. 前端页面路由跳转：router.push("/hits");
2. HIT 页面加载时请求后端：fetch("/api/app/hits?pageNum=1&pageSize=20");
 */
@RestController
@RequestMapping("/api/app/hits")
@RequiredArgsConstructor
public class HitController {

    private final HitService hitService;

    @GetMapping
    public Result<List<HitPostVO>> list(@RequestParam(required = false) Integer pageNum,
                                        @RequestParam(required = false) Integer pageSize) {
        List<HitPostVO> hitPostVOS = hitService.listHits(pageNum,pageSize);
        return Result.success(hitPostVOS);
    }

    @PostMapping
    public Result<Long> publish(@RequestBody HitPostCreateDTO dto) {
        return Result.success(hitService.publish(dto));
    }

    @GetMapping("/{postId}/comments")
    public Result<List<HitCommentVO>> comments(@PathVariable Long postId,@RequestParam(required = false) Integer pageNum,
                                               @RequestParam(required = false) Integer pageSize) {
        List<HitCommentVO> hitCommentVOS = hitService.listComments(postId,pageNum, pageSize);
        return Result.success(hitCommentVOS);
    }

    @PostMapping("/{postId}/comments")
    public Result<Long> comment(@PathVariable Long postId,@RequestBody HitCommentCreateDTO dto) {
        Long commentId = hitService.comment(postId, dto);
        return Result.success(commentId);
    }

    @PostMapping("/{postId}/actions")
    public Result<HitActionResultVO> action(@PathVariable Long postId, @RequestBody HitActionDTO dto) {
        return Result.success(hitService.action(postId, dto));
    }

    @PutMapping("/{postId}/comments/{commentId}/like")
    public Result<HitCommentLikeResultVO> commentLike(
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @RequestBody HitCommentLikeDTO dto) {
        return Result.success(hitService.commentLike(postId, commentId, dto));
    }
}
