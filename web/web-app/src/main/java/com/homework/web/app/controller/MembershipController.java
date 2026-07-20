package com.homework.web.app.controller;

import com.homework.common.result.Result;
import com.homework.web.app.dto.MembershipOrderCreateDTO;
import com.homework.web.app.dto.MembershipPlanChangeDTO;
import com.homework.web.app.service.MembershipCheckoutService;
import com.homework.web.app.service.MembershipService;
import com.homework.web.app.vo.MembershipOrderCreateVO;
import com.homework.web.app.vo.MembershipOrderHistoryVO;
import com.homework.web.app.vo.MembershipOrderStatusVO;
import com.homework.web.app.vo.MembershipPageVO;
import com.homework.web.app.vo.MembershipPlanChangeVO;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * App 端会员接口。
 *
 * <p>Controller 只负责接收 HTTP 参数、触发参数校验、调用 Service 并包装返回值；
 * 套餐价格、升级抵扣、降级时点和支付状态等业务规则全部放在 MembershipService。
 *
 * <p>这里故意不提供“前端确认支付成功”的接口。支付成功只能由完成验签的
 * 微信/支付宝回调适配器调用 MembershipService.confirmPayment()。
 */

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/app/membership")
public class MembershipController {

    private final MembershipService membershipService;
    private final MembershipCheckoutService membershipCheckoutService;

    /**
     * 打开会员中心页面。
     *
     * <p>返回当前订阅，以及 Standard/Premium × Monthly/Yearly 套餐选项。
     * 每个套餐由后端计算 action，前端据此展示购买、升级、当前套餐、预约变更或不可用。
     */
    @GetMapping
    public Result<MembershipPageVO> membershipPage() {
        return Result.success(membershipService.getMembershipPage());
    }

    /**
     * 创建首次购买或即时升级订单。
     *
     * <p>本接口只创建 PENDING 订单，不直接发放会员权益。降级和 Monthly/Yearly
     * 切换不在此处收费，必须使用 /plan-change 预约到本期结束后生效。
     */
    @PostMapping("/orders")
    public Result<MembershipOrderCreateVO> createOrder(
            // 前端为同一次购买意图生成唯一 key；网络重试必须复用该 key，防止重复建单。
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            // @RequestBody 将 JSON 转为 DTO；@Valid 执行 DTO 上的 @NotNull 等校验。
            @Valid @RequestBody MembershipOrderCreateDTO dto
    ) {
        return Result.success(
                membershipCheckoutService.createOrder(idempotencyKey, dto)
        );
    }

    /**
     * 查询一笔订单的最新状态，供支付页面轮询支付成功、失败或超时。
     */
    @GetMapping("/orders/{orderNo}")
    public Result<MembershipOrderStatusVO> orderStatus(@PathVariable String orderNo) {
        return Result.success(membershipService.getOrderStatus(orderNo));
    }

    /**
     * 查询当前登录用户的全部会员订单历史，按照订单时间倒序返回。
     */
    // GET /api/app/membership/orders
    @GetMapping("/orders")
    public Result<List<MembershipOrderHistoryVO>> orderHistory() {
        return Result.success(membershipService.getOrderHistory());
    }

    /**
     * 预约下周期套餐变更。
     *
     * <p>适用于 Premium 降级为 Standard，或同会员等级的 Monthly/Yearly 切换。
     * 这里只保存 pendingPlanId 和生效时间，不立即改变当前权益，也不立即退款或收费。
     */
    // POST /api/app/membership/plan-change
    @PostMapping("/plan-change")
    public Result<MembershipPlanChangeVO> schedulePlanChange(
            @Valid @RequestBody MembershipPlanChangeDTO dto
    ) {
        return Result.success(membershipService.schedulePlanChange(dto));
    }

    /**
     * 取消尚未生效的套餐变更预约。
     *
     * <p>只清除 pendingPlanId，不会取消或缩短当前有效会员。
     */
    // DELETE /api/app/membership/plan-change
    @DeleteMapping("/plan-change")
    public Result<Void> cancelScheduledPlanChange() {
        membershipService.cancelScheduledPlanChange();
        return Result.success();
    }
}
