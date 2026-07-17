package com.homework.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.homework.common.entity.BaseEntity;
import com.homework.model.enums.PremiumOrderStatus;
import com.homework.model.enums.PremiumOrderPayType;
import com.homework.model.enums.PremiumOrderScope;
import com.homework.model.enums.PremiumOrderType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("premium_order")
public class PremiumOrder extends BaseEntity {

    private Long userId;

    /** 1.interview;2.certification */
    private PremiumOrderScope premiumScope;

    private String orderNo;

    /** 1.monthly;2.yearly */
    private PremiumOrderType type;

    /** 这笔订单对应的权益开始时间。 */
    private LocalDateTime startTime;

    /** 这笔订单对应的权益结束时间。 */
    private LocalDateTime expiredTime;

    private BigDecimal price;

    /** 1.wechat;2.alipay; */
    private PremiumOrderPayType payType;

    private LocalDateTime payTime;

    /** 1.pending;2.paid;3.cancelled;4.expired;5.refund */
    private PremiumOrderStatus orderStatus;
}
