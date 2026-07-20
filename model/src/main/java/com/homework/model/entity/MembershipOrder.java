package com.homework.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.homework.common.entity.BaseEntity;
import com.homework.model.enums.BillingType;
import com.homework.model.enums.MembershipOrderAction;
import com.homework.model.enums.MembershipOrderPayType;
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

    private Long fromPlanId;

    private Long toPlanId;

    /** Snapshot fields: historical orders must not change when plan data changes. */
    private MembershipType membershipType;

    private BillingType billingType;

    private BigDecimal originalAmount;

    private BigDecimal creditAmount;

    private BigDecimal payAmount;

    private String currency;

    private LocalDateTime periodStart;

    private LocalDateTime periodEnd;

    private MembershipOrderPayType payType;

    private LocalDateTime payTime;

    private MembershipOrderStatus orderStatus;

    private String providerTradeNo;

    /**
     * 微信 Native 预下单返回的 code_url。
     * 保存后，同一个幂等键重试可以继续展示原二维码，而不重复创建微信订单。
     */
    private String paymentCodeUrl;

    private String idempotencyKey;

    private LocalDateTime paymentExpiredTime;

    private Long sourceSubscriptionVersion;
}
