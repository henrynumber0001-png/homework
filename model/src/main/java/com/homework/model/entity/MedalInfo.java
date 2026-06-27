package com.homework.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.homework.common.entity.BaseEntity;
import com.homework.common.enums.MedalInfoStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("medal_info")
public class MedalInfo extends BaseEntity {

    private String medalName;

    private String medalCode;

    private String medalIcon;

    private String description;

    /** 1.active;2.disabled */
    private MedalInfoStatus status;
}
