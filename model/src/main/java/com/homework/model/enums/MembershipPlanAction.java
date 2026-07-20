package com.homework.model.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum MembershipPlanAction {

    PURCHASE("purchase"),
    CURRENT("current"),
    UPGRADE("upgrade"),
    SCHEDULE_CHANGE("schedule_change"),
    SCHEDULED("scheduled"),
    UNAVAILABLE("unavailable");

    @JsonValue
    private final String value;

    MembershipPlanAction(String value) {
        this.value = value;
    }
}
