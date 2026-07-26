package com.homework.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.homework.common.entity.BaseEntity;
import com.homework.model.enums.CommunityRestrictionScope;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/** 用户发帖和评论权限的后台限制记录。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_community_restriction")
public class UserCommunityRestriction extends BaseEntity {

    /** 被限制的 App 用户 ID。 */
    private Long userId;

    /** 限制发帖、评论或两者。 */
    private CommunityRestrictionScope scope;

    /** 限制开始时间。 */
    private LocalDateTime startTime;

    /** 限制结束时间。 */
    private LocalDateTime endTime;

    /** 是否仍然有效。 */
    private Boolean active;

    /** 操作原因。 */
    private String reason;

    /** 操作管理员 ID。 */
    private Long adminId;

    /** 乐观锁版本。 */
    @Version
    private Integer version;
}
