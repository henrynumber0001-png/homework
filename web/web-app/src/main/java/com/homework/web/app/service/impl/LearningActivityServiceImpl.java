package com.homework.web.app.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.homework.common.exception.HomeworkException;
import com.homework.common.result.ResultCodeEnum;
import com.homework.model.entity.UserLearningStatDaily;
import com.homework.web.app.mapper.UserLearningStatDailyMapper;
import com.homework.web.app.service.LearningActivityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class LearningActivityServiceImpl implements LearningActivityService {
    private static final long MIN_HEARTBEAT_SECONDS = 45;

    private static final long MAX_CONTINUOUS_HEARTBEAT_SECONDS = 120;

    private static final long MAX_DAILY_STUDY_SECONDS = 24L * 60 * 60;

    private final UserLearningStatDailyMapper userLearningStatDailyMapper;


    /**
     * 前端每60秒都会发送一次心跳存储
     */
    @Transactional
    public void heartbeat(Long userId) {
        if(userId == null){
            throw new HomeworkException(ResultCodeEnum.APP_LOGIN_NOT_AUTH);
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();

        //查询当前用户在今天有没有打卡记录
        LambdaQueryWrapper<UserLearningStatDaily> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserLearningStatDaily::getUserId, userId)
                .eq(UserLearningStatDaily::getStatDate, today)
                .last("FOR UPDATE");

        UserLearningStatDaily daily = userLearningStatDailyMapper.selectOne(queryWrapper);

        //如果还没有，创建打卡记录
        if (daily == null) {
            daily = new UserLearningStatDaily();
            daily.setUserId(userId);
            daily.setStatDate(today);
            daily.setStudySeconds(0); //因为刚创建，今日学习时长设置为 0
            daily.setLastHeartbeatTime(now); //最近一次打卡时间设置为 现在

            userLearningStatDailyMapper.insert(daily);
            return;
        }

        //如果今天已经有打卡记录了
        //获取最近一次的心跳时间
        LocalDateTime lastHeartbeat = daily.getLastHeartbeatTime();

        if (lastHeartbeat == null) {
            daily.setLastHeartbeatTime(now);
            userLearningStatDailyMapper.updateById(daily);
            return;
        }

        //如果最近一次心跳时间不会null，获取 最近一次 到 now 之间的时间，基本就是 around 60L
        long gapSeconds = Duration.between(lastHeartbeat, now).getSeconds();

        // 请求过于频繁，不累计，也不更新时间。
        if (gapSeconds <= MIN_HEARTBEAT_SECONDS) {
            return;
        }

        //如果gapSeconds > MAX_IDLE_SECONDS，说明用户长时间没有操作，已经退出上一轮的计时了
        //因此只设置 最近心跳时间为 now，重新开启新一轮统计, studySeconds 不更新
        if(gapSeconds > MAX_CONTINUOUS_HEARTBEAT_SECONDS){
            daily.setLastHeartbeatTime(now);
            userLearningStatDailyMapper.updateById(daily);
            return;
        }

        //如果MIN_HEARTBEAT_SECONDS < gapSeconds <= MAX_IDLE_SECONDS，说明是正常的前端传送心跳的时间间隔区间，因此正常叠加Seconds
        //这里 Max Seconds 多给60秒的 buffer 冗余，用于应对前后端延迟、主线程繁忙、用户设备卡顿等问题造成的时间延长
        long currentSeconds = daily.getStudySeconds() == null ? 0 : daily.getStudySeconds();
        long updatedSeconds = currentSeconds + gapSeconds;
        daily.setStudySeconds(Math.min(updatedSeconds, MAX_DAILY_STUDY_SECONDS));
        daily.setLastHeartbeatTime(now);
        userLearningStatDailyMapper.updateById(daily);
    }
}
