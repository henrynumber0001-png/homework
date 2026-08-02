package com.homework.web.admin.controller;

import com.homework.common.result.PageResult;
import com.homework.common.result.Result;
import com.homework.model.enums.QuestionBankStatus;
import com.homework.web.admin.auth.AdminPermission;
import com.homework.web.admin.dto.QuestionBankActionDTO;
import com.homework.web.admin.dto.QuestionBankCreateDTO;
import com.homework.web.admin.dto.QuestionBankUpdateDTO;
import com.homework.web.admin.service.AdminQuestionBankService;
import com.homework.web.admin.vo.ActionResultVO;
import com.homework.web.admin.vo.QuestionBankRowVO;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 后台题库管理接口。 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/question-banks")
public class AdminQuestionBankController {

    private final AdminQuestionBankService bankService;

    /** 分页查询当前管理员有权查看的题库。 */
    @Operation(summary = "分页查询题库")
    @AdminPermission("bank:view")
    @GetMapping
    public Result<PageResult<QuestionBankRowVO>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long groupId,
            @RequestParam(required = false) Long moduleId,
            @RequestParam(required = false) Long subModuleId,
            @RequestParam(required = false) QuestionBankStatus status,
            @RequestParam(required = false) Integer pageNum,
            @RequestParam(required = false) Integer pageSize,
            // 变更：原 sortBy + sortDirection 合并为 UPDATED_TIME_DESC / SORT_ORDER_DESC。
            @RequestParam(required = false) String sortMode
    ) {
        return Result.success(bankService.list(
                keyword,
                groupId,
                moduleId,
                subModuleId,
                status,
                pageNum,
                pageSize,
                sortMode
        ));
    }

    /** 查询单个题库的分类、统计和状态详情。 */
    @Operation(summary = "查询题库详情")
    @AdminPermission("bank:view")
    @GetMapping("/{bankId}")
    public Result<QuestionBankRowVO> get(@PathVariable Long bankId) {
        return Result.success(bankService.get(bankId));
    }

    /** 单条创建草稿题库。 */
    @Operation(summary = "创建题库")
    @AdminPermission("bank:create")
    @PostMapping
    public Result<QuestionBankRowVO> create(@Valid @RequestBody QuestionBankCreateDTO dto) {
        return Result.success(bankService.create(dto));
    }

    /** 编辑题库名称、分类、标签和优先级。 */
    @Operation(summary = "编辑题库")
    @AdminPermission("bank:update")
    @PutMapping("/{bankId}")
    public Result<QuestionBankRowVO> update(
            @PathVariable Long bankId,
            @Valid @RequestBody QuestionBankUpdateDTO dto
    ) {
        return Result.success(bankService.update(bankId, dto));
    }

    /** 发布、下架或删除题库。 */
    @Operation(summary = "执行题库状态动作")
    @PostMapping("/{bankId}/actions")
    public Result<ActionResultVO> action(
            @PathVariable Long bankId,
            @Valid @RequestBody QuestionBankActionDTO dto
    ) {
        return Result.success(bankService.action(bankId, dto));
    }
}
