package com.homework.web.app.controller;

import com.homework.common.result.Result;
import com.homework.web.app.dto.HitActionDTO;
import com.homework.web.app.dto.HitCommentCreateDTO;
import com.homework.web.app.dto.HitPostCreateDTO;
import com.homework.web.app.service.HitService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/app/hits")
@RequiredArgsConstructor
public class HitController {

    private final HitService hitService;

    @GetMapping
    public Result<List<Map<String, Object>>> list(@RequestParam(required = false) Integer pageNum,
                                                  @RequestParam(required = false) Integer pageSize) {
        return Result.success(hitService.listHits(pageNum, pageSize));
    }

    @PostMapping
    public Result<Long> publish(@RequestBody HitPostCreateDTO dto) {
        return Result.success(hitService.publish(dto));
    }

    @PostMapping("/{postId}/comments")
    public Result<Long> comment(@PathVariable Long postId, @RequestBody HitCommentCreateDTO dto) {
        return Result.success(hitService.comment(postId, dto));
    }

    @PostMapping("/{postId}/actions")
    public Result<Map<String, Object>> action(@PathVariable Long postId, @RequestBody HitActionDTO dto) {
        return Result.success(hitService.action(postId, dto));
    }
}
