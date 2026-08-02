package com.homework.web.admin.dto;

import com.homework.model.enums.BillingType;
import com.homework.model.enums.MembershipPurchaseType;
import com.homework.model.enums.MembershipType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/** 创建会员套餐的请求。 */
@Data
public class MembershipPlanCreateDTO {

    @NotNull
    private MembershipType membershipType;

    @NotNull
    private MembershipPurchaseType purchaseType;

    @NotNull
    private Integer durationMonths;

    private BillingType billingType;

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal price;

    @NotBlank
    @Size(max = 10)
    private String currency;

    @NotNull
    private Boolean enabled;

    @NotBlank
    @Size(max = 500)
    private String reason;
}
