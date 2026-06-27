package com.homework.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.homework.common.entity.BaseEntity;
import com.homework.common.enums.UserAuthIdentityProvider;
import com.homework.common.enums.UserAuthIdentityStatus;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_auth_identities")
public class UserAuthIdentity extends BaseEntity {

    private Long userId;

    /** 1.email_password;2.phone_otp;3.google;4.wechat;5.qq */
    private UserAuthIdentityProvider provider;

    /** Original or display login identifier: email, phone, openid. */
    private String identifier;

    /** Normalized identifier used for login lookup and uniqueness. */
    private String identifierNormalized;

    private String passwordHash;

    /** 1.pending;2.verified;3.disabled;4.unlinked */
    private UserAuthIdentityStatus status;

    private LocalDateTime verifiedTime;

    private LocalDateTime lastUsedTime;
}
