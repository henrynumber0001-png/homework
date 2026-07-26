package com.homework.web.admin.controller;

import com.homework.common.result.PageResult;
import com.homework.common.result.Result;
import com.homework.web.admin.auth.AdminPermission;
import com.homework.web.admin.service.AdminAuditQueryService;
import com.homework.web.admin.vo.AuditLogVO;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/** 后台审计日志查询接口。 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/audit-logs")
public class AdminAuditController {

    private final AdminAuditQueryService auditQueryService;

    /** 分页查询操作日志；普通管理员只能看到自己的记录。 */
    @Operation(summary = "分页查询操作日志")
    @AdminPermission("audit:view")
    @GetMapping
    public Result<PageResult<AuditLogVO>> list(
            @RequestParam(required = false) Long operatorAdminId,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String targetId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(required = false) Integer pageNum,
            @RequestParam(required = false) Integer pageSize
    ) {
        return Result.success(auditQueryService.list(
                operatorAdminId,
                module,
                action,
                targetId,
                startTime,
                endTime,
                pageNum,
                pageSize
        ));
    }
}
