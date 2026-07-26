package com.homework.web.admin.controller;

import com.homework.common.result.Result;
import com.homework.web.admin.auth.AdminPermission;
import com.homework.web.admin.dto.QuestionImportCommitDTO;
import com.homework.web.admin.service.AdminQuestionImportService;
import com.homework.web.admin.vo.QuestionImportErrorVO;
import com.homework.web.admin.vo.QuestionImportTaskVO;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

/** 后台题目 Excel 导入接口。 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminQuestionImportController {

    private final AdminQuestionImportService importService;

    /** 按题库类型下载对应的 Excel 导入模板。 */
    @Operation(summary = "下载题目导入模板")
    @AdminPermission("question:import")
    @GetMapping("/question-banks/{bankId}/question-import-template")
    public ResponseEntity<byte[]> downloadTemplate(@PathVariable Long bankId) {
        byte[] content = importService.createTemplate(bankId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename("question-import-template.xlsx", StandardCharsets.UTF_8)
                .build());
        return new ResponseEntity<>(content, headers, HttpStatus.OK);
    }

    /** 上传 Excel 并同步完成逐行预检。 */
    @Operation(summary = "创建题目导入预检任务")
    @AdminPermission("question:import")
    @PostMapping("/question-imports")
    public ResponseEntity<Result<QuestionImportTaskVO>> createTask(
            @RequestParam Long bankId,
            @RequestPart("file") MultipartFile file
    ) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(Result.success(importService.createTask(bankId, file)));
    }

    /** 查询导入任务的预检或执行状态。 */
    @Operation(summary = "查询题目导入任务")
    @GetMapping("/question-imports/{taskId}")
    public Result<QuestionImportTaskVO> getTask(@PathVariable String taskId) {
        return Result.success(importService.getTask(taskId));
    }

    /** 查询导入任务全部逐行错误。 */
    @Operation(summary = "查询题目导入错误")
    @GetMapping("/question-imports/{taskId}/errors")
    public Result<List<QuestionImportErrorVO>> listErrors(@PathVariable String taskId) {
        return Result.success(importService.listErrors(taskId));
    }

    /** 确认将 READY 状态任务中的全部题目原子写入题库。 */
    @Operation(summary = "确认导入题目")
    @AdminPermission("question:import")
    @PostMapping("/question-imports/{taskId}/commit")
    public Result<QuestionImportTaskVO> commit(
            @PathVariable String taskId,
            @Valid @RequestBody QuestionImportCommitDTO dto
    ) {
        return Result.success(importService.commit(taskId, dto));
    }
}
