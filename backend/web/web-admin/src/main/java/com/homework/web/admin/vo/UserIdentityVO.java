package com.homework.web.admin.vo;

import com.homework.model.enums.UserAuthIdentityProvider;
import com.homework.model.enums.UserAuthIdentityStatus;
import lombok.Data;

import java.time.LocalDateTime;

/** 用户脱敏登录身份。 */
@Data
public class UserIdentityVO {

    /** 登录提供方名称。 */
    private UserAuthIdentityProvider provider;

    /** 脱敏后的邮箱、手机号或第三方标识。 */
    private String maskedIdentifier;

    /** 身份状态名称。 */
    private UserAuthIdentityStatus status;

    /** 最近使用时间。 */
    private LocalDateTime lastUsedTime;
}
