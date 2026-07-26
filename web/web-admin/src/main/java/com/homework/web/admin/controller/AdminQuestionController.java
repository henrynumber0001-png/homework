package com.homework.web.admin.controller;

import com.homework.common.result.PageResult;
import com.homework.common.result.Result;
import com.homework.web.admin.auth.AdminAccessService;
import com.homework.web.admin.auth.AdminPermission;
import com.homework.web.admin.dto.QuestionCreateDTO;
import com.homework.web.admin.dto.QuestionOrderDTO;
import com.homework.web.admin.dto.QuestionUpdateDTO;
import com.homework.web.admin.dto.ResourceActionDTO;
import com.homework.web.admin.service.AdminQuestionService;
import com.homework.web.admin.service.QuestionImageService;
import com.homework.web.admin.vo.ActionResultVO;
import com.homework.web.admin.vo.QuestionDetailVO;
import com.homework.web.admin.vo.QuestionImageUploadVO;
import com.homework.web.admin.vo.QuestionOrderResultVO;
import com.homework.web.admin.vo.QuestionRowVO;
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
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** 后台题目及题目图片接口。 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminQuestionController {

    private final AdminQuestionService questionService;
    private final QuestionImageService imageService;
    private final AdminAccessService accessService;

    /** 分页查询指定题库中的题目。 */
    @Operation(summary = "分页查询题目")
    @AdminPermission("question:view")
    @GetMapping("/question-banks/{bankId}/questions")
    public Result<PageResult<QuestionRowVO>> list(
            @PathVariable Long bankId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String questionType,
            @RequestParam(required = false) Boolean released,
            @RequestParam(required = false) Boolean deleted,
            @RequestParam(required = false) Integer pageNum,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection
    ) {
        return Result.success(questionService.list(
                bankId, keyword, questionType, released, deleted,
                pageNum, pageSize, sortBy, sortDirection
        ));
    }

    /** 查询题目主体、选项和关联题库详情。 */
    @Operation(summary = "查询题目详情")
    @AdminPermission("question:view")
    @GetMapping("/question-banks/{bankId}/questions/{questionId}")
    public Result<QuestionDetailVO> get(
            @PathVariable Long bankId,
            @PathVariable Long questionId
    ) {
        return Result.success(questionService.get(bankId, questionId));
    }

    /** 在指定题库中创建一条默认未发布的题目。 */
    @Operation(summary = "创建题目")
    @AdminPermission("question:create")
    @PostMapping("/question-banks/{bankId}/questions")
    public Result<QuestionDetailVO> create(
            @PathVariable Long bankId,
            @Valid @RequestBody QuestionCreateDTO dto
    ) {
        return Result.success(questionService.create(bankId, dto));
    }

    /** 编辑题目主体，不改变其在任何题库中的排序。 */
    @Operation(summary = "编辑题目")
    @AdminPermission("question:update")
    @PutMapping("/question-banks/{bankId}/questions/{questionId}")
    public Result<QuestionDetailVO> update(
            @PathVariable Long bankId,
            @PathVariable Long questionId,
            @Valid @RequestBody QuestionUpdateDTO dto
    ) {
        return Result.success(questionService.update(bankId, questionId, dto));
    }

    /** 发布、下架、删除或恢复题目。 */
    @Operation(summary = "执行题目状态动作")
    @PostMapping("/question-banks/{bankId}/questions/{questionId}/actions")
    public Result<ActionResultVO> action(
            @PathVariable Long bankId,
            @PathVariable Long questionId,
            @Valid @RequestBody ResourceActionDTO dto
    ) {
        return Result.success(questionService.action(bankId, questionId, dto));
    }

    /** 原子保存指定题库内全部有效题目的顺序。 */
    @Operation(summary = "保存题目顺序")
    @AdminPermission("question:sort")
    @PutMapping("/question-banks/{bankId}/questions/order")
    public Result<QuestionOrderResultVO> updateOrder(
            @PathVariable Long bankId,
            @Valid @RequestBody QuestionOrderDTO dto
    ) {
        return Result.success(questionService.updateOrder(bankId, dto));
    }

    /** 上传 JPG、PNG 或 WebP 题目图片到 MinIO。 */
    @Operation(summary = "上传题目图片")
    @PostMapping("/uploads/question-images")
    public Result<QuestionImageUploadVO> uploadImage(@RequestPart("file") MultipartFile file) {
        accessService.requireAnyPermission("question:create", "question:update");
        return Result.success(imageService.upload(file));
    }
}
