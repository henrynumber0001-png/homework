package com.homework.web.app.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.homework.common.exception.HomeworkException;
import com.homework.common.result.ResultCodeEnum;
import com.homework.model.entity.CategoryModule;
import com.homework.model.entity.CategorySubModule;
import com.homework.model.entity.GraphInfo;
import com.homework.model.entity.QuestionBank;
import com.homework.model.enums.ItemType;
import com.homework.model.enums.SortType;
import com.homework.web.app.mapper.*;
import com.homework.web.app.service.QuestionBankService;
import com.homework.web.app.vo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuestionBankServiceImpl extends ServiceImpl<QuestionBankMapper, QuestionBank> implements QuestionBankService {

    private final CategoryModuleMapper categoryModuleMapper;
    private final CategorySubModuleMapper categorySubModuleMapper;
    private final QuestionBankMapper questionBankMapper;
    private final GraphInfoMapper graphInfoMapper;
    private final CategoryGroupMapper categoryGroupMapper;

    @Override
    public GroupPageVO getGroupPage(Long groupId) {
        if (groupId == null) {
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }

        List<CategoryModuleVO> categoryModuleVos = listModuleVos(groupId);
        if (categoryModuleVos.isEmpty()) {
            throw new HomeworkException(ResultCodeEnum.DATA_ERROR);
        }
        CategoryModuleVO firstModuleVo = categoryModuleVos.get(0);

        Long moduleId = firstModuleVo.getId();
        List<CategorySubModuleVO> subModuleVos = listSubModuleVos(moduleId);

        CategorySubModuleVO firstSubModuleVo = subModuleVos.get(0);

        Long subModuleId = firstSubModuleVo.getId();
        List<QuestionBankVO> questionBankVos = listQuestionBanks(subModuleId);

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
    public ModulePageVO getModulePage(Long groupId, Long moduleId) {
        if (groupId == null || moduleId == null) {
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }

        if(!categoryGroupMapper.selectById(groupId).getId().equals(moduleId)){
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }
        //当moduleId = firstModuleVo.id，不做任何反应
        if(listModuleVos(groupId).get(0).getId().equals(moduleId)){
            return null;
        }

        List<CategorySubModuleVO> subModuleVos = listSubModuleVos(moduleId);
        CategorySubModuleVO firstSubModuleVo = subModuleVos.get(0);

        Long questionBankId = firstSubModuleVo.getId();
        List<QuestionBankVO> questionBankVos = listQuestionBanks(questionBankId);

        ModulePageVO vo = new ModulePageVO();
        vo.setFirstSubModule(firstSubModuleVo);
        vo.setSort(SortType.HOT);
        vo.setSubModules(subModuleVos);
        vo.setBanks(questionBankVos);
        return vo;
    }

    @Override
    public SubModulePageVO getSubModulePage(Long groupId, Long moduleId, Long subModuleId) {
        if (groupId == null || moduleId == null || subModuleId == null) {
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

    private List<QuestionBankVO> listQuestionBanks(Long subModuleId) {

        LambdaQueryWrapper<QuestionBank> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(QuestionBank::getSubModuleId, subModuleId)
                .orderByAsc(QuestionBank::getHotScore) //首次进入题库页面，默认按照“热度”排序
                .orderByDesc(QuestionBank::getId);

        List<QuestionBank> questionBanks = questionBankMapper.selectList(queryWrapper);

        if (questionBanks.isEmpty()) {
            throw new HomeworkException(ResultCodeEnum.DATA_ERROR);
        }

        List<QuestionBankVO> questionBankVos = questionBanks.stream().map(this::toQuestionBankVO).collect(Collectors.toList());
        return questionBankVos;
    }

    private CategoryModuleVO toModuleVo(CategoryModule entity) {
        CategoryModuleVO vo = new CategoryModuleVO();
        vo.setId(entity.getId());
        vo.setGroupId(entity.getGroupId()); //暂时先别删，等继续开发后面的功能，如果不需要再删除；
        vo.setModuleName(entity.getModuleName());
        vo.setSortOrder(entity.getSortOrder());
        GraphInfoVo graphInfoVo = getGraphInfoVo(entity.getId());
        vo.setGraphInfoVo(graphInfoVo);

        return vo;
    }

    private CategorySubModuleVO toSubModuleVO(CategorySubModule entity) {
        CategorySubModuleVO vo = new CategorySubModuleVO();
        vo.setId(entity.getId());
        vo.setModuleId(entity.getModuleId());//暂时先别删，等继续开发后面的功能，如果不需要再删除；
        vo.setSubModuleName(entity.getSubModuleName());
        vo.setSortOrder(entity.getSortOrder());
        return vo;
    }

    private QuestionBankVO toQuestionBankVO(QuestionBank entity) {
        QuestionBankVO vo = new QuestionBankVO();
        vo.setId(entity.getId()); //用于标记题库，这样后续用户再点击每一个题库，就可以知道是哪个题库了
        vo.setBankName(entity.getBankName());
        vo.setSubModuleId(entity.getSubModuleId());//暂时先别删，等继续开发后面的功能，如果不需要再删除；
        vo.setCompleteUserCount(entity.getCompleteUserCount());
        vo.setAvgCorrectRate(entity.getAvgCorrectRate());
        vo.setIsPremium(entity.getIsPremium());
        return vo;
    }

    private GraphInfoVo getGraphInfoVo(Long moduleId){
        LambdaQueryWrapper<GraphInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(GraphInfo::getItemType, ItemType.MODULE)
                .eq(GraphInfo::getItemId, moduleId);


        GraphInfo graphInfo = graphInfoMapper.selectOne(queryWrapper);
        if(graphInfo == null){
            return null; //是允许返回null的，就表示没有图片
        }
        String url = graphInfo.getUrl();
        GraphInfoVo graphInfoVo = new GraphInfoVo();
        graphInfoVo.setUrl(url);

        return graphInfoVo;
    }
}
