package com.homework.web.app.controller;

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
public class QuestionBankController {

    private final QuestionBankService questionBankService;

    @GetMapping("/group-page")
    public Result<GroupPageVO> getGroupPage(@RequestParam Long groupId) {
        GroupPageVO groupPageVo = questionBankService.getGroupPage(groupId);
        return Result.success(groupPageVo);
    }

//    @GetMapping("/group-page/module-page")
//    public Result<ModulePageVO> getModulePage(@RequestParam Long groupId,
//                                              @RequestParam Long moduleId,
//                                              @RequestParam(required = false) Long currentModuleId){
//        ModulePageVO modulePageVo = questionBankService.getModulePage(groupId, moduleId, currentModuleId);
//        return Result.success(modulePageVo);
//    }
//
//    @GetMapping("/group-page/module-page/sub-module-page")
//    public Result<SubModulePageVO> getSubModulePage
//            (@RequestParam Long groupId, @RequestParam Long moduleId,
//             @RequestParam Long subModuleId,@RequestParam(required = false) Long currentSubModuleId){
//        SubModulePageVO subModulePageVO = questionBankService.getSubModulePage(groupId,moduleId,subModuleId,currentSubModuleId);
//        return Result.success(subModulePageVO);
//    }
//
//    @GetMapping("/question-bank-page")
//    public Result<QuestionBankVO> getQuestionBankPage(@RequestParam Long questionBankId){
//        QuestionBankVO questionBankVO = questionBankService.getQuestionBankPage(questionBankId);
//        return Result.success(questionBankVO);
//    }




    @GetMapping("/group-page/module-page")
    public Result<ModulePageVO> getModulePage(@RequestParam Long currentGroupId,
                                              @RequestParam Long moduleId,
                                              @RequestParam(required = false) Long currentModuleId
                                              ){
        ModulePageVO modulePageVo = questionBankService.getModulePage(currentGroupId, moduleId, currentModuleId);
        return Result.success(modulePageVo);
    }

    @GetMapping("/group-page/module-page/sub-module-page")
    public Result<SubModulePageVO> getSubModulePage
            (@RequestParam Long currentGroupId, @RequestParam Long currentModuleId,
             @RequestParam Long subModuleId,@RequestParam(required = false) Long currentSubModuleId){
        SubModulePageVO subModulePageVO = questionBankService.getSubModulePage(currentModuleId,subModuleId,currentSubModuleId);
        return Result.success(subModulePageVO);
    }


    @GetMapping("/group-page/sort-type")
    public Result<List<QuestionBankVO>> getSortType(@RequestParam SortType sortType, @RequestParam Long currentSubModuleId){
        List<QuestionBankVO> questionBankVOList = questionBankService.getSortType(sortType,currentSubModuleId);
        return Result.success(questionBankVOList);
    }




}
