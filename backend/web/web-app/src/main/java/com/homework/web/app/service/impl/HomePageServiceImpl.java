package com.homework.web.app.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.homework.common.exception.HomeworkException;
import com.homework.common.result.ResultCodeEnum;
import com.homework.model.entity.CategoryModule;
import com.homework.model.entity.CategorySubModule;
import com.homework.model.entity.QuestionBank;
import com.homework.model.enums.GroupType;
import com.homework.model.enums.QuestionBankStatus;
import com.homework.web.app.mapper.CategoryModuleMapper;
import com.homework.web.app.mapper.CategorySubModuleMapper;
import com.homework.web.app.mapper.QuestionBankMapper;
import com.homework.web.app.mapper.UserBankCorrectRateMapper;
import com.homework.web.app.service.HitService;
import com.homework.web.app.service.HomePageService;
import com.homework.web.app.vo.BankCorrectRateVO;
import com.homework.web.app.vo.HitPostVO;
import com.homework.web.app.vo.HomePageVO;
import com.homework.web.app.vo.HotQuestionBankVO;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class HomePageServiceImpl implements HomePageService {

    private final QuestionBankMapper questionBankMapper;
    private final CategorySubModuleMapper categorySubModuleMapper;
    private final CategoryModuleMapper categoryModuleMapper;
    private final HitService hitService;
    private final UserBankCorrectRateMapper userBankCorrectRateMapper;

    @Override
    public HomePageVO getHomePage() {
        HomePageVO homePageVO = new HomePageVO();

        List<HitPostVO> hotPostList = hitService.listHits(1,10);
        homePageVO.setHotPostList(hotPostList);

        //Interview
        List<Long> submoduleIds = categorySubModuleMapper.selectByGroupType(GroupType.INTERVIEW);
        List<HotQuestionBankVO> hotInterBanks = getHotBanks(submoduleIds);
        homePageVO.setInterviewQuestionBankVOList(hotInterBanks);


        //Certificate
        List<Long> cerSubModuleIds = categorySubModuleMapper.selectByGroupType(GroupType.CERTIFICATION);
        List<HotQuestionBankVO> hotCertBanks = getHotBanks(cerSubModuleIds);
        homePageVO.setCertificateQuestionBankVOList(hotCertBanks);

        return homePageVO;

    }

    private List<HotQuestionBankVO> getHotBanks(List<Long> submoduleIds){
        LambdaQueryWrapper<QuestionBank> bankQueryWrapper = new LambdaQueryWrapper<>();
        bankQueryWrapper.in(QuestionBank::getSubModuleId, submoduleIds)
                .eq(QuestionBank::getStatus, QuestionBankStatus.PUBLISHED)
                .orderByDesc(QuestionBank::getHotScore)
                .orderByDesc(QuestionBank::getId)
                .last("limit 5");

        //按照 hotScore 排序，查出来5个题库
        List<QuestionBank> questionBanks = questionBankMapper.selectList(bankQueryWrapper);
        if(questionBanks.isEmpty()){
            throw new HomeworkException(ResultCodeEnum.DATA_ERROR);
        }

        List<Long> bankIds = questionBanks.stream().map(QuestionBank::getId).collect(Collectors.toList());
        //user_bank_correct_rate 是在 QuestionInfoServiceImpl 中被存储的，在这里只是调用，而不是存入数据
        //这里传入的是 bankIds，不是 bankId
        //5个题库，是5个平均正确率，所以要放入一个 List
        List<BankCorrectRateVO> bankCorrectRateVOList = userBankCorrectRateMapper.selectAverageByBankIds(bankIds);
        Map<Long, BigDecimal> bankCorrectRateMap = bankCorrectRateVOList.stream().collect(Collectors.toMap(BankCorrectRateVO::getBankId, BankCorrectRateVO::getAvgCorrectRate));
        questionBanks.forEach(questionBank -> {
            BigDecimal avgCorrectRate = bankCorrectRateMap.get(questionBank.getId());
            questionBank.setAvgCorrectRate(avgCorrectRate);
        });

        //Map<submoduleId,moduleName>
        List<CategorySubModule> categorySubModules = categorySubModuleMapper.selectByIds(submoduleIds);
        List<Long> moduleIds = categorySubModules.stream().map(CategorySubModule::getModuleId).toList();
        List<CategoryModule> categoryModules = categoryModuleMapper.selectByIds(moduleIds);

        //这两个 Map 设计的甚是精妙！
        Map<Long, String> categoryModuleMap = categoryModules.stream().collect(Collectors.toMap(CategoryModule::getId, CategoryModule::getModuleName));
        Map<Long, Long> submoduleMap = categorySubModules.stream().collect(Collectors.toMap(CategorySubModule::getId, CategorySubModule::getModuleId));


        //开始组装首页的热门题库组件
        List<HotQuestionBankVO> hotInterviewBanksVOList = new ArrayList<>();
        questionBanks.forEach(questionBank -> {
            HotQuestionBankVO vo = new HotQuestionBankVO();
            Long moduleId = submoduleMap.get(questionBank.getSubModuleId());
            String moduleName = categoryModuleMap.get(moduleId); //非常精妙的通过外键的双层耦合

            vo.setBankId(questionBank.getId());
            vo.setBankName(questionBank.getBankName());
            vo.setModuleName(moduleName);
            vo.setAvgCorrectRate(questionBank.getAvgCorrectRate());
            vo.setCompleteCount(questionBank.getCompleteCount());
            hotInterviewBanksVOList.add(vo);
        });

        return hotInterviewBanksVOList;
    }
}
