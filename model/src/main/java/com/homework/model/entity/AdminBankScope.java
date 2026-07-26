package com.homework.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.homework.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 普通管理员可管理题库的授权关系。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("admin_bank_scope")
public class AdminBankScope extends BaseEntity {

    /** 管理员 ID。 */
    private Long adminId;

    /** 被授权的题库 ID。 */
    private Long bankId;
}
