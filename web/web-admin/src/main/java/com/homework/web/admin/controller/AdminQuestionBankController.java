package com.homework.web.admin.controller;

import com.homework.common.result.Result;
import com.homework.web.app.dto.BankQueryDTO;
import com.homework.web.app.dto.QuestionBankSaveDTO;
import com.homework.web.app.service.QuestionBankService;
import com.homework.web.app.vo.QuestionBankListVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/question-banks")
@RequiredArgsConstructor
public class AdminQuestionBankController {

    private final QuestionBankService questionBankService;

    @GetMapping
    public Result<List<QuestionBankListVO>> list(BankQueryDTO query) {
        return Result.success(questionBankService.listQuestionBanks(query));
    }

    @PostMapping
    public Result<Long> save(@RequestBody QuestionBankSaveDTO dto) {
        return Result.success(questionBankService.saveQuestionBank(dto));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        questionBankService.deleteQuestionBank(id);
        return Result.success();
    }
}
