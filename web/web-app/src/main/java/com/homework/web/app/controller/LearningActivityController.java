package com.homework.web.app.controller;

import com.homework.common.result.Result;
import com.homework.web.app.context.LoginUserHolder;
import com.homework.web.app.service.LearningActivityService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/app/learning-activity")
@RequiredArgsConstructor
public class LearningActivityController {

    private final LearningActivityService learningActivityService;

    @PostMapping("/heartbeat")
    public Result<Void> heartbeat() {
        learningActivityService.heartbeat(LoginUserHolder.getUserId());

        return Result.success();
    }
}

/*
如果前端连续 10 分钟没有监听到任何被定义为“用户活动”的事件，则停止发送心跳。
即，前端持续记录最后一次用户操作时间，每次定时发送心跳前，判断距离最后一次操作是否已经达到 10 分钟。
 */