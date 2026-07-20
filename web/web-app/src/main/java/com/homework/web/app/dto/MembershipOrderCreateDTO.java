package com.homework.web.app.dto;

import com.homework.model.enums.MembershipOrderPayType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MembershipOrderCreateDTO {

    @NotNull
    private Long planId;

    @NotNull
    private MembershipOrderPayType payType;
}
