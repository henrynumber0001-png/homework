package com.homework.web.app.controller;

import com.homework.common.result.Result;
import com.homework.web.app.context.LoginUserHolder;
import com.homework.web.app.dto.FollowActionDTO;
import com.homework.web.app.service.FollowService;
import com.homework.web.app.vo.FollowStateVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/** 用户关注接口；产生的新增关注通知归入“我的消息 / 系统消息”。 */
@RestController
@RequestMapping("/api/app/users")
@RequiredArgsConstructor
public class FollowController {

    private final FollowService followService;

    @PutMapping("/{targetUserId}/follow")
    public Result<FollowStateVO> follow(@PathVariable Long targetUserId, @RequestBody FollowActionDTO dto) {
        return Result.success(followService.follow(LoginUserHolder.getUserId(), targetUserId, dto.getActive()));
    }
}
