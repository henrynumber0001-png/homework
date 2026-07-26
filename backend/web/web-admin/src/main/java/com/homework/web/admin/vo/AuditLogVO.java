package com.homework.web.admin.vo;

import lombok.Data;

import java.time.LocalDateTime;

/** 后台操作审计日志列表行。 */
@Data
public class AuditLogVO {

    /** HTTP 请求标识。 */
    private String requestId;

    /** 操作管理员 ID。 */
    private Long operatorAdminId;

    /** 操作管理员名称快照。 */
    private String operatorName;

    /** 业务模块。 */
    private String module;

    /** 操作动作。 */
    private String action;

    /** 目标资源类型。 */
    private String targetType;

    /** 目标资源 ID。 */
    private String targetId;

    /** 操作原因。 */
    private String reason;

    /** 变更前摘要 JSON。 */
    private String beforeSnapshot;

    /** 变更后摘要 JSON。 */
    private String afterSnapshot;

    /** 操作是否成功。 */
    private Boolean success;

    /** 失败原因。 */
    private String failureMessage;

    /** 请求 IP。 */
    private String ip;

    /** 请求 User-Agent。 */
    private String userAgent;

    /** 操作时间。 */
    private LocalDateTime createdTime;
}
