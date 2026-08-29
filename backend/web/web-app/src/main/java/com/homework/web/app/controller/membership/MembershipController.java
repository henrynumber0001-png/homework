package com.homework.web.app.controller.membership;

import com.homework.common.result.Result;
import com.homework.model.enums.MembershipOrderStatus;
import com.homework.web.app.dto.MembershipOrderCreateDTO;
import com.homework.web.app.service.MembershipCheckoutService;
import com.homework.web.app.service.MembershipService;
import com.homework.web.app.vo.*;
import jakarta.validation.Valid;

import java.util.List;

import lombok.RequiredArgsConstructor;
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
 * 套餐价格、补差资格、双台账调度和支付状态等业务规则全部放在 MembershipService。
 *
 * <p>这里故意不提供“前端确认支付成功”的接口。支付成功只能由完成验签的
 * 微信回调适配器调用 MembershipService.confirmPayment()。
 */

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/app/membership")
public class MembershipController {

    private final MembershipService membershipService;
    private final MembershipCheckoutService membershipCheckoutService;

    //点击“会员中心”跳转到这个页面，用于宣传和展示会员功能
    @GetMapping("center")
    public Result<MembershipInfoVO> membershipCenter() {
        return Result.success(membershipService.getMembershipInfo());
    }


    //为购买会员准备的详情页，这是支付动作之前的最后一步
    @GetMapping
    public Result<MembershipDetailPageVO> membershipDetailPage() {
        return Result.success(membershipService.getMembershipDetailPage());
    }

    /**
     * 创建全款购买或补差升级订单。本接口只创建 PENDING 订单，不发放权益。
     */
    @PostMapping("/orders")
    public Result<MembershipOrderCreateVO> createOrder(
            // 前端为同一次购买意图生成唯一 key；网络重试必须复用该 key，防止重复建单。
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            // @RequestBody 将 JSON 转为 DTO；@Valid 执行 DTO 上的 @NotNull 等校验。
            @Valid @RequestBody MembershipOrderCreateDTO dto
    ) {
        MembershipOrderCreateVO orderResponse = membershipCheckoutService.createOrder(idempotencyKey, dto);
        return Result.success(orderResponse);
    }

    /**
     * 查询一笔订单的最新状态，供支付页面轮询支付成功、失败或超时。
     */
    @GetMapping("/orders/{orderNo}")
    public Result<MembershipOrderStatus> orderStatus(@PathVariable String orderNo) {
        return Result.success(membershipService.getOrderStatus(orderNo));
    }

    //点击“查看全部订单”，默认返回 “待支付”页面
    @GetMapping("/pending-orders")
    public Result<List<PendingOrderVO>> pendingOrders() {
        return Result.success(membershipService.getPendingOrders());
    }

    //在查询全部订单页面，如果用户点击 “全部订单”，调用此接口
    @GetMapping("/all-orders")
    public Result<List<AllOrderVO>> allOrders(){
        return Result.success(membershipService.getAllOrders());
    }
}
