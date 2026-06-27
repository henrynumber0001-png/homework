package com.homework.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.homework.common.entity.BaseEntity;
import com.homework.common.enums.PremiumOrderOrderStatus;
import com.homework.common.enums.PremiumOrderPayType;
import com.homework.common.enums.PremiumOrderPremiumScope;
import com.homework.common.enums.PremiumOrderType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("premium_order")
public class PremiumOrder extends BaseEntity {

    private Long userId;

    private Long planId;

    /** 1.interview;2.certification */
    private PremiumOrderPremiumScope premiumScope;

    private String orderNo;

    /** 1.monthly;2.yearly */
    private PremiumOrderType type;

    private LocalDateTime startTime;

    private LocalDateTime expiredTime;

    private BigDecimal payAmount;

    /** 1.wechat;2.alipay; */
    private PremiumOrderPayType payType;

    private LocalDateTime payTime;

    /** 1.pending;2.paid;3.cancelled;4.expired;5.refund */
    private PremiumOrderOrderStatus orderStatus;
}
