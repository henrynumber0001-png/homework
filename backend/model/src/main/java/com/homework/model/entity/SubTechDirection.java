package com.homework.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.homework.common.entity.BaseEntity;
import lombok.Data;

@Data
@TableName("sub_tech_direction")
public class SubTechDirection extends BaseEntity {

    //比如：Java后端/C++后端/PHP后端/.NET后端/Golang后端
    private String subDirectionName;

    private Long directionId;
}
