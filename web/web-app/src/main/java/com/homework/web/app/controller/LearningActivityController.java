package com.homework.web.app.controller;

import com.homework.common.result.Result;
import com.homework.web.app.context.LoginUserHolder;
import com.homework.web.app.service.LearningActivityService;
import com.homework.web.app.vo.LearningCalendarItemVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// UserLearningStatDaily 是“数据怎么存”
// LearningActivity 是“业务在做什么”
// Controller 和 Service 面向业务命名
// Entity 和 Mapper 面向存储命名
// 前端关心的是“学习活动”，不关心后端保存在哪张表。

@RestController
@RequestMapping("/api/app/learning-activity")
@RequiredArgsConstructor
public class LearningActivityController {

    private final LearningActivityService learningActivityService;

    //写入 学习日期、学习时长，更新 最后心跳时间
    @PostMapping("/heartbeat")
    public Result<Void> heartbeat() {
        learningActivityService.heartbeat(LoginUserHolder.getUserId());

        return Result.success();
    }

    //查询指定年份的全年每日热力日历图
    @GetMapping("/calendar")
    public Result<List<LearningCalendarItemVO>> calendar(@RequestParam(required = false) Integer year) {
        Long userId = LoginUserHolder.getUserId();
        List<LearningCalendarItemVO> calendar = learningActivityService.getCalendar(userId, year);
        return Result.success(calendar);
    }



}

/*
如果前端连续 10 分钟没有监听到任何被定义为“用户活动”的事件，则停止发送心跳。
即，前端持续记录最后一次用户操作时间，每次定时发送心跳前，判断距离最后一次操作是否已经达到 10 分钟。
 */

/*
没有数据    灰色
0分钟       最浅绿色，表示已签到
1～30分钟   浅绿色
31～60分钟  中浅绿色
61～120分钟 中绿色
超过120分钟 深绿色
 */