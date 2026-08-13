package com.homework.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.homework.common.entity.BaseEntity;
import com.homework.model.enums.BankDataScope;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/** 普通管理员一次性邀请记录。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("admin_invitation")
/*
admin_invitation 是 数据库表，如果不把 List<String> permissions 和 List<Long> assignedBankIds 转换成对应的 permissionsJson 和 bankIdsJson
就无法保存数据，因为数据库不能存 List，但是能存 Json
 */
public class AdminInvitation extends BaseEntity {

    /** 被邀请邮箱。 */
    private String email;

    /** 被邀请管理员的管理员名称。 */
    private String displayName;

    /** 一次性邀请 Token 的 SHA-256 摘要。 */
    private String tokenDigest;

    /** 邀请时分配的权限码 JSON 数组。 */
    private String permissionsJson;

    /** 邀请时分配的题库数据范围。 */
    private BankDataScope bankDataScope;

    /** 已分配题库 ID 的 JSON 数组。 */
    private String bankIdsJson;

    /** 邀请到期时间。 */
    private LocalDateTime expiresTime;

    /** 接受邀请的时间，未接受时为空。 */
    private LocalDateTime acceptedTime;

    /** 创建邀请的超级管理员 ID。 */
    private Long invitedByAdminId;
}
