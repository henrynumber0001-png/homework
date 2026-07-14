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
    public Result<Long> publish(@RequestBody String content) {
        Long postId = hitService.publish(content);//返回postId，是为了让前端知道刚刚创建的 Hit 在数据库中的唯一标识。
        return Result.success();
    }

    @GetMapping("/{postId}/comments")
    public Result<List<HitCommentVO>> comments(@RequestParam Long postId,@RequestParam(required = false) Integer pageNum,
                                               @RequestParam(required = false) Integer pageSize) {
        List<HitCommentVO> hitCommentVOS = hitService.listComments(postId,pageNum, pageSize);
        return Result.success(hitCommentVOS);
    }

    @PostMapping("/{postId}/comments")
    public Result<Long> comment(@RequestParam Long postId,@RequestBody HitCommentCreateDTO dto) {
        Long commentId = hitService.comment(postId, dto);
        return Result.success(commentId);
    }

    @PostMapping("/{postId}/actions")
    public Result<Map<String, Object>> action(@PathVariable Long postId, @RequestBody HitActionDTO dto) {
         Map<String, Object> actionResult = hitService.action(postId, dto);
        return Result.success(actionResult);
    }
}
