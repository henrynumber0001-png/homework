package com.homework.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.homework.common.entity.BaseEntity;
import lombok.Data;

@Data
@TableName("tech_direction")
public class TechDirection extends BaseEntity {

    //比如：后端开发/前端开发/测试/运维/AI算法/大数据/BA/PM
    private String techDirectionName;

}
