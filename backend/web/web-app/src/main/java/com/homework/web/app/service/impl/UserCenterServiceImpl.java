package com.homework.web.app.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.homework.common.exception.HomeworkException;
import com.homework.common.result.PageResult;
import com.homework.common.result.ResultCodeEnum;
import com.homework.common.storage.CosReadUrlSigner;
import com.homework.common.storage.UserImageUrlResolver;
import com.homework.model.entity.*;
import com.homework.model.enums.*;
import com.homework.web.app.dto.AiEvaluationResult;
import com.homework.web.app.dto.EditProfileDTO;
import com.homework.web.app.mapper.*;
import com.homework.web.app.service.MembershipAccessService;
import com.homework.web.app.service.MembershipAccessSnapshot;
import com.homework.web.app.service.UserCenterService;
import com.homework.web.app.vo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserCenterServiceImpl implements UserCenterService {
    private final UserInfoMapper userInfoMapper;
    private final MembershipAccessService membershipAccessService;
    private final UserFollowMapper userFollowMapper;
    private final HitPostMapper hitPostMapper;
    private final UserQuestionAnswerMapper userQuestionAnswerMapper;
    private final UserFavoriteQuestionMapper userFavoriteQuestionMapper;
    private final UserQuestionNoteMapper userQuestionNoteMapper;
    private final UserLearningStatDailyMapper userLearningStatDailyMapper;
    private final BankTagMapper bankTagMapper;
    private final InterviewQuestionInfoMapper interviewQuestionInfoMapper;
    private final CertificateQuestionInfoMapper certificateQuestionInfoMapper;
    private final QuestionAiEvaluationMapper questionAiEvaluationMapper;
    private final QuestionBankMapper questionBankMapper;
    private final CosReadUrlSigner readUrlSigner;
    private final UserImageUrlResolver userImageUrlResolver;
    private final SubTechDirectionMapper subTechDirectionMapper;
    private final TechDirectionMapper techDirectionMapper;

    @Override
    public UserCenterPageVO getCenterPageInfo(Long userId) {
        if (userId == null) {
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }

        UserCenterPageVO userCenterPageVO = new UserCenterPageVO();

        LambdaQueryWrapper<UserInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserInfo::getId, userId)
                .eq(UserInfo::getStatus, UserInfoStatus.ACTIVE);
        UserInfo userInfo = userInfoMapper.selectOne(queryWrapper);
        //用户展示名称 和 AccountNo 不能为空或null
        if (userInfo == null || !StringUtils.hasText(userInfo.getDisplayName()) || !StringUtils.hasText(userInfo.getAccountNo())) {
            throw new HomeworkException(ResultCodeEnum.DATA_ERROR);
        }
        //装配UserInfoVO
        UserInfoVO userInfoVO = new UserInfoVO();
        userInfoVO.setAccountNo(userInfo.getAccountNo());
        userInfoVO.setAvatarUrl(userImageUrlResolver.resolveAvatar(userInfo.getAvatarObjectKey()));
        userInfoVO.setBannerUrl(userImageUrlResolver.resolveBanner(userInfo.getBannerObjectKey()));
        userInfoVO.setDisplayName(userInfo.getDisplayName());
        userInfoVO.setGender(userInfo.getGender());
        userInfoVO.setIntroduction(userInfo.getIntroduction());
        userInfoVO.setCompanyOrSchool(userInfo.getCompanyOrSchool());
        userInfoVO.setSubTechDirectionId(userInfo.getSubTechDirectionId());


        userCenterPageVO.setUserInfoVO(userInfoVO);

        MembershipAccessSnapshot membership = membershipAccessService.getAccess(userId);
        userCenterPageVO.setMembershipActive(membership.status() != MembershipStatus.FREE);
        userCenterPageVO.setMembershipType(membership.membershipType());
        userCenterPageVO.setAiFeaturesEnabled(membership.status() == MembershipStatus.PREMIUM_PLUS);

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
        postQueryWrapper.eq(HitPost::getPostUserId, userId);
        Long postCount = hitPostMapper.selectCount(postQueryWrapper);

        //组装answeredQuestionCount
        LambdaQueryWrapper<UserQuestionAnswer> userQuestionAnswerQueryWrapper = new LambdaQueryWrapper<>();
        userQuestionAnswerQueryWrapper.eq(UserQuestionAnswer::getUserId, userId);
        Long answeredQuestionCount = userQuestionAnswerMapper.selectCount(userQuestionAnswerQueryWrapper);

        //learnedBankCount
        LambdaQueryWrapper<UserQuestionAnswer> bankQueryWrapper = new LambdaQueryWrapper<>();
        bankQueryWrapper.eq(UserQuestionAnswer::getUserId, userId);
        long learnedBankCount = userQuestionAnswerMapper.selectList(bankQueryWrapper).stream().map(UserQuestionAnswer::getBankId).distinct().count();

        //studySeconds
        LambdaQueryWrapper<UserLearningStatDaily> dailyQueryWrapper = new LambdaQueryWrapper<>();
        dailyQueryWrapper.eq(UserLearningStatDaily::getUserId, userId)
                .select(UserLearningStatDaily::getStudySeconds);
        List<UserLearningStatDaily> userLearningStatDailies = userLearningStatDailyMapper.selectList(dailyQueryWrapper);
        long studySeconds = userLearningStatDailies.stream().mapToLong(UserLearningStatDaily::getStudySeconds).sum();
        long studyHours = Math.round((studySeconds / 3600.0));

        //wrongQuestionCount
        LambdaQueryWrapper<UserQuestionAnswer> wrongQuestionQueryWrapper = new LambdaQueryWrapper<>();
        wrongQuestionQueryWrapper.eq(UserQuestionAnswer::getUserId, userId);
        wrongQuestionQueryWrapper.eq(UserQuestionAnswer::getIsCorrect, false);
        Long wrongQuestionCount = userQuestionAnswerMapper.selectCount(wrongQuestionQueryWrapper);

        //favoriteQuestionCount
        LambdaQueryWrapper<UserFavoriteQuestion> favoriteQuestionQueryWrapper = new LambdaQueryWrapper<>();
        favoriteQuestionQueryWrapper.eq(UserFavoriteQuestion::getUserId, userId);
        Long favoriteQuestionCount = userFavoriteQuestionMapper.selectCount(favoriteQuestionQueryWrapper);

        //noteCount
        LambdaQueryWrapper<UserQuestionNote> noteQueryWrapper = new LambdaQueryWrapper<>();
        noteQueryWrapper.eq(UserQuestionNote::getUserId, userId);
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

        if (userId == null) {
            throw new HomeworkException(ResultCodeEnum.APP_LOGIN_NOT_AUTH);
        }
        if (groupType == null) {
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
        if (records.isEmpty()) {
            return result;
        }
        List<Long> bankIds = records.stream().map(WrongQuestionBankVO::getBankId).toList();


        LambdaQueryWrapper<BankTag> tagQueryWrapper = new LambdaQueryWrapper<>();
        tagQueryWrapper.in(BankTag::getBankId, bankIds);
        List<BankTag> bankTags = bankTagMapper.selectList(tagQueryWrapper);
        Map<Long, List<String>> tagNamesMap = bankTags.stream().collect(Collectors.groupingBy(BankTag::getBankId, Collectors.mapping(BankTag::getTagName, Collectors.toList())));

        records.forEach(wrongQuestionBankVO ->
                wrongQuestionBankVO.setTagNames(tagNamesMap.getOrDefault(wrongQuestionBankVO.getBankId(), Collections.emptyList())));
        return result;
    }

    @Override
    public PageResult<WrongQuestionVO> getWrongQuestions(Long userId, Long bankId, Integer pageNum, Integer pageSize) {
        //校验pageNum, pageSize，以防前端传的数字过大
        long current = pageNum == null || pageNum < 1 ? 1L : pageNum;
        long size = pageSize == null ? 20L : Math.min(Math.max(pageSize, 1), 50);

        if (userId == null) {
            throw new HomeworkException(ResultCodeEnum.APP_LOGIN_NOT_AUTH);
        }

        if (bankId == null) {
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }

        GroupType groupType = questionBankMapper.getGroupType(bankId);
        if (groupType == null) {
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
        if (records.isEmpty()) {
            return result;
        }

        List<Long> questionIds = records.stream().map(WrongQuestionVO::getQuestionId).toList();

        if (groupType.equals(GroupType.INTERVIEW)) {
            LambdaQueryWrapper<InterviewQuestionInfo> interviewInfoQueryWrapper = new LambdaQueryWrapper<>();
            interviewInfoQueryWrapper.in(InterviewQuestionInfo::getId, questionIds)
                    .eq(InterviewQuestionInfo::getStatus, QuestionInfoStatus.PUBLISHED)
                    .eq(InterviewQuestionInfo::getQuestionType, QuestionInfoQuestionType.ESSAY);
            List<InterviewQuestionInfo> interviewQuestionInfos = interviewQuestionInfoMapper.selectList(interviewInfoQueryWrapper);
            Map<Long, String> questionIdToTitleMap = interviewQuestionInfos.stream().collect(Collectors.toMap(InterviewQuestionInfo::getId, InterviewQuestionInfo::getTitle));

            records.forEach(wrongQuestionVO -> {
                String title = questionIdToTitleMap.get(wrongQuestionVO.getQuestionId());
                if (!StringUtils.hasText(title)) {
                    wrongQuestionVO.setIsAvailable(false); //给前端一个标识，当isAvailable = false，表示这道题已下架或删除
                    wrongQuestionVO.setTitle(null);
                } else {
                    wrongQuestionVO.setIsAvailable(true);
                    wrongQuestionVO.setTitle(title);
                }
            });
        }
        if (groupType.equals(GroupType.CERTIFICATION)) {
            LambdaQueryWrapper<CertificateQuestionInfo> questionInfoQueryWrapper = new LambdaQueryWrapper<>();
            questionInfoQueryWrapper.in(CertificateQuestionInfo::getId, questionIds)
                    .eq(CertificateQuestionInfo::getStatus, QuestionInfoStatus.PUBLISHED)
                    .in(CertificateQuestionInfo::getQuestionType, QuestionInfoQuestionType.SINGLE_CHOICE, QuestionInfoQuestionType.MULTIPLE);
            List<CertificateQuestionInfo> questionInfos = certificateQuestionInfoMapper.selectList(questionInfoQueryWrapper);
            Map<Long, String> questionIdToTitleMap = questionInfos.stream().collect(Collectors.toMap(CertificateQuestionInfo::getId, CertificateQuestionInfo::getTitle));

            records.forEach(wrongQuestionVO -> {
                String title = questionIdToTitleMap.get(wrongQuestionVO.getQuestionId());
                if (!StringUtils.hasText(title)) {
                    wrongQuestionVO.setIsAvailable(false); //给前端一个标识，当isAvailable = false，表示这道题已下架或删除
                    wrongQuestionVO.setTitle(null);
                } else {
                    wrongQuestionVO.setIsAvailable(true);
                    wrongQuestionVO.setTitle(title);
                }

            });
        }
        return result;

    }

    @Override
    public WrongQuestionReviewVO getWrongQuestion(Long userId, Long bankId, Long questionId) {
        if (userId == null) {
            throw new HomeworkException(ResultCodeEnum.APP_LOGIN_NOT_AUTH);
        }
        if (questionId == null || bankId == null) {
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }

        LambdaQueryWrapper<UserQuestionAnswer> userQuestionAnswerQueryWrapper = new LambdaQueryWrapper<>();
        userQuestionAnswerQueryWrapper.eq(UserQuestionAnswer::getUserId, userId)
                .eq(UserQuestionAnswer::getBankId, bankId)
                .eq(UserQuestionAnswer::getQuestionId, questionId)
                .eq(UserQuestionAnswer::getIsCorrect, false);

        UserQuestionAnswer userQuestionAnswer = userQuestionAnswerMapper.selectOne(userQuestionAnswerQueryWrapper);
        if (userQuestionAnswer == null) {
            throw new HomeworkException(ResultCodeEnum.DATA_ERROR);
        }

        GroupType groupType = questionBankMapper.getGroupType(bankId);
        if (groupType == null) {
            throw new HomeworkException(ResultCodeEnum.DATA_ERROR);
        }

        WrongQuestionReviewVO vo = new WrongQuestionReviewVO();
        if (groupType.equals(GroupType.INTERVIEW)) {
            LambdaQueryWrapper<InterviewQuestionInfo> interviewInfoQueryWrapper = new LambdaQueryWrapper<>();
            interviewInfoQueryWrapper.eq(InterviewQuestionInfo::getId, questionId)
                    .eq(InterviewQuestionInfo::getStatus, QuestionInfoStatus.PUBLISHED)
                    .eq(InterviewQuestionInfo::getQuestionType, QuestionInfoQuestionType.ESSAY);
            InterviewQuestionInfo interviewInfo = interviewQuestionInfoMapper.selectOne(interviewInfoQueryWrapper);
            if (interviewInfo == null) {
                throw new HomeworkException(ResultCodeEnum.DATA_ERROR);
            }

            LambdaQueryWrapper<QuestionAiEvaluation> aiQueryWrapper = new LambdaQueryWrapper<>();
            aiQueryWrapper.eq(QuestionAiEvaluation::getAnswerId, userQuestionAnswer.getId())
                    .eq(QuestionAiEvaluation::getUserId, userId);

            QuestionAiEvaluation aiEvaluation = questionAiEvaluationMapper.selectOne(aiQueryWrapper);
            if (aiEvaluation != null) {
                AiEvaluationResult aiResult = new AiEvaluationResult();
                BeanUtils.copyProperties(aiEvaluation, aiResult);
                vo.setAiResult(aiResult);
            }

            vo.setTitle(interviewInfo.getTitle());
            vo.setAnalysis(interviewInfo.getAnalysis());
            vo.setQuestionType(interviewInfo.getQuestionType());
            vo.setImageUrl(readUrlSigner.sign(interviewInfo.getImageObjectKey()));

        }

        if (groupType.equals(GroupType.CERTIFICATION)) {
            LambdaQueryWrapper<CertificateQuestionInfo> questionInfoQueryWrapper = new LambdaQueryWrapper<>();
            questionInfoQueryWrapper.in(CertificateQuestionInfo::getId, questionId)
                    .eq(CertificateQuestionInfo::getStatus, QuestionInfoStatus.PUBLISHED)
                    .in(CertificateQuestionInfo::getQuestionType, QuestionInfoQuestionType.SINGLE_CHOICE, QuestionInfoQuestionType.MULTIPLE);
            CertificateQuestionInfo certificateInfo = certificateQuestionInfoMapper.selectOne(questionInfoQueryWrapper);
            if (certificateInfo == null) {
                throw new HomeworkException(ResultCodeEnum.DATA_ERROR);
            }
            vo.setOptions(certificateInfo.getOptions());
            vo.setQuestionType(certificateInfo.getQuestionType());
            vo.setCorrectAnswer(certificateInfo.getCorrectAnswer());
            vo.setImageUrl(readUrlSigner.sign(certificateInfo.getImageObjectKey()));
            vo.setTitle(certificateInfo.getTitle());
            vo.setAnalysis(certificateInfo.getAnalysis());
        }

        vo.setQuestionId(userQuestionAnswer.getQuestionId());
        vo.setChosenOptions(userQuestionAnswer.getChosenOptions());
        vo.setContent(userQuestionAnswer.getContent());
        vo.setAnsweredTime(userQuestionAnswer.getAnsweredTime());
        vo.setContent(userQuestionAnswer.getContent());

        return vo;
    }

    @Override
    public PageResult<FavoriteQuestionBankVO> getFavoriteQuestionBanks(Long userId, GroupType groupType, Integer pageNum, Integer pageSize) {
        long current = pageNum == null || pageNum < 1 ? 1L : pageNum;
        long size = pageSize == null ? 20L : Math.min(Math.max(pageSize, 1), 50);

        if (userId == null) {
            throw new HomeworkException(ResultCodeEnum.APP_LOGIN_NOT_AUTH);
        }
        if (groupType == null) {
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }

        Page<FavoriteQuestionBankVO> page = new Page<>(current, size);

        IPage<FavoriteQuestionBankVO> favoriteQuestionBanks = userFavoriteQuestionMapper.getFavQuestionBanks(page, groupType, userId);

        PageResult<FavoriteQuestionBankVO> result = new PageResult<>();
        result.setTotal(favoriteQuestionBanks.getTotal());
        result.setRecords(favoriteQuestionBanks.getRecords());
        result.setPageNum(favoriteQuestionBanks.getCurrent());
        result.setPageSize(favoriteQuestionBanks.getSize());


        List<FavoriteQuestionBankVO> records = result.getRecords();
        if (records.isEmpty()) {
            return result;
        }
        List<Long> bankIds = records.stream().map(FavoriteQuestionBankVO::getBankId).toList();


        LambdaQueryWrapper<BankTag> tagQueryWrapper = new LambdaQueryWrapper<>();
        tagQueryWrapper.in(BankTag::getBankId, bankIds);
        List<BankTag> bankTags = bankTagMapper.selectList(tagQueryWrapper);
        Map<Long, List<String>> tagNamesMap = bankTags.stream().collect(Collectors.groupingBy(BankTag::getBankId, Collectors.mapping(BankTag::getTagName, Collectors.toList())));

        records.forEach(record ->
                record.setTagNames(tagNamesMap.getOrDefault(record.getBankId(), Collections.emptyList())));
        return result;
    }

    @Override
    public PageResult<FavoriteQuestionVO> getFavoriteQuestions(Long userId, Long bankId, Integer pageNum, Integer pageSize) {
        //校验pageNum, pageSize，以防前端传的数字过大
        long current = pageNum == null || pageNum < 1 ? 1L : pageNum;
        long size = pageSize == null ? 20L : Math.min(Math.max(pageSize, 1), 50);

        if (userId == null) {
            throw new HomeworkException(ResultCodeEnum.APP_LOGIN_NOT_AUTH);
        }

        if (bankId == null) {
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }

        GroupType groupType = questionBankMapper.getGroupType(bankId);
        if (groupType == null) {
            throw new HomeworkException(ResultCodeEnum.DATA_ERROR);
        }

        Page<FavoriteQuestionVO> page = new Page<>(current, size);
        IPage<FavoriteQuestionVO> favQuestions = userFavoriteQuestionMapper.getFavQuestions(page, userId, bankId);


        PageResult<FavoriteQuestionVO> result = new PageResult<>();
        result.setTotal(favQuestions.getTotal());
        result.setRecords(favQuestions.getRecords());
        result.setPageNum(favQuestions.getCurrent());
        result.setPageSize(favQuestions.getSize());


        List<FavoriteQuestionVO> records = result.getRecords();
        if (records.isEmpty()) {
            return result;
        }

        List<Long> questionIds = records.stream().map(FavoriteQuestionVO::getQuestionId).toList();

        if (groupType.equals(GroupType.INTERVIEW)) {
            LambdaQueryWrapper<InterviewQuestionInfo> interviewInfoQueryWrapper = new LambdaQueryWrapper<>();
            interviewInfoQueryWrapper.in(InterviewQuestionInfo::getId, questionIds)
                    .eq(InterviewQuestionInfo::getStatus, QuestionInfoStatus.PUBLISHED)
                    .eq(InterviewQuestionInfo::getQuestionType, QuestionInfoQuestionType.ESSAY);
            List<InterviewQuestionInfo> interviewQuestionInfos = interviewQuestionInfoMapper.selectList(interviewInfoQueryWrapper);
            Map<Long, String> questionIdToTitleMap = interviewQuestionInfos.stream().collect(Collectors.toMap(InterviewQuestionInfo::getId, InterviewQuestionInfo::getTitle));
            Map<Long, QuestionInfoQuestionType> questionTypeMap = interviewQuestionInfos.stream().collect(Collectors.toMap(InterviewQuestionInfo::getId, InterviewQuestionInfo::getQuestionType));

            records.forEach(record -> {
                String title = questionIdToTitleMap.get(record.getQuestionId());
                QuestionInfoQuestionType questionType = questionTypeMap.get(record.getQuestionId());
                if (!StringUtils.hasText(title)) {
                    record.setIsAvailable(false); //给前端一个标识，当isAvailable = false，表示这道题已下架或删除
                    record.setTitle(null);
                    record.setQuestionType(null); //没找到questionId，title和type都设置为null，因为这两个字段都是必填项，当找不到questionId时，说明这道题已下架或删除
                } else {
                    record.setIsAvailable(true);
                    record.setTitle(title);
                    record.setQuestionType(questionType);
                }

            });
        }
        if (groupType.equals(GroupType.CERTIFICATION)) {
            LambdaQueryWrapper<CertificateQuestionInfo> questionInfoQueryWrapper = new LambdaQueryWrapper<>();
            questionInfoQueryWrapper.in(CertificateQuestionInfo::getId, questionIds)
                    .eq(CertificateQuestionInfo::getStatus, QuestionInfoStatus.PUBLISHED)
                    .in(CertificateQuestionInfo::getQuestionType, QuestionInfoQuestionType.SINGLE_CHOICE, QuestionInfoQuestionType.MULTIPLE);
            List<CertificateQuestionInfo> certificateQuestionInfos = certificateQuestionInfoMapper.selectList(questionInfoQueryWrapper);
            Map<Long, String> questionIdToTitleMap = certificateQuestionInfos.stream().collect(Collectors.toMap(CertificateQuestionInfo::getId, CertificateQuestionInfo::getTitle));
            Map<Long, QuestionInfoQuestionType> questionTypeMap = certificateQuestionInfos.stream().collect(Collectors.toMap(CertificateQuestionInfo::getId, CertificateQuestionInfo::getQuestionType));


            records.forEach(record -> {
                String title = questionIdToTitleMap.get(record.getQuestionId());
                QuestionInfoQuestionType questionType = questionTypeMap.get(record.getQuestionId());
                if (!StringUtils.hasText(title)) {
                    record.setIsAvailable(false); //给前端一个标识，当isAvailable = false，表示这道题已下架或删除
                    record.setTitle(null);
                    record.setQuestionType(null);
                } else {
                    record.setIsAvailable(true);
                    record.setTitle(title);
                    record.setQuestionType(questionType);
                }
            });
        }
        return result;
    }

    @Override
    public FavoriteQuestionReviewVO getFavoriteQuestion(Long userId, Long bankId, Long questionId) {
        if (userId == null) {
            throw new HomeworkException(ResultCodeEnum.APP_LOGIN_NOT_AUTH);
        }

        if (bankId == null || questionId == null) {
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }

        GroupType groupType = questionBankMapper.getGroupType(bankId);
        if (groupType == null) {
            throw new HomeworkException(ResultCodeEnum.DATA_ERROR);
        }

        //用于校验用户主动取消的收藏题目
        LambdaQueryWrapper<UserFavoriteQuestion> userFavoriteQuestionQueryWrapper = new LambdaQueryWrapper<>();
        userFavoriteQuestionQueryWrapper.eq(UserFavoriteQuestion::getUserId, userId)
                .eq(UserFavoriteQuestion::getBankId, bankId)
                .eq(UserFavoriteQuestion::getQuestionId, questionId);
        UserFavoriteQuestion userFavoriteQuestion = userFavoriteQuestionMapper.selectOne(userFavoriteQuestionQueryWrapper);
        if (userFavoriteQuestion == null) {
            throw new HomeworkException(ResultCodeEnum.DATA_ERROR);
        }

        FavoriteQuestionReviewVO vo = new FavoriteQuestionReviewVO();
        if (groupType.equals(GroupType.INTERVIEW)) {
            LambdaQueryWrapper<InterviewQuestionInfo> interviewInfoQueryWrapper = new LambdaQueryWrapper<>();
            interviewInfoQueryWrapper.eq(InterviewQuestionInfo::getId, questionId)
                    .eq(InterviewQuestionInfo::getStatus, QuestionInfoStatus.PUBLISHED)
                    .eq(InterviewQuestionInfo::getQuestionType, QuestionInfoQuestionType.ESSAY);
            InterviewQuestionInfo interviewInfo = interviewQuestionInfoMapper.selectOne(interviewInfoQueryWrapper);
            if (interviewInfo == null) {
                throw new HomeworkException(ResultCodeEnum.DATA_ERROR);
            }

            vo.setTitle(interviewInfo.getTitle());
            vo.setAnalysis(interviewInfo.getAnalysis());
            vo.setQuestionType(interviewInfo.getQuestionType());
            vo.setQuestionId(interviewInfo.getId());
            vo.setImageUrl(readUrlSigner.sign(interviewInfo.getImageObjectKey()));
        }
        if (groupType.equals(GroupType.CERTIFICATION)) {
            LambdaQueryWrapper<CertificateQuestionInfo> questionInfoQueryWrapper = new LambdaQueryWrapper<>();
            questionInfoQueryWrapper.eq(CertificateQuestionInfo::getId, questionId)
                    .eq(CertificateQuestionInfo::getStatus, QuestionInfoStatus.PUBLISHED)
                    .in(CertificateQuestionInfo::getQuestionType, QuestionInfoQuestionType.SINGLE_CHOICE, QuestionInfoQuestionType.MULTIPLE);
            CertificateQuestionInfo certificateInfo = certificateQuestionInfoMapper.selectOne(questionInfoQueryWrapper);
            if (certificateInfo == null) {
                throw new HomeworkException(ResultCodeEnum.DATA_ERROR);
            }
            vo.setTitle(certificateInfo.getTitle());
            vo.setAnalysis(certificateInfo.getAnalysis());
            vo.setQuestionType(certificateInfo.getQuestionType());
            vo.setQuestionId(certificateInfo.getId());
            vo.setCorrectAnswer(certificateInfo.getCorrectAnswer());
            vo.setOptions(certificateInfo.getOptions());
            vo.setImageUrl(readUrlSigner.sign(certificateInfo.getImageObjectKey()));
        }
        return vo;
    }

    @Override
    public PageResult<NoteBankVO> getNoteBanks(Long userId, GroupType groupType, Integer pageNum, Integer pageSize) {

        long current = pageNum == null || pageNum < 1 ? 1L : pageNum;
        long size = pageSize == null ? 20L : Math.min(Math.max(pageSize, 1), 50);

        if (userId == null) {
            throw new HomeworkException(ResultCodeEnum.APP_LOGIN_NOT_AUTH);
        }

        if (groupType == null) {
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }

        Page<NoteBankVO> page = new Page<>(current, size);

        IPage<NoteBankVO> noteBanks = userQuestionNoteMapper.getNoteBanks(page, groupType, userId);

        PageResult<NoteBankVO> result = new PageResult<>();
        result.setTotal(noteBanks.getTotal());
        result.setRecords(noteBanks.getRecords());
        result.setPageNum(noteBanks.getCurrent());
        result.setPageSize(noteBanks.getSize());


        List<NoteBankVO> records = result.getRecords();
        if (records.isEmpty()) {
            return result;
        }
        List<Long> bankIds = records.stream().map(NoteBankVO::getBankId).toList();


        LambdaQueryWrapper<BankTag> tagQueryWrapper = new LambdaQueryWrapper<>();
        tagQueryWrapper.in(BankTag::getBankId, bankIds);
        List<BankTag> bankTags = bankTagMapper.selectList(tagQueryWrapper);
        Map<Long, List<String>> tagNamesMap = bankTags.stream().collect(Collectors.groupingBy(BankTag::getBankId, Collectors.mapping(BankTag::getTagName, Collectors.toList())));

        records.forEach(record ->
                record.setTagNames(tagNamesMap.getOrDefault(record.getBankId(), Collections.emptyList())));
        return result;

    }

    @Override
    public PageResult<NoteQuestionVO> getNoteQuestions(Long userId, Long bankId, Integer pageNum, Integer pageSize) {
        long current = pageNum == null || pageNum < 1 ? 1L : pageNum;
        long size = pageSize == null ? 20L : Math.min(Math.max(pageSize, 1), 50);

        if (userId == null) {
            throw new HomeworkException(ResultCodeEnum.APP_LOGIN_NOT_AUTH);
        }

        if (bankId == null) {
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }

        GroupType groupType = questionBankMapper.getGroupType(bankId);
        if (groupType == null) {
            throw new HomeworkException(ResultCodeEnum.DATA_ERROR);
        }

        Page<NoteQuestionVO> page = new Page<>(current, size);
        IPage<NoteQuestionVO> noteQuestions = userQuestionNoteMapper.getNoteQuestions(page, userId, bankId);

        PageResult<NoteQuestionVO> result = new PageResult<>();
        result.setTotal(noteQuestions.getTotal());
        result.setRecords(noteQuestions.getRecords());
        result.setPageNum(noteQuestions.getCurrent());
        result.setPageSize(noteQuestions.getSize());

        List<NoteQuestionVO> records = result.getRecords();
        if (records.isEmpty()) {
            return result;
        }
        List<Long> questionIds = records.stream().map(NoteQuestionVO::getQuestionId).toList();

        if (groupType.equals(GroupType.INTERVIEW)) {
            LambdaQueryWrapper<InterviewQuestionInfo> interviewInfoQueryWrapper = new LambdaQueryWrapper<>();
            interviewInfoQueryWrapper.in(InterviewQuestionInfo::getId, questionIds)
                    .eq(InterviewQuestionInfo::getStatus, QuestionInfoStatus.PUBLISHED)
                    .eq(InterviewQuestionInfo::getQuestionType, QuestionInfoQuestionType.ESSAY);
            List<InterviewQuestionInfo> interviewQuestionInfos = interviewQuestionInfoMapper.selectList(interviewInfoQueryWrapper);
            Map<Long, String> questionIdToTitleMap = interviewQuestionInfos.stream().collect(Collectors.toMap(InterviewQuestionInfo::getId, InterviewQuestionInfo::getTitle));
            Map<Long, QuestionInfoQuestionType> questionTypeMap = interviewQuestionInfos.stream().collect(Collectors.toMap(InterviewQuestionInfo::getId, InterviewQuestionInfo::getQuestionType));

            records.forEach(record -> {
                String title = questionIdToTitleMap.get(record.getQuestionId());
                QuestionInfoQuestionType questionType = questionTypeMap.get(record.getQuestionId());
                if (!StringUtils.hasText(title)) {
                    record.setIsAvailable(false); //给前端一个标识，当isAvailable = false，表示这道题已下架或删除
                    record.setTitle(null);
                    record.setQuestionType(null); //没找到questionId，title和type都设置为null，因为这两个字段都是必填项，当找不到questionId时，说明这道题已下架或删除
                } else {
                    record.setIsAvailable(true);
                    record.setTitle(title);
                    record.setQuestionType(questionType);
                }

            });
        }
        if (groupType.equals(GroupType.CERTIFICATION)) {
            LambdaQueryWrapper<CertificateQuestionInfo> questionInfoQueryWrapper = new LambdaQueryWrapper<>();
            questionInfoQueryWrapper.in(CertificateQuestionInfo::getId, questionIds)
                    .eq(CertificateQuestionInfo::getStatus, QuestionInfoStatus.PUBLISHED)
                    .in(CertificateQuestionInfo::getQuestionType, QuestionInfoQuestionType.SINGLE_CHOICE, QuestionInfoQuestionType.MULTIPLE);
            List<CertificateQuestionInfo> certificateQuestionInfos = certificateQuestionInfoMapper.selectList(questionInfoQueryWrapper);
            Map<Long, String> questionIdToTitleMap = certificateQuestionInfos.stream().collect(Collectors.toMap(CertificateQuestionInfo::getId, CertificateQuestionInfo::getTitle));
            Map<Long, QuestionInfoQuestionType> questionTypeMap = certificateQuestionInfos.stream().collect(Collectors.toMap(CertificateQuestionInfo::getId, CertificateQuestionInfo::getQuestionType));


            records.forEach(record -> {
                String title = questionIdToTitleMap.get(record.getQuestionId());
                QuestionInfoQuestionType questionType = questionTypeMap.get(record.getQuestionId());
                if (!StringUtils.hasText(title)) {
                    record.setIsAvailable(false); //给前端一个标识，当isAvailable = false，表示这道题已下架或删除
                    record.setTitle(null);
                    record.setQuestionType(null);
                } else {
                    record.setIsAvailable(true);
                    record.setTitle(title);
                    record.setQuestionType(questionType);
                }
            });
        }
        return result;

    }

    @Override
    public NoteVO getNote(Long userId, Long bankId, Long questionId) {
        if (userId == null) {
            throw new HomeworkException(ResultCodeEnum.APP_LOGIN_NOT_AUTH);
        }

        if (bankId == null || questionId == null) {
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }

        GroupType groupType = questionBankMapper.getGroupType(bankId);
        if (groupType == null) {
            throw new HomeworkException(ResultCodeEnum.DATA_ERROR);
        }

        LambdaQueryWrapper<UserQuestionNote> userQuestionNoteQueryWrapper = new LambdaQueryWrapper<>();
        userQuestionNoteQueryWrapper.eq(UserQuestionNote::getUserId, userId)
                .eq(UserQuestionNote::getBankId, bankId)
                .eq(UserQuestionNote::getQuestionId, questionId);
        UserQuestionNote userQuestionNote = userQuestionNoteMapper.selectOne(userQuestionNoteQueryWrapper);
        if (userQuestionNote == null) {
            throw new HomeworkException(ResultCodeEnum.DATA_ERROR);
        }

        NoteVO vo = new NoteVO();
        if (groupType.equals(GroupType.INTERVIEW)) {
            LambdaQueryWrapper<InterviewQuestionInfo> interviewInfoQueryWrapper = new LambdaQueryWrapper<>();
            interviewInfoQueryWrapper.eq(InterviewQuestionInfo::getId, questionId)
                    .eq(InterviewQuestionInfo::getStatus, QuestionInfoStatus.PUBLISHED)
                    .eq(InterviewQuestionInfo::getQuestionType, QuestionInfoQuestionType.ESSAY);
            InterviewQuestionInfo interviewInfo = interviewQuestionInfoMapper.selectOne(interviewInfoQueryWrapper);
            if (interviewInfo == null) {
                throw new HomeworkException(ResultCodeEnum.DATA_ERROR);
            }

            vo.setTitle(interviewInfo.getTitle());
            vo.setAnalysis(interviewInfo.getAnalysis());
            vo.setQuestionType(interviewInfo.getQuestionType());
            vo.setQuestionId(interviewInfo.getId());
            vo.setImageUrl(readUrlSigner.sign(interviewInfo.getImageObjectKey()));
        }
        if (groupType.equals(GroupType.CERTIFICATION)) {
            LambdaQueryWrapper<CertificateQuestionInfo> questionInfoQueryWrapper = new LambdaQueryWrapper<>();
            questionInfoQueryWrapper.eq(CertificateQuestionInfo::getId, questionId)
                    .eq(CertificateQuestionInfo::getStatus, QuestionInfoStatus.PUBLISHED)
                    .in(CertificateQuestionInfo::getQuestionType, QuestionInfoQuestionType.SINGLE_CHOICE, QuestionInfoQuestionType.MULTIPLE);
            CertificateQuestionInfo certificateInfo = certificateQuestionInfoMapper.selectOne(questionInfoQueryWrapper);
            if (certificateInfo == null) {
                throw new HomeworkException(ResultCodeEnum.DATA_ERROR);
            }
            vo.setTitle(certificateInfo.getTitle());
            vo.setAnalysis(certificateInfo.getAnalysis());
            vo.setQuestionType(certificateInfo.getQuestionType());
            vo.setQuestionId(certificateInfo.getId());
            vo.setCorrectAnswer(certificateInfo.getCorrectAnswer());
            vo.setOptions(certificateInfo.getOptions());
            vo.setImageUrl(readUrlSigner.sign(certificateInfo.getImageObjectKey()));
        }
        vo.setNoteId(userQuestionNote.getId());
        vo.setUpdatedTime(userQuestionNote.getUpdatedTime());
        vo.setNoteContent(userQuestionNote.getNoteContent());

        return vo;

    }

    @Override
    public MembershipInfoVO getMembershipInfo(Long userId) {
        if(userId == null){
            throw new HomeworkException(ResultCodeEnum.APP_LOGIN_NOT_AUTH);
        }

        //先查询用户名和头像
        LambdaQueryWrapper<UserInfo> userInfoQueryWrapper = new LambdaQueryWrapper<>();
        userInfoQueryWrapper.eq(UserInfo::getId,userId)
                .eq(UserInfo::getStatus,UserInfoStatus.ACTIVE);
        UserInfo userInfo = userInfoMapper.selectOne(userInfoQueryWrapper);
        if(userInfo == null){
            throw new HomeworkException(ResultCodeEnum.DATA_ERROR);
        }

        MembershipInfoVO vo = new MembershipInfoVO();
        vo.setDisplayName(userInfo.getDisplayName());
        vo.setAvatarUrl(userImageUrlResolver.resolveAvatar(userInfo.getAvatarObjectKey()));

        MembershipAccessSnapshot membership = membershipAccessService.getAccess(userId);
        vo.setMemberStatus(membership.status());
        vo.setMembershipType(membership.membershipType());
        vo.setExpiredTime(membership.currentExpireTime());
        vo.setBaseFreezeExpireTime(membership.baseFreezeExpireTime());
        return vo;
    }

    @Override
    @Transactional
    public EditedProfileVO editProfile(Long userId, EditProfileDTO dto) {
        if(userId == null){
            throw new HomeworkException(ResultCodeEnum.APP_LOGIN_NOT_AUTH);
        }
        if(dto == null || !StringUtils.hasText(dto.getDisplayName())){
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }



        LambdaQueryWrapper<UserInfo> userInfoQueryWrapper = new LambdaQueryWrapper<>();
        userInfoQueryWrapper.eq(UserInfo::getId,userId)
                .eq(UserInfo::getStatus,UserInfoStatus.ACTIVE);
        UserInfo userInfo = userInfoMapper.selectOne(userInfoQueryWrapper);
        if(userInfo == null){
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }

        Integer version = userInfo.getVersion();
        if(!version.equals(dto.getVersion())){
            throw new HomeworkException(ResultCodeEnum.APP_VERSION_CONFLICT);
        }

        //校验前端传入的这个 subTechDirectionId，在数据库表中是否存在
        //如果不做校验，会让错误的，对不上任何数据的 subTechDirectionId 进入到 数据库
        if(dto.getSubTechDirectionId() != null){
            SubTechDirection subTechDirection = subTechDirectionMapper.selectById(dto.getSubTechDirectionId());
            if(subTechDirection == null){
                throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
            }
        }

        //这个EditedProfileVO, 是用户提交的修改信息成功保存之后，返回的数据
        //avatarUrl 不在这里返回
        EditedProfileVO vo = new EditedProfileVO();
        vo.setDisplayName(dto.getDisplayName().strip());
        vo.setSubTechDirectionId(dto.getSubTechDirectionId());
        vo.setIntroduction(dto.getIntroduction() == null ? null : dto.getIntroduction().strip());
        vo.setGender(dto.getGender());
        vo.setCompanyOrSchool((dto.getCompanyOrSchool() == null ? null : dto.getCompanyOrSchool().strip()));


        //MyBatis-Plus 的普通更新默认通常会忽略 null 字段
        //某些清空的字段，MyBatis-Plus会忽略掉 null，而依然使用数据库原来的值
        //因此需要自定义一个SQL，别忘记自己更新乐观锁版本
        int result = userInfoMapper.updateProfile(vo,userId,version);

        //乐观锁防并发
        if(result != 1){
            throw new HomeworkException(ResultCodeEnum.APP_VERSION_CONFLICT);
        }
        //重新查询更新后的 userInfo，此时的 version 已成功+1
        UserInfo userinfoUpdate = userInfoMapper.selectById(userId);
        vo.setVersion(userinfoUpdate.getVersion());
        return vo;


    }

    @Override
    public ProfileVO getProfile(Long userId) {

        if(userId == null){
            throw new HomeworkException(ResultCodeEnum.APP_LOGIN_NOT_AUTH);
        }

        LambdaQueryWrapper<UserInfo> userInfoQueryWrapper = new LambdaQueryWrapper<>();
        userInfoQueryWrapper.eq(UserInfo::getId,userId)
                .eq(UserInfo::getStatus,UserInfoStatus.ACTIVE);
        UserInfo userInfo = userInfoMapper.selectOne(userInfoQueryWrapper);
        if(userInfo == null){
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }

        ProfileVO vo = new ProfileVO();
        vo.setDisplayName(userInfo.getDisplayName());
        vo.setIntroduction(userInfo.getIntroduction());
        vo.setVersion(userInfo.getVersion());
        vo.setCompanyOrSchool(userInfo.getCompanyOrSchool());
        vo.setGender(userInfo.getGender());
        vo.setAvatarUrl(userImageUrlResolver.resolveAvatar(userInfo.getAvatarObjectKey()));
        vo.setSubTechDirectionId(userInfo.getSubTechDirectionId());

        return vo;
    }

    @Override
    public ProfileOptionsVO getProfileOptions() {

        ProfileOptionsVO profileOptionsVO = new ProfileOptionsVO();

        List<TechDirectionTreeVO> techDirectionTreeVOList = new ArrayList<>();

        LambdaQueryWrapper<TechDirection> techQueryWrapper = new LambdaQueryWrapper<>();
        //这里有一个小技巧，如果想查询全部，LambdaQueryWrapper没办法输入null，那么就随便查一个排序条件，从而获得表中全部数据
        techQueryWrapper.orderByAsc(TechDirection::getId);
        List<TechDirection> techDirectionList = techDirectionMapper.selectList(techQueryWrapper);
        techDirectionList.forEach(techDirection -> {
            TechDirectionTreeVO vo = new TechDirectionTreeVO();
            vo.setDirectionName(techDirection.getTechDirectionName());
            vo.setDirectionId(techDirection.getId());

            List<SubTechDirectionTreeVO> subTechDirectionTreeVOList = new ArrayList<>();
            LambdaQueryWrapper<SubTechDirection> subTechQueryWrapper = new LambdaQueryWrapper<>();
            //注意：二级分类树可不再是查全部了，要根据一级分类树的ID（也就是外键）进行查询
            subTechQueryWrapper.eq(SubTechDirection::getDirectionId,techDirection.getId())
                    .orderByAsc(SubTechDirection::getId);
            List<SubTechDirection> subTechDirectionList = subTechDirectionMapper.selectList(subTechQueryWrapper);
            subTechDirectionList.forEach(subTechDirection -> {
                SubTechDirectionTreeVO subVo = new SubTechDirectionTreeVO();
                subVo.setSubTechDirectionId(subTechDirection.getId());
                subVo.setSubTechDirectionName(subTechDirection.getSubDirectionName());
                subTechDirectionTreeVOList.add(subVo);
            });
            vo.setSubTechDirectionTreeVOList(subTechDirectionTreeVOList);
            techDirectionTreeVOList.add(vo);
        });

        profileOptionsVO.setTechDirectionTreeVOList(techDirectionTreeVOList);
        return profileOptionsVO;
    }

}
