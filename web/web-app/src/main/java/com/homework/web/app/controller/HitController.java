package com.homework.web.app.controller;

import com.homework.common.result.Result;
import com.homework.web.app.context.LoginUserHolder;
import com.homework.web.app.dto.HitActionDTO;
import com.homework.web.app.dto.HitCommentCreateDTO;
import com.homework.web.app.dto.HitPostCreateDTO;
import com.homework.web.app.service.HitService;
import com.homework.web.app.vo.HitCommentVO;
import com.homework.web.app.vo.HitPostVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** Hit 学习打卡的公共时间线、评论和互动接口。 */
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
        return Result.success(hitService.publish(LoginUserHolder.getUserId(), dto));
    }

    @GetMapping("/{postId}/comments")
    public Result<List<HitCommentVO>> comments(@PathVariable Long postId,
                                               @RequestParam(required = false) Integer pageNum,
                                               @RequestParam(required = false) Integer pageSize) {
        return Result.success(hitService.listComments(postId, pageNum, pageSize));
    }

    @PostMapping("/{postId}/comments")
    public Result<Long> comment(@PathVariable Long postId, @RequestBody HitCommentCreateDTO dto) {
        return Result.success(hitService.comment(LoginUserHolder.getUserId(), postId, dto));
    }

    @PostMapping("/{postId}/actions")
    public Result<Map<String, Object>> action(@PathVariable Long postId, @RequestBody HitActionDTO dto) {
        return Result.success(hitService.action(LoginUserHolder.getUserId(), postId, dto));
    }
}
