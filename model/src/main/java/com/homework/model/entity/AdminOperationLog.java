package com.homework.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.homework.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 后台写操作审计日志。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("admin_operation_log")
public class AdminOperationLog extends BaseEntity {

    /** 单次 HTTP 请求标识。 */
    private String requestId;

    /** 操作管理员 ID。 */
    private Long operatorAdminId;

    /** 操作管理员显示名称快照。 */
    private String operatorName;

    /** 业务模块，例如 BANK、QUESTION。 */
    private String module;

    /** 操作动作，例如 CREATE、UPDATE、PUBLISH。 */
    private String action;

    /** 目标资源类型。 */
    private String targetType;

    /** 目标资源 ID。 */
    private String targetId;

    /** 管理员填写的操作原因。 */
    private String reason;

    /** 变更前数据摘要 JSON。 */
    private String beforeSnapshot;

    /** 变更后数据摘要 JSON。 */
    private String afterSnapshot;

    /** 操作是否成功。 */
    private Boolean success;

    /** 失败原因，成功时为空。 */
    private String failureMessage;

    /** 请求来源 IP。 */
    private String ip;

    /** 请求 User-Agent。 */
    private String userAgent;
}
