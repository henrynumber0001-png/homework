package com.homework.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.homework.common.entity.BaseEntity;
import com.homework.model.enums.MembershipChangeType;
import com.homework.model.enums.MembershipType;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 管理员引起的不可变会员台账变更记录。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("membership_change_record")
public class MembershipChangeRecord extends BaseEntity {

    /** 被调整会员的 App 用户 ID。 */
    private Long userId;

    /** 变更类型。 */
    private MembershipChangeType changeType;

    /** 涉及的会员等级，不涉及具体等级时为空。 */
    private MembershipType membershipType;

    /** 发放月数，非发放动作时为空。 */
    private Integer durationMonths;

    /** 变更前双台账快照 JSON。 */
    private String beforeSnapshot;

    /** 变更后双台账快照 JSON。 */
    private String afterSnapshot;

    /** 操作原因。 */
    private String reason;

    /** 操作管理员 ID。 */
    private Long adminId;
}
