package com.homework.web.app.controller.bank;

import com.homework.common.result.Result;
import com.homework.model.enums.SortType;
import com.homework.web.app.service.QuestionBankService;
import com.homework.web.app.vo.GroupPageVO;
import com.homework.web.app.vo.ModulePageVO;
import com.homework.web.app.vo.QuestionBankVO;
import com.homework.web.app.vo.SubModulePageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/app/question-banks")
@RequiredArgsConstructor
/*
设计思路：
参数分为用户传递 和 前端传递 两个部分（所有 currentId 由前端传递）
每个 handler方法的参数，只能有一个是用户传入的
通过控制用户传递的每个不同的参数，实现自动切换不同接口，刷新不同模块的目的
 */

public class QuestionBankController {

    private final QuestionBankService questionBankService;

    @GetMapping("/group-page")
    public Result<GroupPageVO> getGroupPage(@RequestParam Long groupId) {
        GroupPageVO groupPageVo = questionBankService.getGroupPage(groupId);
        return Result.success(groupPageVo);
    }


    @GetMapping("/group-page/module-page")
    public Result<ModulePageVO> getModulePage(@RequestParam Long currentGroupId,
                                              @RequestParam Long moduleId,
                                              @RequestParam Long currentModuleId
                                              ){
        ModulePageVO modulePageVo = questionBankService.getModulePage(currentGroupId, moduleId, currentModuleId);
        return Result.success(modulePageVo);
    }

    @GetMapping("/group-page/module-page/sub-module-page")
    public Result<SubModulePageVO> getSubModulePage
            (@RequestParam Long currentGroupId, @RequestParam Long currentModuleId,
             @RequestParam Long subModuleId,@RequestParam Long currentSubModuleId){
        SubModulePageVO subModulePageVO = questionBankService.getSubModulePage(currentGroupId,currentModuleId,subModuleId,currentSubModuleId);
        return Result.success(subModulePageVO);
    }


    @GetMapping("/group-page/sort-type")
    public Result<List<QuestionBankVO>> getSortType(@RequestParam SortType sortType, @RequestParam Long currentSubModuleId){
        List<QuestionBankVO> questionBankVOList = questionBankService.getSortType(sortType,currentSubModuleId);
        return Result.success(questionBankVOList);
    }




}
