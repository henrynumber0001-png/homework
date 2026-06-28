package com.homework.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.homework.common.entity.BaseEntity;
import com.homework.common.enums.UserInfoStatus;
import com.homework.common.enums.UserInfoUserRole;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_info")
public class UserInfo extends BaseEntity {

    @Schema(description = "用户账号ID")
    private String accountNo;

    private String displayName;

    private String avatar;

    /** 1.active;2.disabled;3.banned */
    private UserInfoStatus status;

    /** 1.user;2.admin */
    private UserInfoUserRole userRole;
}
