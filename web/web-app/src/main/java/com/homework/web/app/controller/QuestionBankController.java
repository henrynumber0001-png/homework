package com.homework.web.app.controller;

import com.homework.common.result.Result;
import com.homework.web.app.service.QuestionBankService;
import com.homework.web.app.vo.GroupPageVO;
import com.homework.web.app.vo.ModulePageVO;
import com.homework.web.app.vo.SubModulePageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping("/group-page/module-page")
    public Result<ModulePageVO> getModulePage(@RequestParam Long groupId, @RequestParam Long moduleId){
        ModulePageVO modulePageVo = questionBankService.getModulePage(groupId, moduleId);
        return Result.success(modulePageVo);
    }

    public Result<SubModulePageVO> getSubModulePage
            (@RequestParam Long groupId, @RequestParam Long moduleId,
             @RequestParam Long subModuleId){
        SubModulePageVO subModulePageVO = questionBankService.getSubModulePage(groupId,moduleId,subModuleId);
        return Result.success(subModulePageVO);
    }
}
