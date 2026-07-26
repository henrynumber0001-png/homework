package com.homework.web.admin.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/** 修改会员套餐价格和启停状态的请求。 */
@Data
public class MembershipPlanUpdateDTO {

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal price;

    @NotNull
    private Boolean enabled;

    @NotBlank
    @Size(max = 500)
    private String reason;

    @NotNull
    private Integer version;
}
