package com.homework.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.homework.common.entity.BaseEntity;
import com.homework.model.enums.BillingType;
import com.homework.model.enums.MembershipSubscriptionStatus;
import com.homework.model.enums.MembershipType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("membership_subscription")
public class MembershipSubscription extends BaseEntity {

    private Long userId; //一个用户只有一条当前订阅记录。

    private Long planId; //用户当前持有的套餐id

    private MembershipType membershipType; // standard or premium

    private BillingType billingType;

    private MembershipSubscriptionStatus status; //是否active

    private LocalDateTime currentPeriodStart; //当前会员套餐的开始时间

    private LocalDateTime currentPeriodEnd; //当前会员套餐的结束时间

    private BigDecimal currentPeriodAmount; //当前会员套餐的实际价格

    private Long latestPaidOrderId;

    private Long pendingPlanId;

    private LocalDateTime pendingChangeTime;

    private Long pendingOrderId;

    private Boolean autoRenew;

    /**
     * Payment confirmation compares this version with the order snapshot so a
     * stale upgrade cannot overwrite a newer subscription state.
     */
    private Long subscriptionVersion; //这条订阅记录已经被正式修改过多少次。
}
