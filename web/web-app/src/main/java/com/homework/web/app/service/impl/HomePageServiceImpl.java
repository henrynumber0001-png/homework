package com.homework.web.app.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.homework.common.exception.HomeworkException;
import com.homework.common.result.ResultCodeEnum;
import com.homework.model.entity.CategoryModule;
import com.homework.model.entity.CategorySubModule;
import com.homework.model.entity.QuestionBank;
import com.homework.model.enums.GroupType;
import com.homework.web.app.mapper.CategoryModuleMapper;
import com.homework.web.app.mapper.CategorySubModuleMapper;
import com.homework.web.app.mapper.QuestionBankMapper;
import com.homework.web.app.service.HitService;
import com.homework.web.app.service.HomePageService;
import com.homework.web.app.vo.HitPostVO;
import com.homework.web.app.vo.HomePageVO;
import com.homework.web.app.vo.HotQuestionBankVO;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

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
                .orderByDesc(QuestionBank::getHotScore)
                .orderByDesc(QuestionBank::getId)
                .last("limit 5");
        List<QuestionBank> questionBanks = questionBankMapper.selectList(bankQueryWrapper);
        if(questionBanks.isEmpty()){
            throw new HomeworkException(ResultCodeEnum.DATA_ERROR);
        }

        //Map<submoduleId,moduleName>
        List<CategorySubModule> categorySubModules = categorySubModuleMapper.selectByIds(submoduleIds);
        List<Long> moduleIds = categorySubModules.stream().map(CategorySubModule::getModuleId).toList();
        List<CategoryModule> categoryModules = categoryModuleMapper.selectByIds(moduleIds);
        Map<Long, String> categoryModuleMap = categoryModules.stream().collect(Collectors.toMap(CategoryModule::getId, CategoryModule::getModuleName));

        Map<Long, Long> submoduleMap = categorySubModules.stream().collect(Collectors.toMap(CategorySubModule::getId, CategorySubModule::getModuleId));


        List<HotQuestionBankVO> hotInterviewBanksVOList = new ArrayList<>();
        questionBanks.forEach(questionBank -> {
            HotQuestionBankVO vo = new HotQuestionBankVO();
            Long moduleId = submoduleMap.get(questionBank.getSubModuleId());
            String moduleName = categoryModuleMap.get(moduleId);

            vo.setBankId(questionBank.getId());
            vo.setBankName(questionBank.getBankName());
            vo.setModuleName(moduleName);
            vo.setAvgCorrectRate(questionBank.getAvgCorrectRate());
            vo.setCompleteUserCount(questionBank.getCompleteUserCount());
            hotInterviewBanksVOList.add(vo);
        });

        return hotInterviewBanksVOList;
    }
}
