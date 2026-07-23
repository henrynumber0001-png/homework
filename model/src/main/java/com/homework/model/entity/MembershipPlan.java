package com.homework.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.homework.common.entity.BaseEntity;
import com.homework.model.enums.BillingType;
import com.homework.model.enums.MembershipPurchaseType;
import com.homework.model.enums.MembershipType;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("membership_plan")
public class MembershipPlan extends BaseEntity {

    private MembershipType membershipType;

    private MembershipPurchaseType purchaseType;

    /** 全款套餐为 1/3/12；补差套餐为 1-12。 */
    private Integer durationMonths;

    /** 补差套餐没有自然月、季、年的计费类型，因此该字段为空。 */
    private BillingType billingType;

    private BigDecimal price;

    private String currency;

    //是否下架检测标识
    private Boolean enabled;
}
