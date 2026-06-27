package com.homework.web.app.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.homework.model.entity.*;
import com.homework.web.app.mapper.*;
import com.homework.web.app.service.ProfileService;
import com.homework.web.app.vo.ProfileOverviewVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {

    private final UserInfoMapper userInfoMapper;
    private final UserFollowMapper userFollowMapper;
    private final HitPostMapper hitPostMapper;
    private final UserQuestionAnswerMapper userQuestionAnswerMapper;
    private final PracticeSessionMapper practiceSessionMapper;
    private final UserLearningStatDailyMapper userLearningStatDailyMapper;
    private final UserMedalMapper userMedalMapper;
    private final UserWrongQuestionMapper userWrongQuestionMapper;
    private final UserFavoriteBankMapper userFavoriteBankMapper;
    private final UserFavoriteQuestionMapper userFavoriteQuestionMapper;
    private final UserQuestionNoteMapper userQuestionNoteMapper;

    @Override
    public ProfileOverviewVO getOverview(Long userId) {
        UserInfo user = userInfoMapper.selectById(userId);
        ProfileOverviewVO vo = new ProfileOverviewVO();
        vo.setUserId(userId);
        if (user != null) {
            vo.setDisplayName(user.getDisplayName());
            vo.setAvatar(user.getAvatar());
        }
        vo.setFollowingCount(userFollowMapper.selectCount(new QueryWrapper<UserFollow>().eq("follower_user_id", userId).eq("is_deleted", 0)));
        vo.setFollowerCount(userFollowMapper.selectCount(new QueryWrapper<UserFollow>().eq("following_user_id", userId).eq("is_deleted", 0)));
        vo.setPostCount(hitPostMapper.selectCount(new QueryWrapper<HitPost>().eq("user_id", userId).eq("is_deleted", 0)));
        vo.setAnsweredCount(userQuestionAnswerMapper.selectCount(new QueryWrapper<UserQuestionAnswer>().eq("user_id", userId).eq("is_deleted", 0)));
        vo.setLearnedBankCount(practiceSessionMapper.selectCount(new QueryWrapper<PracticeSession>().eq("user_id", userId).eq("is_deleted", 0)));
        Map<String, Object> study = userLearningStatDailyMapper.selectMaps(new QueryWrapper<UserLearningStatDaily>()
                .select("IFNULL(SUM(study_seconds),0) AS study_seconds")
                .eq("user_id", userId)
                .eq("is_deleted", 0)).stream().findFirst().orElse(Map.of());
        Object seconds = study.get("study_seconds");
        vo.setStudySeconds(seconds == null ? 0L : ((Number) seconds).longValue());
        vo.setMedalCount(userMedalMapper.selectCount(new QueryWrapper<UserMedal>().eq("user_id", userId).eq("is_deleted", 0)));
        vo.setWeeklyStudyStats(userLearningStatDailyMapper.selectMaps(new QueryWrapper<UserLearningStatDaily>()
                .select("stat_date", "study_seconds", "answered_count", "correct_count")
                .eq("user_id", userId)
                .eq("is_deleted", 0)
                .orderByDesc("stat_date")
                .last("LIMIT 7")));
        return vo;
    }

    @Override
    public List<Map<String, Object>> listWrongQuestions(Long userId) {
        return userWrongQuestionMapper.selectMaps(new QueryWrapper<UserWrongQuestion>()
                .eq("user_id", userId)
                .eq("is_deleted", 0)
                .orderByDesc("updated_time"));
    }

    @Override
    public List<Map<String, Object>> listFavoriteBanks(Long userId) {
        return userFavoriteBankMapper.selectMaps(new QueryWrapper<UserFavoriteBank>()
                .eq("user_id", userId)
                .eq("is_deleted", 0)
                .orderByDesc("save_time"));
    }

    @Override
    public List<Map<String, Object>> listFavoriteQuestions(Long userId) {
        return userFavoriteQuestionMapper.selectMaps(new QueryWrapper<UserFavoriteQuestion>()
                .eq("user_id", userId)
                .eq("is_deleted", 0)
                .orderByDesc("save_time"));
    }

    @Override
    public List<Map<String, Object>> listNotes(Long userId) {
        return userQuestionNoteMapper.selectMaps(new QueryWrapper<UserQuestionNote>()
                .eq("user_id", userId)
                .eq("is_deleted", 0)
                .orderByDesc("updated_time"));
    }
}
