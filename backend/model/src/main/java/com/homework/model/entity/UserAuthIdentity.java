package com.homework.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.homework.common.entity.BaseEntity;
import com.homework.model.enums.UserAuthIdentityProvider;
import com.homework.model.enums.UserAuthIdentityStatus;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_auth_identities")
public class UserAuthIdentity extends BaseEntity {

    private Long userId;

    private UserAuthIdentityProvider provider;

    //normalized之后的
    private String account;

    private String passwordHash;

    /** 1.pending;2.verified;3.disabled;4.unlinked */
    private UserAuthIdentityStatus status;

    private LocalDateTime verifiedTime;

    private LocalDateTime lastUsedTime;
}
