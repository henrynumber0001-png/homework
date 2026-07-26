package com.homework.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.homework.common.entity.BaseEntity;
import com.homework.model.enums.BillingType;
import com.homework.model.enums.MembershipOrderAction;
import com.homework.model.enums.MembershipOrderStatus;
import com.homework.model.enums.MembershipType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("membership_order")
public class MembershipOrder extends BaseEntity {

    private Long userId;

    private String orderNo;

    private MembershipOrderAction action;

    private Long toPlanId;

    /** 以下字段是下单时的商品快照，套餐改价不会改变历史订单。 */
    private MembershipType membershipType;

    private BillingType billingType;

    private Integer durationMonths;

    private BigDecimal payAmount;

    private String currency;

    private LocalDateTime periodEnd;

    private LocalDateTime payTime;

    private MembershipOrderStatus orderStatus;

    private String providerTradeNo;

    /** 微信 Native 预下单返回的 code_url。 */
    private String paymentCodeUrl;

    private String idempotencyKey;

    private LocalDateTime paymentExpiredTime;

}
