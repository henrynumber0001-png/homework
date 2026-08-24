package com.homework.web.app.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.homework.common.exception.HomeworkException;
import com.homework.common.result.ResultCodeEnum;
import com.homework.model.entity.*;
import com.homework.model.enums.QuestionBankStatus;
import com.homework.model.enums.SortType;
import com.homework.web.app.mapper.*;
import com.homework.web.app.mapper.UserBankCorrectRateMapper;
import com.homework.web.app.service.QuestionBankService;
import com.homework.web.app.vo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuestionBankServiceImpl extends ServiceImpl<QuestionBankMapper, QuestionBank> implements QuestionBankService {

    private final CategoryModuleMapper categoryModuleMapper;
    private final CategorySubModuleMapper categorySubModuleMapper;
    private final QuestionBankMapper questionBankMapper;
    private final BankTagMapper bankTagMapper;
    private final UserBankCorrectRateMapper userBankCorrectRate;

    @Override
    public GroupPageVO getGroupPage(Long groupId) {
        if (groupId == null) {
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }

        List<CategoryModuleVO> categoryModuleVos = listModuleVos(groupId);
        if (categoryModuleVos.isEmpty()) {
            throw new HomeworkException(ResultCodeEnum.DATA_ERROR);
        }

        //点击题库，进入的默认模块页面
        //返回默认 moduleVO 作为高亮提示
        CategoryModuleVO firstModuleVo = categoryModuleVos.get(0);

        Long moduleId = firstModuleVo.getId();
        List<CategorySubModuleVO> subModuleVos = listSubModuleVos(moduleId);

        //返回 第一个（默认）subModule
        //返回默认 subModuleVO 作为高亮提示
        CategorySubModuleVO firstSubModuleVo = subModuleVos.get(0);

        long subModuleId = firstSubModuleVo.getId();
        List<QuestionBankVO> questionBankVos = listQuestionBanksByHot(subModuleId);

        GroupPageVO vo = new GroupPageVO();
        vo.setFirstModule(firstModuleVo);
        vo.setFirstSubModule(firstSubModuleVo);
        vo.setSort(SortType.HOT);
        vo.setModules(categoryModuleVos);
        vo.setSubModules(subModuleVos);
        vo.setBanks(questionBankVos);
        return vo;
    }


    @Override
    /*
    首次 module-page 操作，currentModuleId = 传回前端的 firstModuleVo.id (高亮)
    后续 module-page 操作，currentModuleId = clickedModuleId
     */
    public ModulePageVO getModulePage(Long currentGroupId, Long moduleId, Long currentModuleId) {
        if (currentGroupId == null || moduleId == null || currentModuleId == null) {
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }

        //校验一下用户点击的 moduleId 是否在当前 group 的
        validateModuleInGroup(currentGroupId, moduleId);

        //这个currentModuleId是web前端传入的，非用户传入的
        //用户这次点击的 moduleId，是不是和页面当前已经高亮/选中的 currentModuleId 一样。
        /*
        比如第一次进入 groupPage 后，后端返回：
        {
          "firstModule": {
            "id": 1
          }
        }
        前端此时应该记住：
        currentModuleId = 1
        如果用户又点击了这个 id=1 的 module，前端请求：
        /api/app/question-banks/group-page/module-page?groupId=100&moduleId=1&currentModuleId=1

        不过更推荐前端自己判断：
        if (clickedModuleId === currentModuleId) {
          return;
        }
         */
        if (moduleId.equals(currentModuleId)) {
            return null;
        }

        List<CategorySubModuleVO> subModuleVos = listSubModuleVos(moduleId);
        CategorySubModuleVO firstSubModuleVo = subModuleVos.get(0);

        Long firstSubModuleId = firstSubModuleVo.getId();
        List<QuestionBankVO> questionBankVos = listQuestionBanksByHot(firstSubModuleId);

        ModulePageVO vo = new ModulePageVO();
        vo.setFirstSubModule(firstSubModuleVo);
        vo.setSort(SortType.HOT);
        vo.setSubModules(subModuleVos);
        vo.setBanks(questionBankVos);
        return vo;
    }

    @Override
    /*
    subModule-page 操作，currentModuleId = 传回前端的 firstModuleVo.id (高亮) / clickedModuleId
    首次 subModule-page 操作，currentSubModuleId 参数 = 传回前端的 firstSubModuleVo.id (高亮)
    后续 subModule-page 操作，currentSubModuleId 参数 = clickedSubModuleId
    */
    public SubModulePageVO getSubModulePage(Long currentGroupId, Long currentModuleId, Long subModuleId, Long currentSubModuleId) {
        if (currentGroupId == null || currentModuleId == null || subModuleId == null || currentSubModuleId == null) {
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }

        validateModuleInGroup(currentGroupId, currentModuleId);
        validateSubModuleInModule(currentModuleId, subModuleId);

        //验证subModule的点击是否与前端记录的重复（即是已经是当前subModule）
        if (subModuleId.equals(currentSubModuleId)) {
            return null;
        }

        List<QuestionBankVO> questionBankVos = listQuestionBanksByHot(subModuleId);

        SubModulePageVO vo = new SubModulePageVO();
        vo.setSort(SortType.HOT);
        vo.setBanks(questionBankVos);
        return vo;
    }

    @Override
    public List<QuestionBankVO> getSortType(SortType sortType, Long currentSubModuleId) {
        if (currentSubModuleId == null || sortType == null) {
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }
        if (sortType == SortType.HOT) {
            return listQuestionBanksByHot(currentSubModuleId);
        } else if (sortType == SortType.LATEST) {
            return listQuestionBanksByLatest(currentSubModuleId);
        } else {
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }
    }


    //子查询的逻辑
    private List<CategoryModuleVO> listModuleVos(Long groupId) {
        if (groupId == null) {
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }

        LambdaQueryWrapper<CategoryModule> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CategoryModule::getGroupId, groupId)
                .orderByAsc(CategoryModule::getSortOrder)
                .orderByAsc(CategoryModule::getId);

        List<CategoryModule> categoryModuleList = categoryModuleMapper.selectList(queryWrapper);
        if (categoryModuleList.isEmpty()) {
            throw new HomeworkException(ResultCodeEnum.DATA_ERROR);
        }

        //妙哉啊！把categoryModuleList中的每一个categoryModule，作为参数，通过map()传入到方法体：this.toModuleVO(categoryModule) 中
        //这样就能实现 跨类型转换了
        List<CategoryModuleVO> categoryModuleVOs = categoryModuleList.stream()
                .map(categoryModule -> this.toModuleVo(categoryModule))
                .collect(Collectors.toList());

        return categoryModuleVOs;

    }

    private List<CategorySubModuleVO> listSubModuleVos(Long moduleId) {

        LambdaQueryWrapper<CategorySubModule> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CategorySubModule::getModuleId, moduleId)
                .orderByAsc(CategorySubModule::getSortOrder)
                .orderByAsc(CategorySubModule::getId);

        List<CategorySubModule> categorySubModules = categorySubModuleMapper.selectList(queryWrapper);
        if (categorySubModules.isEmpty()) {
            throw new HomeworkException(ResultCodeEnum.DATA_ERROR);
        }

        List<CategorySubModuleVO> categorySubModuleVos = categorySubModules.stream().map(this::toSubModuleVO).collect(Collectors.toList());
        return categorySubModuleVos;

    }

    private List<QuestionBankVO> listQuestionBanksByHot(Long subModuleId) {

        LambdaQueryWrapper<QuestionBank> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(QuestionBank::getSubModuleId, subModuleId)
                .eq(QuestionBank::getStatus, QuestionBankStatus.PUBLISHED)
                .orderByDesc(QuestionBank::getSortOrder) //排序优先级最高的是自定义的 sortOder
                .orderByDesc(QuestionBank::getHotScore) //首次进入题库页面，默认按照“热度”排序， hot_score 已经写进数据库 question_bank 的 hot_score 字段
                .orderByDesc(QuestionBank::getId);
        List<QuestionBank> questionBanks = questionBankMapper.selectList(queryWrapper);
        if (questionBanks.isEmpty()) {
            return List.of();
        }

        // 平均正确率不是 QuestionBank 的持久化属性，而是按 bankId 实时聚合出的展示数据。
        List<Long> bankIds = questionBanks.stream().map(QuestionBank::getId).toList();
        List<BankCorrectRateVO> bankCorrectRateVOList = userBankCorrectRate.selectAverageByBankIds(bankIds);//这里有个细节，这种查询集合的SQL，返回的也是集合
        Map<Long, BigDecimal> avgCorrectRateMap = bankCorrectRateVOList.stream().collect(Collectors.toMap(BankCorrectRateVO::getBankId, BankCorrectRateVO::getAvgCorrectRate));

        //这里不必担心 avgCorrectRate 是 null 的情况，因为聚合函数 AVG(correct_rate) 前面已经加了 COALESCE，如果没有用户做过这个题库，或者没有错题，那么就返回 0
        List<QuestionBankVO> questionBankVOList = questionBanks.stream()
                .map(questionBank -> toQuestionBankVO(questionBank, avgCorrectRateMap.get(questionBank.getId())))
                .collect(Collectors.toList());

        return questionBankVOList;
    }

    private List<QuestionBankVO> listQuestionBanksByLatest(Long subModuleId) {
        LambdaQueryWrapper<QuestionBank> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(QuestionBank::getSubModuleId, subModuleId)
                .eq(QuestionBank::getStatus, QuestionBankStatus.PUBLISHED)
                // 变更：题库人工权重优先；默认值都为10时，实际顺序继续由创建时间决定。
                .orderByDesc(QuestionBank::getSortOrder)
                .orderByDesc(QuestionBank::getCreatedTime)
                .orderByDesc(QuestionBank::getId);

        List<QuestionBank> questionBanks = questionBankMapper.selectList(queryWrapper);
        if (questionBanks.isEmpty()) {
            return List.of();
        }

        List<Long> bankIds = questionBanks.stream().map(QuestionBank::getId).toList();
        List<BankCorrectRateVO> bankCorrectRateVOList = userBankCorrectRate.selectAverageByBankIds(bankIds);//这里有个细节，这种查询集合的SQL，返回的也是集合
        Map<Long, BigDecimal> avgCorrectRateMap = bankCorrectRateVOList.stream().collect(Collectors.toMap(BankCorrectRateVO::getBankId, BankCorrectRateVO::getAvgCorrectRate));

        List<QuestionBankVO> questionBankVos = questionBanks.stream()
                .map(questionBank -> toQuestionBankVO(questionBank, avgCorrectRateMap.get(questionBank.getId())))
                .collect(Collectors.toList());
        return questionBankVos;
    }


    private void validateModuleInGroup(Long groupId, Long moduleId) {
        LambdaQueryWrapper<CategoryModule> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CategoryModule::getId, moduleId)
                .eq(CategoryModule::getGroupId, groupId);

        if (categoryModuleMapper.selectCount(queryWrapper) == 0) {
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }
    }

    private void validateSubModuleInModule(Long moduleId, Long subModuleId) {
        LambdaQueryWrapper<CategorySubModule> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CategorySubModule::getId, subModuleId)
                .eq(CategorySubModule::getModuleId, moduleId);

        if (categorySubModuleMapper.selectCount(queryWrapper) == 0) {
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }
    }

    private CategoryModuleVO toModuleVo(CategoryModule entity) {
        CategoryModuleVO vo = new CategoryModuleVO();
        vo.setId(entity.getId());
        vo.setModuleName(entity.getModuleName());
        vo.setSortOrder(entity.getSortOrder());
        return vo;
    }

    private CategorySubModuleVO toSubModuleVO(CategorySubModule entity) {
        CategorySubModuleVO vo = new CategorySubModuleVO();
        vo.setId(entity.getId());
        vo.setSubModuleName(entity.getSubModuleName());
        vo.setSortOrder(entity.getSortOrder());
        return vo;
    }

    private QuestionBankVO toQuestionBankVO(QuestionBank entity, BigDecimal avgCorrectRate) {
        LambdaQueryWrapper<BankTag> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(BankTag::getBankId, entity.getId()); //bankId 不会出现 像 questionId 那样可能重复的情况，因为question_bank 只有一张表

        List<BankTag> bankTags = bankTagMapper.selectList(queryWrapper);
        List<String> tagNames = bankTags.stream().map(BankTag::getTagName).toList();

        QuestionBankVO vo = new QuestionBankVO();
        vo.setId(entity.getId()); //用于标记题库，这样后续用户再点击每一个题库，就可以知道是哪个题库了
        vo.setBankName(entity.getBankName());
        vo.setSubModuleId(entity.getSubModuleId());
        vo.setCompleteCount(entity.getCompleteCount());
        vo.setAvgCorrectRate(avgCorrectRate);
        vo.setTagNames(tagNames);
        return vo;
    }

}
