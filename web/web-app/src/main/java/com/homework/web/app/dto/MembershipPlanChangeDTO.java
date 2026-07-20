package com.homework.web.app.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MembershipPlanChangeDTO {

    @NotNull
    private Long planId;
}
