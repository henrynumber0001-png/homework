package com.homework.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.homework.common.entity.BaseEntity;

import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** Premium 基础会员台账。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("base_vip_record")
public class BaseVipRecord extends BaseEntity {

    private Long userId;

    /** 包含前置 Premium Plus 时长后的最终到期时间。 */
    private LocalDateTime expireTime;
}
