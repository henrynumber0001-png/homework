package com.homework.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.homework.common.entity.BaseEntity;
import com.homework.common.enums.PremiumPlanBillingCycle;
import com.homework.common.enums.PremiumPlanPremiumScope;
import com.homework.common.enums.PremiumPlanStatus;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("premium_plan")
public class PremiumPlan extends BaseEntity {

    private String planName;

    /** 1.interview;2.certification */
    private PremiumPlanPremiumScope premiumScope;

    /** 1.monthly;2.yearly */
    private PremiumPlanBillingCycle billingCycle;

    private BigDecimal price;

    private Integer durationDays;

    private String benefits;

    /** 1.active;2.disabled */
    private PremiumPlanStatus status;
}
