package com.homework.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.homework.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 普通管理员与功能权限码的关联记录。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("admin_account_permission")
public class AdminAccountPermission extends BaseEntity {

    /** 管理员 ID。 */
    private Long adminId;

    /** 功能权限码。 */
    private String permissionCode;
}
