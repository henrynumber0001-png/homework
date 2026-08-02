package com.homework.web.admin.vo;

import com.homework.model.enums.BillingType;
import com.homework.model.enums.MembershipPurchaseType;
import com.homework.model.enums.MembershipType;
import lombok.Data;

import java.math.BigDecimal;

/** 后台会员套餐。 */
@Data
public class MembershipPlanVO {

    /** 套餐 ID。 */
    private Long id;

    /** 会员等级名称。 */
    private MembershipType membershipType;

    /** 全款或补差购买类型。 */
    private MembershipPurchaseType purchaseType;

    /** 套餐月数。 */
    private Integer durationMonths;

    /** 月、季或年计费类型。 */
    private BillingType billingType;

    /** 当前价格。 */
    private BigDecimal price;

    /** 币种。 */
    private String currency;

    /** 是否上架。 */
    private Boolean enabled;

    /** 乐观锁版本。 */
    private Integer version;
}
