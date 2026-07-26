package com.homework.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.homework.common.entity.BaseEntity;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** Premium Plus 高级会员台账。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("svip_record")
public class SvipRecord extends BaseEntity {

    private Long userId;

    private LocalDateTime expireTime;

    /** 乐观锁版本。 */
    @Version
    private Integer version;
}
