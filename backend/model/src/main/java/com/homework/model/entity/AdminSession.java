package com.homework.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.homework.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/** 可主动撤销的后台管理员登录会话。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("admin_session")
public class AdminSession extends BaseEntity {

    /** 写入 Admin Token 的随机会话标识。 */
    private String sessionKey;

    /** 会话所属管理员 ID。 */
    private Long adminId;

    /** 会话到期时间。 */
    private LocalDateTime expiresTime;

    /** 会话撤销时间，未撤销时为空。 */
    private LocalDateTime revokedTime;
}
