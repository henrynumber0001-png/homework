package com.homework.web.app.vo;

import lombok.Data;

@Data
public class MembershipInfoVO {

    private String displayName;

    private String avatarUrl;

    private MembershipCardVO membershipCard;
}
