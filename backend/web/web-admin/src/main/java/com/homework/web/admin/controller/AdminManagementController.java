package com.homework.web.admin.controller;

import com.homework.common.result.PageResult;
import com.homework.common.result.Result;
import com.homework.model.enums.AdminStatus;
import com.homework.web.admin.auth.AdminPermission;
import com.homework.web.admin.auth.AdminReauthService;
import com.homework.web.admin.dto.AdminAccessUpdateDTO;
import com.homework.web.admin.dto.AdminAccountActionDTO;
import com.homework.web.admin.dto.AdminInvitationCreateDTO;
import com.homework.web.admin.service.AdminManagementService;
import com.homework.web.admin.vo.ActionResultVO;
import com.homework.web.admin.vo.AdminInvitationCreateVO;
import com.homework.web.admin.vo.AdminRowVO;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 超级管理员维护普通管理员的接口。 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
@AdminPermission("admin:manage")
public class AdminManagementController {

    private final AdminManagementService managementService;
    private final AdminReauthService reauthService;

    /** 分页查询管理员及完整权限配置。 */
    @Operation(summary = "分页查询管理员")
    @GetMapping("/admins")
    public Result<PageResult<AdminRowVO>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) AdminStatus status,
            @RequestParam(required = false) Integer pageNum,
            @RequestParam(required = false) Integer pageSize
    ) {
        return Result.success(managementService.list(keyword, status, pageNum, pageSize));
    }

    /** 创建或刷新普通管理员一次性邀请链接。 */
    @Operation(summary = "邀请管理员")
    @PostMapping("/admin-invitations")
    public Result<AdminInvitationCreateVO> invite(
            @RequestHeader("X-Admin-Reauth-Token") String reauthToken,
            @Valid @RequestBody AdminInvitationCreateDTO dto
    ) {
        reauthService.consume(reauthToken, "admin:manage");
        return Result.success(managementService.invite(dto));
    }

    /** 修改普通管理员功能权限和题库数据范围。 */
    @Operation(summary = "修改管理员权限")
    @PutMapping("/admins/{adminId}/access")
    public Result<AdminRowVO> updateAccess(
            @PathVariable Long adminId,
            @RequestHeader("X-Admin-Reauth-Token") String reauthToken,
            @Valid @RequestBody AdminAccessUpdateDTO dto
    ) {
        reauthService.consume(reauthToken, "admin:manage");
        return Result.success(managementService.updateAccess(adminId, dto));
    }

    /** 禁用、启用或归档普通管理员并撤销旧会话。 */
    @Operation(summary = "执行管理员状态动作")
    @PostMapping("/admins/{adminId}/actions")
    public Result<ActionResultVO> action(
            @PathVariable Long adminId,
            @RequestHeader("X-Admin-Reauth-Token") String reauthToken,
            @Valid @RequestBody AdminAccountActionDTO dto
    ) {
        reauthService.consume(reauthToken, "admin:manage");
        return Result.success(managementService.action(adminId, dto));
    }
}
