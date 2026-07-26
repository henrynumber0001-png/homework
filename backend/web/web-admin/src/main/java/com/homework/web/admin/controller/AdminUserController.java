package com.homework.web.admin.controller;

import com.homework.common.result.PageResult;
import com.homework.common.result.Result;
import com.homework.web.admin.auth.AdminAccessService;
import com.homework.web.admin.auth.AdminPermission;
import com.homework.web.admin.auth.AdminReauthService;
import com.homework.web.admin.dto.ResourceActionDTO;
import com.homework.web.admin.dto.UserCommunityAccessDTO;
import com.homework.web.admin.service.AdminUserService;
import com.homework.web.admin.vo.ActionResultVO;
import com.homework.web.admin.vo.UserDetailVO;
import com.homework.web.admin.vo.UserRowVO;
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

import java.util.Locale;

/** 后台 App 用户管理接口。 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final AdminUserService userService;
    private final AdminAccessService accessService;
    private final AdminReauthService reauthService;

    /** 分页查询 App 用户基础信息和当前会员等级。 */
    @Operation(summary = "分页查询用户")
    @AdminPermission("user:view")
    @GetMapping
    public Result<PageResult<UserRowVO>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer pageNum,
            @RequestParam(required = false) Integer pageSize
    ) {
        return Result.success(userService.list(keyword, status, pageNum, pageSize));
    }

    /** 查询用户脱敏身份、社区限制和内容数量。 */
    @Operation(summary = "查询用户详情")
    @AdminPermission("user:view")
    @GetMapping("/{userId}")
    public Result<UserDetailVO> get(@PathVariable Long userId) {
        return Result.success(userService.get(userId));
    }

    /** 禁用、启用、封禁或解封 App 用户。 */
    @Operation(summary = "执行用户状态动作")
    @PostMapping("/{userId}/actions")
    public Result<ActionResultVO> action(
            @PathVariable Long userId,
            @RequestHeader(value = "X-Admin-Reauth-Token", required = false) String reauthToken,
            @Valid @RequestBody ResourceActionDTO dto
    ) {
        String action = dto.getAction().trim().toUpperCase(Locale.ROOT);
        if ("BAN".equals(action) || "UNBAN".equals(action)) {
            accessService.requirePermission("user:ban");
            reauthService.consume(reauthToken, "user:ban");
        } else {
            accessService.requirePermission("user:manage");
        }
        return Result.success(userService.action(userId, dto));
    }

    /** 限制或提前恢复用户发帖和评论权限。 */
    @Operation(summary = "设置用户社区权限")
    @AdminPermission("user:manage")
    @PutMapping("/{userId}/community-access")
    public Result<UserDetailVO> updateCommunityAccess(
            @PathVariable Long userId,
            @Valid @RequestBody UserCommunityAccessDTO dto
    ) {
        return Result.success(userService.updateCommunityAccess(userId, dto));
    }
}
