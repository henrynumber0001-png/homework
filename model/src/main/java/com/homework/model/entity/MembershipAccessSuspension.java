package com.homework.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.homework.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/** 管理员暂停用户会员访问权的记录。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("membership_access_suspension")
public class MembershipAccessSuspension extends BaseEntity {

    /** 被暂停会员权益的 App 用户 ID。 */
    private Long userId;

    /** 暂停原因。 */
    private String reason;

    /** 发起暂停的管理员 ID。 */
    private Long adminId;

    /** 暂停时间。 */
    private LocalDateTime suspendedTime;

    /** 恢复时间，未恢复时为空。 */
    private LocalDateTime resumedTime;

    /** 恢复操作管理员 ID。 */
    private Long resumedByAdminId;
}
