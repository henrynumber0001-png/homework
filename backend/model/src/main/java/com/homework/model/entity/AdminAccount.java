package com.homework.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.homework.common.entity.BaseEntity;
import com.homework.model.enums.AdminRole;
import com.homework.model.enums.AdminStatus;
import com.homework.model.enums.BankDataScope;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/** 后台管理员独立登录账号，不复用 App 用户账号。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("admin_account")
public class AdminAccount extends BaseEntity {

    /** 管理员登录邮箱，按小写形式唯一。 */
    private String email;

    /** BCrypt 密码摘要。 */
    private String passwordHash;

    /** 后台展示名称。 */
    private String displayName;

    /** 超级管理员或普通管理员。 */
    private AdminRole role;

    /** 账号当前状态。 */
    private AdminStatus status;

    /** 可访问全部题库或仅分配题库。 */
    private BankDataScope bankDataScope;

    /** 会话版本，权限或状态变化后递增以撤销旧 Token。 */
    private Integer sessionVersion;

    /** 最近一次成功登录时间。 */
    private LocalDateTime lastLoginTime;

    /** 是否为部署时初始化的内置超级管理员。 */
    private Boolean builtIn;

    /** 乐观锁版本。 */
    @Version
    private Integer version;
}
