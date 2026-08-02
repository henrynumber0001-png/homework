package com.homework.web.admin.controller;

import com.homework.common.result.PageResult;
import com.homework.common.result.Result;
import com.homework.web.admin.auth.AdminAccessService;
import com.homework.web.admin.auth.AdminPermission;
import com.homework.web.admin.auth.AdminReauthService;
import com.homework.web.admin.dto.MembershipActionDTO;
import com.homework.model.enums.MembershipAction;
import com.homework.model.enums.MembershipOrderStatus;
import com.homework.model.enums.MembershipStatus;
import com.homework.web.admin.dto.MembershipPlanCreateDTO;
import com.homework.web.admin.dto.MembershipPlanUpdateDTO;
import com.homework.web.admin.service.AdminMembershipService;
import com.homework.web.admin.vo.MembershipDetailVO;
import com.homework.web.admin.vo.MembershipOrderVO;
import com.homework.web.admin.vo.MembershipPlanVO;
import com.homework.web.admin.vo.MembershipRowVO;
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

import java.util.List;
/** 后台会员、订单和套餐配置接口。 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminMembershipController {

    private final AdminMembershipService membershipService;
    private final AdminAccessService accessService;
    private final AdminReauthService reauthService;

    /** 分页查询 App 用户会员概况。 */
    @Operation(summary = "分页查询会员")
    @AdminPermission("membership:view")
    @GetMapping("/memberships")
    public Result<PageResult<MembershipRowVO>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) MembershipStatus membershipType,
            @RequestParam(required = false) Integer pageNum,
            @RequestParam(required = false) Integer pageSize
    ) {
        return Result.success(membershipService.list(keyword, membershipType, pageNum, pageSize));
    }

    /** 查询指定 `user_info.id` 的双会员台账和最近变更。 */
    @Operation(summary = "查询用户会员详情")
    @AdminPermission("membership:view")
    @GetMapping("/memberships/users/{userId}")
    public Result<MembershipDetailVO> get(@PathVariable Long userId) {
        return Result.success(membershipService.get(userId));
    }

    /** 给指定 App 用户发放、暂停、恢复或回收会员。 */
    @Operation(summary = "执行会员动作")
    @PostMapping("/memberships/users/{userId}/actions")
    public Result<MembershipDetailVO> action(
            @PathVariable Long userId,
            @RequestHeader(value = "X-Admin-Reauth-Token", required = false) String reauthToken,
            @Valid @RequestBody MembershipActionDTO dto
    ) {
        MembershipAction action = dto.getAction();
        if (action == MembershipAction.REVOKE) {
            accessService.requirePermission("membership:revoke");
            reauthService.consume(reauthToken, "membership:revoke");
        } else {
            accessService.requirePermission("membership:manage");
        }
        return Result.success(membershipService.action(userId, dto));
    }

    /** 分页查询会员支付订单；当前版本不提供退款入口。 */
    @Operation(summary = "分页查询会员订单")
    @AdminPermission("membership:view")
    @GetMapping("/membership-orders")
    public Result<PageResult<MembershipOrderVO>> listOrders(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) MembershipOrderStatus orderStatus,
            @RequestParam(required = false) Integer pageNum,
            @RequestParam(required = false) Integer pageSize
    ) {
        return Result.success(membershipService.listOrders(
                keyword, userId, orderStatus, pageNum, pageSize));
    }

    /** 查询全部会员套餐配置。 */
    @Operation(summary = "查询会员套餐")
    @AdminPermission("membership:view")
    @GetMapping("/membership-plans")
    public Result<List<MembershipPlanVO>> listPlans() {
        return Result.success(membershipService.listPlans());
    }

    /** 创建一个默认可独立启停的会员套餐。 */
    @Operation(summary = "创建会员套餐")
    @AdminPermission("membership:plan")
    @PostMapping("/membership-plans")
    public Result<MembershipPlanVO> createPlan(
            @RequestHeader("X-Admin-Reauth-Token") String reauthToken,
            @Valid @RequestBody MembershipPlanCreateDTO dto
    ) {
        reauthService.consume(reauthToken, "membership:plan");
        return Result.success(membershipService.createPlan(dto));
    }

    /** 修改会员套餐价格和启停状态。 */
    @Operation(summary = "编辑会员套餐")
    @AdminPermission("membership:plan")
    @PutMapping("/membership-plans/{planId}")
    public Result<MembershipPlanVO> updatePlan(
            @PathVariable Long planId,
            @RequestHeader("X-Admin-Reauth-Token") String reauthToken,
            @Valid @RequestBody MembershipPlanUpdateDTO dto
    ) {
        reauthService.consume(reauthToken, "membership:plan");
        return Result.success(membershipService.updatePlan(planId, dto));
    }
}
