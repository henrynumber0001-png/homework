package com.homework.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.homework.common.entity.BaseEntity;
import com.homework.model.enums.BillingType;
import com.homework.model.enums.MembershipType;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("membership_plan") //表示“会员详情页的 可购买会员套餐列表”。
public class MembershipPlan extends BaseEntity {

    private MembershipType membershipType;

    private BillingType billingType;

    private BigDecimal price;

    private String currency;

    private Boolean enabled; //这个套餐当前是否允许被用户选择和创建新订单。
}
