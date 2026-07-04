package com.homework.web.app.dto;

import com.homework.model.enums.UserAuthIdentityProvider;
import lombok.Data;

@Data
public class ThirdPartyUser {
    private UserAuthIdentityProvider provider;

    /**
     * 第三方平台稳定用户ID：
     * Google/Apple: sub
     * 微信: unionid 优先，没有 unionid 用 openid
     * QQ: openid
     */
    private String externalUserId;

    private String displayName;

    private String avatar;

    private String email;
}
