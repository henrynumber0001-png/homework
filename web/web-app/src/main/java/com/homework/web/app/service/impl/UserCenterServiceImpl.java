package com.homework.web.app.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.homework.common.exception.HomeworkException;
import com.homework.common.result.PageResult;
import com.homework.common.result.ResultCodeEnum;
import com.homework.model.entity.*;
import com.homework.model.enums.*;
import com.homework.web.app.mapper.*;
import com.homework.web.app.service.UserCenterService;
import com.homework.web.app.vo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserCenterServiceImpl implements UserCenterService {
    private static final long USER_CENTER_BANNER_ITEM_ID = 0L;

    private final UserInfoMapper userInfoMapper;
    private final GraphInfoMapper graphInfoMapper;
    private final PremiumUserInfoMapper premiumUserInfoMapper;
    private final UserFollowMapper userFollowMapper;
    private final HitPostMapper hitPostMapper;
    private final UserQuestionAnswerMapper userQuestionAnswerMapper;
    private final UserFavoriteQuestionMapper userFavoriteQuestionMapper;
    private final UserQuestionNoteMapper userQuestionNoteMapper;
    private final UserLearningStatDailyMapper userLearningStatDailyMapper;
    private final BankTagMapper bankTagMapper;
    private final InterviewQuestionInfoMapper interviewQuestionInfoMapper;
    private final CertificateQuestionInfoMapper certificateQuestionInfoMapper;

    @Override
    public UserCenterPageVO getCenterPageInfo(Long userId) {
        if (userId == null) {
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }

        UserCenterPageVO userCenterPageVO = new UserCenterPageVO();

        LambdaQueryWrapper<UserInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserInfo::getId, userId)
                .eq(UserInfo::getStatus, UserInfoStatus.ACTIVE)
                .eq(UserInfo::getUserRole, UserInfoUserRole.USER);
        UserInfo userInfo = userInfoMapper.selectOne(queryWrapper);
        //用户展示名称 和 AccountNo 不能为空或null
        if (userInfo == null || !StringUtils.hasText(userInfo.getDisplayName()) || !StringUtils.hasText(userInfo.getAccountNo())) {
            throw new HomeworkException(ResultCodeEnum.DATA_ERROR);
        }
        //装配UserInfoVO
        UserInfoVO userInfoVO = new UserInfoVO();
        userInfoVO.setAccountNo(userInfo.getAccountNo());
        userInfoVO.setAvatar(userInfo.getAvatar() == null ? null : userInfo.getAvatar());
        userInfoVO.setDisplayName(userInfo.getDisplayName());

        userCenterPageVO.setUserInfoVO(userInfoVO);

        //装配GraphInfoVO graphInfoVO
        LambdaQueryWrapper<GraphInfo> graphInfoQueryWrapper = new LambdaQueryWrapper<>();
        graphInfoQueryWrapper.eq(GraphInfo::getItemType, ItemType.USER_CENTER_BANNER)
                .eq(GraphInfo::getItemId, USER_CENTER_BANNER_ITEM_ID);
        GraphInfo graphInfo = graphInfoMapper.selectOne(graphInfoQueryWrapper);

        if (graphInfo != null) {
            GraphInfoVO graphInfoVO = new GraphInfoVO();
            graphInfoVO.setUrl(graphInfo.getUrl());
            graphInfoVO.setName(graphInfo.getName());
            userCenterPageVO.setGraphInfoVO(graphInfoVO);
        }


        //装配List<PremiumUserInfoVO> premiumUserInfoVOList;

        LambdaQueryWrapper<PremiumUserInfo> premiumUserInfoQueryWrapper = new LambdaQueryWrapper<>();
        premiumUserInfoQueryWrapper.eq(PremiumUserInfo::getUserId, userId)
                .eq(PremiumUserInfo::getStatus, PremiumStatus.ACTIVE);

        List<PremiumUserInfo> premiumUserInfos = premiumUserInfoMapper.selectList(premiumUserInfoQueryWrapper);
        //Set集合，遍历速度快 0(1)
        Set<PremiumOrderScope> premiumScopesSet = premiumUserInfos.stream().map(PremiumUserInfo::getPremiumScope).collect(Collectors.toSet());
        if(premiumUserInfos.isEmpty()) {
            userCenterPageVO.setPremium(false);
            userCenterPageVO.setSuperPremium(false);
        } else {
            userCenterPageVO.setPremium(true);
            userCenterPageVO.setSuperPremium(premiumScopesSet.contains(PremiumOrderScope.FULLACCESS)); //返回true/false
        }

        //组装followerCount
        LambdaQueryWrapper<UserFollow> userFollowQueryWrapper = new LambdaQueryWrapper<>();
        userFollowQueryWrapper.eq(UserFollow::getFolloweeUserId, userId);
        Long followerCount = userFollowMapper.selectCount(userFollowQueryWrapper);

        //组装followingCount
        LambdaQueryWrapper<UserFollow> userFollowingQueryWrapper = new LambdaQueryWrapper<>();
        userFollowingQueryWrapper.eq(UserFollow::getFollowerUserId, userId);
        Long followingCount = userFollowMapper.selectCount(userFollowingQueryWrapper);

        //组装postCount
        LambdaQueryWrapper<HitPost> postQueryWrapper = new LambdaQueryWrapper<>();
        postQueryWrapper.eq(HitPost::getPostUserId,userId);
        Long postCount = hitPostMapper.selectCount(postQueryWrapper);

        //组装answeredQuestionCount
        LambdaQueryWrapper<UserQuestionAnswer> userQuestionAnswerQueryWrapper = new LambdaQueryWrapper<>();
        userQuestionAnswerQueryWrapper.eq(UserQuestionAnswer::getUserId,userId);
        Long answeredQuestionCount = userQuestionAnswerMapper.selectCount(userQuestionAnswerQueryWrapper);

        //learnedBankCount
        LambdaQueryWrapper<UserQuestionAnswer> bankQueryWrapper = new LambdaQueryWrapper<>();
        bankQueryWrapper.eq(UserQuestionAnswer::getUserId,userId);
        long learnedBankCount = userQuestionAnswerMapper.selectList(bankQueryWrapper).stream().map(UserQuestionAnswer::getBankId).distinct().count();

        //studySeconds
        LambdaQueryWrapper<UserLearningStatDaily> dailyQueryWrapper = new LambdaQueryWrapper<>();
        dailyQueryWrapper.eq(UserLearningStatDaily::getUserId,userId)
                .select(UserLearningStatDaily::getStudySeconds);
        List<UserLearningStatDaily> userLearningStatDailies = userLearningStatDailyMapper.selectList(dailyQueryWrapper);
        long studySeconds = userLearningStatDailies.stream().mapToLong(UserLearningStatDaily::getStudySeconds).sum();
        long studyHours = Math.round((studySeconds / 3600.0));

        //wrongQuestionCount
        LambdaQueryWrapper<UserQuestionAnswer> wrongQuestionQueryWrapper = new LambdaQueryWrapper<>();
        wrongQuestionQueryWrapper.eq(UserQuestionAnswer::getUserId,userId);
        wrongQuestionQueryWrapper.eq(UserQuestionAnswer::getIsCorrect,false);
        Long wrongQuestionCount = userQuestionAnswerMapper.selectCount(wrongQuestionQueryWrapper);

        //favoriteQuestionCount
        LambdaQueryWrapper<UserFavoriteQuestion> favoriteQuestionQueryWrapper = new LambdaQueryWrapper<>();
        favoriteQuestionQueryWrapper.eq(UserFavoriteQuestion::getUserId,userId);
        Long favoriteQuestionCount = userFavoriteQuestionMapper.selectCount(favoriteQuestionQueryWrapper);

        //noteCount
        LambdaQueryWrapper<UserQuestionNote> noteQueryWrapper = new LambdaQueryWrapper<>();
        noteQueryWrapper.eq(UserQuestionNote::getUserId,userId);
        Long noteCount = userQuestionNoteMapper.selectCount(noteQueryWrapper);

        UserCenterCountsVO userCenterCountsVO = new UserCenterCountsVO();
        userCenterCountsVO.setLearnedBankCount(learnedBankCount);
        userCenterCountsVO.setStudyHours(studyHours);
        userCenterCountsVO.setWrongQuestionCount(wrongQuestionCount);
        userCenterCountsVO.setFavoriteQuestionCount(favoriteQuestionCount);
        userCenterCountsVO.setNoteCount(noteCount);
        userCenterCountsVO.setFollowerCount(followerCount);
        userCenterCountsVO.setFollowingCount(followingCount);
        userCenterCountsVO.setPostCount(postCount);
        userCenterCountsVO.setAnsweredQuestionCount(answeredQuestionCount);

        userCenterPageVO.setCountsVO(userCenterCountsVO);
        return userCenterPageVO;

        
    }

    @Override
    public PageResult<WrongQuestionBankVO> getWrongQuestionBanks(Long userId, GroupType groupType, Integer pageNum, Integer pageSize) {
        long current = pageNum == null || pageNum < 1 ? 1L : pageNum;
        long size = pageSize == null ? 20L : Math.min(Math.max(pageSize, 1), 50);

        if(userId == null){
            throw new HomeworkException(ResultCodeEnum.APP_LOGIN_NOT_AUTH);
        }
        if(groupType == null){
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }

        Page<WrongQuestionBankVO> page = new Page<>(current, size);

        IPage<WrongQuestionBankVO> wrongQuestionBanks = userQuestionAnswerMapper.getWrongQuestionBanks(page, groupType, userId);

        PageResult<WrongQuestionBankVO> result = new PageResult<>();
        result.setTotal(wrongQuestionBanks.getTotal());
        result.setRecords(wrongQuestionBanks.getRecords());
        result.setPageNum(wrongQuestionBanks.getCurrent());
        result.setPageSize(wrongQuestionBanks.getSize());


        List<WrongQuestionBankVO> records = result.getRecords();
        if(records.isEmpty()){
            return result;
        }
        List<Long> bankIds = records.stream().map(WrongQuestionBankVO::getBankId).toList();


        LambdaQueryWrapper<BankTag> tagQueryWrapper = new LambdaQueryWrapper<>();
        tagQueryWrapper.in(BankTag::getBankId,bankIds);
        List<BankTag> bankTags = bankTagMapper.selectList(tagQueryWrapper);
        Map<Long, List<String>> tagNamesMap = bankTags.stream().collect(Collectors.groupingBy(BankTag::getBankId, Collectors.mapping(BankTag::getTagName, Collectors.toList())));

        records.forEach(wrongQuestionBankVO ->
                wrongQuestionBankVO.setTagNames(tagNamesMap.getOrDefault(wrongQuestionBankVO.getBankId(), Collections.emptyList())));
        return result;
    }

    @Override
    public PageResult<WrongQuestionVO> getWrongQuestions(Long userId,Long bankId,Integer pageNum, Integer pageSize) {
        //校验pageNum, pageSize，以防前端传的数字过大
        long current = pageNum == null || pageNum < 1 ? 1L : pageNum;
        long size = pageSize == null ? 20L : Math.min(Math.max(pageSize, 1), 50);

        if(userId == null){
            throw new HomeworkException(ResultCodeEnum.APP_LOGIN_NOT_AUTH);
        }

        if(bankId == null){
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }

        GroupType groupType = userQuestionAnswerMapper.getGroupType(bankId);
        if(groupType == null){
            throw new HomeworkException(ResultCodeEnum.DATA_ERROR);
        }

        Page<WrongQuestionVO> page = new Page<>(current, size);
        IPage<WrongQuestionVO> wrongQuestions = userQuestionAnswerMapper.getWrongQuestions(page, userId, bankId);


        PageResult<WrongQuestionVO> result = new PageResult<>();
        result.setTotal(wrongQuestions.getTotal());
        result.setRecords(wrongQuestions.getRecords());
        result.setPageNum(wrongQuestions.getCurrent());
        result.setPageSize(wrongQuestions.getSize());


        List<WrongQuestionVO> records = result.getRecords();
        if(records.isEmpty()){
            return result;
        }

        List<Long> questionIds = records.stream().map(WrongQuestionVO::getQuestionId).toList();

        if(groupType.equals(GroupType.INTERVIEW)){
            LambdaQueryWrapper<InterviewQuestionInfo> interviewInfoQueryWrapper = new LambdaQueryWrapper<>();
            interviewInfoQueryWrapper.in(InterviewQuestionInfo::getId,questionIds)
                    .eq(InterviewQuestionInfo::getIsReleased,true)
                    .eq(InterviewQuestionInfo::getQuestionType,QuestionInfoQuestionType.ESSAY);
            List<InterviewQuestionInfo> interviewQuestionInfos = interviewQuestionInfoMapper.selectList(interviewInfoQueryWrapper);
            Map<Long, String> questionIdToTitleMap = interviewQuestionInfos.stream().collect(Collectors.toMap(InterviewQuestionInfo::getId, InterviewQuestionInfo::getTitle));

            records.forEach(wrongQuestionVO ->{
                String title = questionIdToTitleMap.get(wrongQuestionVO.getQuestionId());
                if(!StringUtils.hasText(title)){
                    wrongQuestionVO.setIsAvailable(false); //给前端一个标识，当isAvailable = false，表示这道题已下架或删除
                    wrongQuestionVO.setTitle(null);
                }else {
                    wrongQuestionVO.setIsAvailable(true);
                    wrongQuestionVO.setTitle(title);
                }
            });
        }
        if(groupType.equals(GroupType.CERTIFICATION)){
            LambdaQueryWrapper<CertificateQuestionInfo> questionInfoQueryWrapper = new LambdaQueryWrapper<>();
            questionInfoQueryWrapper.in(CertificateQuestionInfo::getId,questionIds)
                    .eq(CertificateQuestionInfo::getIsReleased,true)
                    .in(CertificateQuestionInfo::getQuestionType,QuestionInfoQuestionType.SINGLE_CHOICE,QuestionInfoQuestionType.MULTIPLE);
            List<CertificateQuestionInfo> questionInfos = certificateQuestionInfoMapper.selectList(questionInfoQueryWrapper);
            Map<Long, String> questionIdToTitleMap = questionInfos.stream().collect(Collectors.toMap(CertificateQuestionInfo::getId, CertificateQuestionInfo::getTitle));

            records.forEach(wrongQuestionVO ->{
                String title = questionIdToTitleMap.get(wrongQuestionVO.getQuestionId());
                if(!StringUtils.hasText(title)){
                    wrongQuestionVO.setIsAvailable(false); //给前端一个标识，当isAvailable = false，表示这道题已下架或删除
                    wrongQuestionVO.setTitle(null);
                }else {
                    wrongQuestionVO.setIsAvailable(true);
                    wrongQuestionVO.setTitle(title);
                }

            });
        }
        return result;

    }

}
