package com.homework.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.homework.common.entity.BaseEntity;
import com.homework.common.enums.OtpCodePurpose;
import com.homework.common.enums.OtpCodeStatus;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("otp_code")
public class OtpCode extends BaseEntity {

    /** E.164 format, for example +8613812345678. */
    private String phoneNumber;

    private String codeHash;

    /** 1.login;2.register;3.bind_phone;4.reset_password */
    private OtpCodePurpose purpose;

    /** 1.active;2.consumed;3.expired;4.blocked */
    private OtpCodeStatus status;

    private LocalDateTime expiresTime;

    private LocalDateTime consumedTime;

    private Integer attemptCount;
}
