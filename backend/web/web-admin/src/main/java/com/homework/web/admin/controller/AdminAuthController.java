package com.homework.web.admin.controller;

import com.homework.common.result.Result;
import com.homework.web.admin.dto.AdminInvitationAcceptDTO;
import com.homework.web.admin.dto.AdminLoginDTO;
import com.homework.web.admin.dto.AdminPasswordChangeDTO;
import com.homework.web.admin.dto.AdminReauthDTO;
import com.homework.web.admin.service.AdminAuthService;
import com.homework.web.admin.vo.AdminInvitationPreviewVO;
import com.homework.web.admin.vo.AdminLoginVO;
import com.homework.web.admin.vo.AdminReauthVO;
import com.homework.web.admin.vo.CurrentAdminVO;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 管理员认证接口。 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/auth")
public class AdminAuthController {

    private final AdminAuthService authService;

    /** 校验一次性管理员邀请是否仍然有效。 */
    @Operation(summary = "校验管理员邀请")
    @GetMapping("/invitations/{token}")
    public Result<AdminInvitationPreviewVO> previewInvitation(@PathVariable String token) {
        return Result.success(authService.previewInvitation(token));
    }

    /** 接受管理员邀请并设置初始密码。 */
    @Operation(summary = "接受管理员邀请")
    @PostMapping("/invitations/{token}/accept")
    public Result<Void> acceptInvitation(@PathVariable String token, @Valid @RequestBody AdminInvitationAcceptDTO dto) {
        authService.acceptInvitation(token, dto);
        return Result.success();
    }

    /** 使用管理员邮箱和密码登录后台。 */
    @Operation(summary = "管理员登录")
    @PostMapping("/login")
    public Result<AdminLoginVO> login(@Valid @RequestBody AdminLoginDTO dto) {
        return Result.success(authService.login(dto));
    }

    /** 注销当前管理员会话。 */
    @Operation(summary = "管理员退出")
    @PostMapping("/logout")
    public Result<Void> logout() {
        authService.logout();
        return Result.success();
    }

    /** 查询当前管理员、权限和题库数据范围。 */
    @Operation(summary = "获取当前管理员")
    @GetMapping("/me")
    public Result<CurrentAdminVO> currentAdmin() {
        return Result.success(authService.currentAdmin());
    }

    /** 修改当前管理员密码并撤销其他会话。 */
    @Operation(summary = "修改管理员密码")
    @PutMapping("/password")
    public Result<String> changePassword(@Valid @RequestBody AdminPasswordChangeDTO dto) {
        return Result.success(authService.changePassword(dto));
    }

    /** 校验当前密码并签发一次性高风险操作令牌。 */
    @Operation(summary = "管理员二次认证")
    @PostMapping("/reauth")
    public Result<AdminReauthVO> reauthenticate(@Valid @RequestBody AdminReauthDTO dto) {
        return Result.success(authService.reauthenticate(dto));
    }
}
